package com.adminvisitor.exception;

import com.adminvisitor.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final ErrorResponseFactory errorResponseFactory;


    @ExceptionHandler(VmsException.class)
    public ResponseEntity<ErrorResponse> handleVmsException(
            VmsException exception, HttpServletRequest request) {

        ErrorCode errorCode = exception.getErrorCode();
        String path = request.getRequestURI();

        if (errorCode.getHttpStatus().is5xxServerError()) {
            log.error("VMS application exception. code={}, path={}",
                    errorCode, path, exception);

            return respond(errorCode, path);
        }

        log.warn("VMS application exception. code={}, path={}",
                errorCode, path);

        return respond(errorCode, exception.getMessage(), path);
    }


    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException exception, HttpServletRequest request) {

        log.warn("Data integrity violation. path={}", request.getRequestURI(), exception);
        return respond(ErrorCode.DUPLICATE_RESOURCE, request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException exception, HttpServletRequest request) {

        log.warn("Authorization denied. method={}, path={}",
                request.getMethod(), request.getRequestURI());
        return respond(ErrorCode.FORBIDDEN, request.getRequestURI());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException exception, HttpServletRequest request) {

        log.warn("Authentication failed. method={}, path={}",
                request.getMethod(), request.getRequestURI());
        return respond(ErrorCode.UNAUTHORIZED, request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception, HttpServletRequest request) {

        log.error("Unhandled exception. path={}", request.getRequestURI(), exception);
        return respond(ErrorCode.INTERNAL_ERROR, request.getRequestURI());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(e -> fieldErrors.putIfAbsent(e.getField(), e.getDefaultMessage()));

        String path = pathOf(request);
        log.warn("Request validation failed. path={}, errorCount={}", path, fieldErrors.size());

        ErrorCode code = ErrorCode.VALIDATION_FAILED;
        return ResponseEntity.status(code.getHttpStatus()).body(
                errorResponseFactory.create(code, code.getDefaultMessage(), path, fieldErrors));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {

        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        String path = pathOf(request);

        if (status.is5xxServerError()) {
            log.error("Framework exception. status={}, path={}", status.value(), path, exception);
        } else {
            log.warn("Request rejected. status={}, path={}, reason={}",
                    status.value(), path, exception.getClass().getSimpleName());
        }

        ErrorResponse errorResponse = switch (status) {
            case BAD_REQUEST -> errorResponseFactory.create(ErrorCode.INVALID_REQUEST, path);
            case NOT_FOUND -> errorResponseFactory.create(ErrorCode.RESOURCE_NOT_FOUND, path);
            case INTERNAL_SERVER_ERROR -> errorResponseFactory.create(ErrorCode.INTERNAL_ERROR, path);
            default -> errorResponseFactory.create(status, path);   // 405, 415, 406...
        };

        return ResponseEntity.status(statusCode).headers(headers).body(errorResponse);
    }

    // ---------- Helpers ----------
    private ResponseEntity<ErrorResponse> respond(ErrorCode code, String path) {
        return respond(code, code.getDefaultMessage(), path);
    }

    private ResponseEntity<ErrorResponse> respond(ErrorCode code, String message, String path) {
        return ResponseEntity.status(code.getHttpStatus())
                .body(errorResponseFactory.create(code, message, path, null));
    }

    private String pathOf(WebRequest request) {
        return request instanceof ServletWebRequest swr ? swr.getRequest().getRequestURI() : "";
    }
}