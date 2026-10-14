package com.adminvisitor.auth.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class ApiLoggingAspect {

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logApiExecutionTime(ProceedingJoinPoint joinPoint)
            throws Throwable {

        long startTime = System.nanoTime();

        String httpMethod = request.getMethod();
        String path = request.getRequestURI();

        try {
            Object result = joinPoint.proceed();

            long durationMs = TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - startTime
            );

            log.info(
                    "API_COMPLETED method={} path={} status={} durationMs={}",
                    httpMethod,
                    path,
                    response.getStatus(),
                    durationMs
            );

            return result;

        } catch (Throwable ex) {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - startTime
            );

            log.warn(
                    "API_FAILED method={} path={} status={} durationMs={} exception={}",
                    httpMethod,
                    path,
                    response.getStatus(),
                    durationMs,
                    ex.getClass().getSimpleName()
            );

            throw ex;
        }
    }
}