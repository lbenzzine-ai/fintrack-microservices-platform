package com.fintrack.configserver.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Aspect
@Component
public class AuditAspect {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");
    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @AfterReturning(
        pointcut = "execution(* com.fintrack..*Service.transfer*(..)) " +
                   "|| execution(* com.fintrack..*Service.deposit*(..)) " +
                   "|| execution(* com.fintrack..*Service.withdraw*(..)) " +
                   "|| execution(* com.fintrack..*Service.create*(..)) " +
                   "|| execution(* com.fintrack..*Service.complete*(..))",
        returning = "result")
    public void audit(JoinPoint jp, Object result) {
        AuditEvent event = new AuditEvent(
                Instant.now(),
                currentUser(),
                jp.getSignature().toShortString(),
                summarise(result),
                null);
        try {
            AUDIT.info(JSON.writeValueAsString(event));
        } catch (JsonProcessingException ex) {
            log.warn("AUDIT serialisation failed: {}", ex.getMessage());
        }
    }

    private String currentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return (a == null) ? "anonymous" : String.valueOf(a.getPrincipal());
    }

    private String summarise(Object result) {
        return switch (result) {
            case null -> "void";
            case String s -> s.length() > 200 ? s.substring(0, 200) + "…" : s;
            default -> {
                String s = result.toString();
                yield s.length() > 200 ? s.substring(0, 200) + "…" : s;
            }
        };
    }
}
