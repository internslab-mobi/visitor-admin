package com.adminvisitor.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ApiLoggingAspect {

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logApiExecutionTime(
            ProceedingJoinPoint joinPoint) throws Throwable {

        long startTime = System.nanoTime();

        try {

            Object result = joinPoint.proceed();

            double durationMs =
                    (System.nanoTime() - startTime) / 1_000_000.0;

            log.info(
                    "API_COMPLETED controller={} method={} durationMs={}",
                    joinPoint.getTarget().getClass().getSimpleName(),
                    joinPoint.getSignature().getName(),
                    durationMs
            );

            return result;

        } catch (Throwable ex) {

            double durationMs =
                    (System.nanoTime() - startTime) / 1_000_000.0;

            log.warn(
                    "API_FAILED controller={} method={} durationMs={} exception={}",
                    joinPoint.getTarget().getClass().getSimpleName(),
                    joinPoint.getSignature().getName(),
                    durationMs,
                    ex.getClass().getSimpleName()
            );

            throw ex;
        }
    }
}