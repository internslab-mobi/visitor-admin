
package com.adminvisitor.auth.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Aspect-oriented programming (AOP) component that intercepts all REST controller executions
 * to measure, record, and log execution duration and completion status for incoming HTTP requests.
 *
 * @author Logeshwaran
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class ApiLoggingAspect {

    private final HttpServletRequest request;

    /**
     * Intercepts execution of methods within classes annotated with {@code @RestController},
     * calculating elapsed execution time and logging completion or interruption events.
     *
     * @param joinPoint the proceeding join point representing the intercepted controller method execution
     * @return the execution result returned by the intercepted method
     * @throws Throwable any exception thrown during controller or downstream service execution
     */
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
