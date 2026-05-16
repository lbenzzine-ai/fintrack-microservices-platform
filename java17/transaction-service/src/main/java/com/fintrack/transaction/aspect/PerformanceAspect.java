package com.fintrack.transaction.aspect;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Records per-service-method execution time as a Micrometer {@link Timer} (tagged with method
 * name) so it shows up in Prometheus / Grafana as {@code fintrack_service_method_seconds}.
 * Emits a WARN log when a single call exceeds the SLO threshold (default 500ms).
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PerformanceAspect {

    private final MeterRegistry meterRegistry;

    @Value("${fintrack.aspect.performance.slo-ms:500}")
    private long sloMs;

    @Around("execution(* com.fintrack..*Service.*(..))")
    public Object measure(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().getDeclaringType().getSimpleName() + "." + pjp.getSignature().getName();
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return pjp.proceed();
        } finally {
            long ns = sample.stop(Timer.builder("fintrack.service.method")
                    .tag("method", method)
                    .description("Execution time of @Service methods")
                    .publishPercentileHistogram()
                    .register(meterRegistry));
            long ms = TimeUnit.NANOSECONDS.toMillis(ns);
            if (ms > sloMs) {
                log.warn("⏱ SLO breach {} took {}ms (slo={}ms)", method, ms, sloMs);
            }
        }
    }
}
