package com.fintrack.notification.aspect;

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

/**
 * Writes a structured audit record to the dedicated {@code AUDIT} logger for any
 * {@code transfer*}, {@code deposit*} or {@code withdraw*} service method. Kibana picks these
 * up via the {@code app=audit} field set by logstash-logback-encoder.
 */
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
        String username = currentUser();
        AuditEvent event = AuditEvent.builder()
                .timestamp(Instant.now())
                .username(username)
                .method(jp.getSignature().toShortString())
                .result(summarise(result))
                .build();
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
        if (result == null) return "void";
        String s = result.toString();
        return s.length() > 200 ? s.substring(0, 200) + "…" : s;
    }
}
