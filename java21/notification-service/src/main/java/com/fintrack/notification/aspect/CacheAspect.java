package com.fintrack.notification.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class CacheAspect {

    @Around("@annotation(org.springframework.cache.annotation.Cacheable)")
    public Object trace(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        Cacheable annotation = method.getAnnotation(Cacheable.class);
        String regions = Arrays.toString(annotation.value().length == 0 ? annotation.cacheNames() : annotation.value());
        String key = Arrays.toString(pjp.getArgs());
        long start = System.nanoTime();
        Object result = pjp.proceed();
        long ms = (System.nanoTime() - start) / 1_000_000;
        String label = ms < 1 ? "HIT(likely)" : "MISS";
        log.debug("⛁ CACHE {} {} regions={} key={} took={}ms", label, sig.toShortString(), regions, key, ms);
        return result;
    }
}
