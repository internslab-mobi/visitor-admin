package com.adminvisitor.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletResponse;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class ApiLoggingAspect {

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logApiExecutionTime(
            ProceedingJoinPoint joinPoint) throws Throwable {

        long startTime = System.nanoTime();

        String httpMethod =
                request.getMethod();

        String path =
                request.getRequestURI();

        try {

            Object result = joinPoint.proceed();

            double durationMs =
                    (System.nanoTime() - startTime) / 1_000_000.0;

            int status =
                    response.getStatus();

            log.info(
                    "API_COMPLETED method={} path={} status={} durationMs={}",
                    httpMethod,
                    path,
                    status,
                    String.format("%.2f", durationMs)
            );

            return result;

        } catch (Throwable ex) {

            double durationMs =
                    (System.nanoTime() - startTime) / 1_000_000.0;

            int status =
                    response.getStatus();

            log.warn(
                    "API_FAILED method={} path={} status={} durationMs={} exception={}",
                    httpMethod,
                    path,
                    status,
                    String.format("%.2f", durationMs),
                    ex.getClass().getSimpleName()
            );

            throw ex;
        }
    }
}