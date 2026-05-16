package com.fintrack.user.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Java 21 — uses pattern-matching {@code switch} on the throwable to classify exceptions in
 * {@link #logException(JoinPoint, Throwable)}. Otherwise identical to the Java 17 sibling.
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Before("execution(* com.fintrack..*Controller.*(..))")
    public void logController(JoinPoint jp) {
        log.info("→ REQUEST {}.{}({})",
                jp.getSignature().getDeclaringType().getSimpleName(),
                jp.getSignature().getName(),
                Arrays.toString(jp.getArgs()));
    }

    @Around("execution(* com.fintrack..*Service.*(..))")
    public Object logService(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.nanoTime();
        String name = pjp.getSignature().toShortString();
        log.debug("⮕ SERVICE {} args={}", name, Arrays.toString(pjp.getArgs()));
        try {
            Object out = pjp.proceed();
            long ms = (System.nanoTime() - start) / 1_000_000;
            log.info("⬅ SERVICE {} took {}ms", name, ms);
            return out;
        } catch (Throwable ex) {
            long ms = (System.nanoTime() - start) / 1_000_000;
            log.debug("✗ SERVICE {} failed after {}ms", name, ms);
            throw ex;
        }
    }

    @AfterThrowing(pointcut = "execution(* com.fintrack..*Service.*(..))", throwing = "ex")
    public void logException(JoinPoint jp, Throwable ex) {
        String name = jp.getSignature().toShortString();
        // Pattern-matching switch on the throwable — exhaustive on the categories we care about.
        switch (ex) {
            case IllegalArgumentException iae ->
                    log.warn("✗ SERVICE {} bad-input {}: {}", name, iae.getClass().getSimpleName(), iae.getMessage());
            case NullPointerException npe ->
                    log.error("✗ SERVICE {} NPE: {}", name, npe.getMessage(), npe);
            case RuntimeException re ->
                    log.warn("✗ SERVICE {} runtime {}: {}", name, re.getClass().getSimpleName(), re.getMessage());
            default ->
                    log.error("✗ SERVICE {} checked {}: {}", name, ex.getClass().getSimpleName(), ex.getMessage());
        }
    }
}
