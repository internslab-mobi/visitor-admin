package com.adminvisitor.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class ApiLoggingAspect {

    private final HttpServletRequest request;

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logApiExecutionTime(
            ProceedingJoinPoint joinPoint) throws Throwable {

        long startTime = System.nanoTime();

        String httpMethod = request.getMethod();
        String path = request.getRequestURI();

        try {
            Object result = joinPoint.proceed();

            double durationMs =
                    (System.nanoTime() - startTime) / 1_000_000.0;

            log.info(
                    "API_COMPLETED method={} path={} durationMs={}",
                    httpMethod,
                    path,
                    String.format("%.2f", durationMs)
            );

            return result;

        } catch (Throwable ex) {
            double durationMs =
                    (System.nanoTime() - startTime) / 1_000_000.0;

            log.warn(
                    "API_EXECUTION_INTERRUPTED method={} path={} durationMs={}",
                    httpMethod,
                    path,
                    String.format("%.2f", durationMs)
            );

            throw ex;
        }
    }
}
