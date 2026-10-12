package com.adminvisitor.exception;

import com.adminvisitor.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        fieldErrors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        log.warn(
                "Request validation failed. requestId={}, path={}, errorCount={}",
                MDC.get("requestId"),
                request.getRequestURI(),
                fieldErrors.size()
        );

        ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;

        return buildErrorResponse(
                errorCode.getHttpStatus(),
                errorCode,
                errorCode.getPublicMessage(),
                request,
                fieldErrors
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {

        log.warn(
                "Invalid request body. requestId={}, path={}",
                MDC.get("requestId"),
                request.getRequestURI()
        );

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_BODY;

        return buildErrorResponse(
                errorCode.getHttpStatus(),
                errorCode,
                errorCode.getPublicMessage(),
                request,
                null
        );
    }

    @ExceptionHandler(VmsException.class)
    public ResponseEntity<ErrorResponse> handleVmsException(
            VmsException exception,
            HttpServletRequest request) {

        ErrorCode errorCode = exception.getErrorCode();
        HttpStatus status = errorCode.getHttpStatus();

        if (status.is5xxServerError()) {
            log.error(
                    "VMS application exception. code={}, requestId={}, path={}",
                    errorCode,
                    MDC.get("requestId"),
                    request.getRequestURI(),
                    exception
            );
        } else {
            log.warn(
                    "VMS application exception. code={}, status={}, requestId={}, path={}",
                    errorCode,
                    status.value(),
                    MDC.get("requestId"),
                    request.getRequestURI()
            );
        }

        return buildErrorResponse(
                status,
                errorCode,
                errorCode.getPublicMessage(),
                request,
                null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request) {

        String requestId = MDC.get("requestId");

        log.error(
                "Unhandled exception. requestId={}, path={}",
                requestId,
                request.getRequestURI(),
                exception
        );

        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;

        return buildErrorResponse(
                errorCode.getHttpStatus(),
                errorCode,
                errorCode.getPublicMessage(),
                request,
                null
        );
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status,
            ErrorCode errorCode,
            String message,
            HttpServletRequest request,
            Map<String, String> fieldErrors) {

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                errorCode.name(),
                message,
                request.getRequestURI(),
                MDC.get("requestId"),
                fieldErrors
        );

        return ResponseEntity
                .status(status)
                .body(errorResponse);
    }
}

