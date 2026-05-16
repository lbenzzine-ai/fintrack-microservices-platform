package com.fintrack.account.aspect;

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
 * Generic logging aspect — Java 17 build. Logs:
 * <ul>
 *   <li>{@code @Before} on every {@code *Controller} method — request boundary.</li>
 *   <li>{@code @Around} on every {@code *Service} method — name, args, execution time.</li>
 *   <li>{@code @AfterThrowing} on every {@code *Service} method — type + message.</li>
 * </ul>
 * Kept package-agnostic via {@code com.fintrack..} so any module reuses it as-is.
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
        // Java 17 — traditional instanceof checks (no pattern-matching binding).
        String simpleType = ex.getClass().getSimpleName();
        if (ex instanceof IllegalArgumentException) {
            log.warn("✗ SERVICE {} bad-input {}: {}", jp.getSignature().toShortString(), simpleType, ex.getMessage());
        } else if (ex instanceof RuntimeException) {
            log.warn("✗ SERVICE {} runtime {}: {}", jp.getSignature().toShortString(), simpleType, ex.getMessage());
        } else {
            log.error("✗ SERVICE {} checked {}: {}", jp.getSignature().toShortString(), simpleType, ex.getMessage());
        }
    }
}
