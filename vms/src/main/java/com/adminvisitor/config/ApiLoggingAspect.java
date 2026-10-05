package com.adminvisitor.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ApiLoggingAspect {

    private final HttpServletRequest request;

    public ApiLoggingAspect(HttpServletRequest request) {
        this.request = request;
    }

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logApiExecutionTime(
            ProceedingJoinPoint joinPoint) throws Throwable {

        long startTime = System.nanoTime();

        String controller =
                joinPoint.getTarget().getClass().getSimpleName();

        String method =
                joinPoint.getSignature().getName();

        String httpMethod =
                request.getMethod();

        String path =
                request.getRequestURI();

        try {

            Object result = joinPoint.proceed();

            double durationMs =
                    (System.nanoTime() - startTime) / 1_000_000.0;

            log.info(
                    "API_COMPLETED method={} path={} controller={} action={} durationMs={}",
                    httpMethod,
                    path,
                    controller,
                    method,
                    String.format("%.2f", durationMs)
            );

            return result;

        } catch (Throwable ex) {

            double durationMs =
                    (System.nanoTime() - startTime) / 1_000_000.0;

            log.warn(
                    "API_FAILED method={} path={} controller={} action={} durationMs={} exception={}",
                    httpMethod,
                    path,
                    controller,
                    method,
                    String.format("%.2f", durationMs),
                    ex.getClass().getSimpleName()
            );

            throw ex;
        }
    }
}