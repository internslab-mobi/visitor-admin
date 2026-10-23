package com.adminvisitor.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration class that activates Spring's asynchronous method execution capability
 * and establishes a global uncaught exception handler for background tasks.
 *
 * @author Logeshwaran
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Provides the exception handler invoked when an {@code @Async} method throws an unhandled exception.
     *
     * @return the {@link AsyncUncaughtExceptionHandler} that logs asynchronous execution failures
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error(
                    "Async operation failed. method={}",
                    method.getName(),
                    throwable
            );
        };
    }
}