
package com.adminvisitor.auth.exception;

import com.adminvisitor.auth.dto.responsedto.ErrorResponse;
import com.adminvisitor.auth.dto.responsedto.ErrorResponseFactory;
import com.adminvisitor.auth.enums.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
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

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(
            AuthException exception,
            HttpServletRequest request) {

        AuthErrorCode errorCode = exception.getErrorCode();
        String path = request.getRequestURI();
        HttpStatus status = errorCode.getHttpStatus();

        if (status.is5xxServerError()) {
            log.error(
                    "Auth application exception. method={} path={} code={}",
                    request.getMethod(),
                    path,
                    errorCode,
                    exception
            );
        } else {
            log.warn(
                    "Auth application exception. method={} path={} code={}",
                    request.getMethod(),
                    path,
                    errorCode
            );
        }

        return respond(
                status,
                errorCode.name(),
                errorCode.getDefaultMessage(),
                path,
                null
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request) {

        log.warn(
                "Invalid argument. method={} path={} exceptionType={}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getSimpleName()
        );

        return respond(
                HttpStatus.BAD_REQUEST,
                AuthErrorCode.INVALID_REQUEST.name(),
                AuthErrorCode.INVALID_REQUEST.getDefaultMessage(),
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException exception,
            HttpServletRequest request) {

        log.warn(
                "Authentication failed. method={} path={} exceptionType={}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getSimpleName()
        );

        return respond(
                HttpStatus.UNAUTHORIZED,
                AuthErrorCode.INVALID_CREDENTIALS.name(),
                AuthErrorCode.INVALID_CREDENTIALS.getDefaultMessage(),
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException exception,
            HttpServletRequest request) {

        log.error(
                "Authentication failed. method={} path={} exceptionType={}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getSimpleName(),
                exception
        );

        return respond(
                HttpStatus.UNAUTHORIZED,
                AuthErrorCode.INVALID_CREDENTIALS.name(),
                AuthErrorCode.INVALID_CREDENTIALS.getDefaultMessage(),
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request) {

        log.warn(
                "Authorization denied. method={} path={} exceptionType={}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getSimpleName()
        );

        return respond(
                HttpStatus.FORBIDDEN,
                AuthErrorCode.FORBIDDEN.name(),
                AuthErrorCode.FORBIDDEN.getDefaultMessage(),
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request) {

        log.error(
                "Unhandled exception. method={} path={}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        return respond(
                HttpStatus.INTERNAL_SERVER_ERROR,
                AuthErrorCode.INTERNAL_ERROR.name(),
                AuthErrorCode.INTERNAL_ERROR.getDefaultMessage(),
                request.getRequestURI(),
                null
        );
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();

        exception.getBindingResult().getFieldErrors()
                .forEach(error ->
                        fieldErrors.putIfAbsent(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        String path = pathOf(request);
        String method = methodOf(request);

        log.warn(
                "Request validation failed. method={} path={} errorCount={}",
                method,
                path,
                fieldErrors.size()
        );

        AuthErrorCode errorCode = AuthErrorCode.VALIDATION_FAILED;

        ErrorResponse errorResponse = errorResponseFactory.create(
                errorCode.getHttpStatus(),
                errorCode.name(),
                errorCode.getDefaultMessage(),
                path,
                fieldErrors
        );

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .headers(headers)
                .body(errorResponse);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {

        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        String path = pathOf(request);
        String method = methodOf(request);

        if (status.is5xxServerError()) {
            log.error(
                    "Framework exception. method={} status={} path={}",
                    method,
                    status.value(),
                    path,
                    exception
            );
        } else {
            log.warn(
                    "Request rejected. method={} status={} path={} exceptionType={}",
                    method,
                    status.value(),
                    path,
                    exception.getClass().getSimpleName()
            );
        }

        AuthErrorCode errorCode = switch (status) {
            case BAD_REQUEST -> AuthErrorCode.INVALID_REQUEST;
            case UNAUTHORIZED -> AuthErrorCode.INVALID_CREDENTIALS;
            case FORBIDDEN -> AuthErrorCode.FORBIDDEN;
            case INTERNAL_SERVER_ERROR -> AuthErrorCode.INTERNAL_ERROR;
            default -> null;
        };

        ErrorResponse errorResponse;

        if (errorCode != null) {
            errorResponse = errorResponseFactory.create(
                    status,
                    errorCode.name(),
                    errorCode.getDefaultMessage(),
                    path,
                    null
            );
        } else {
            errorResponse = errorResponseFactory.create(
                    status,
                    status.name(),
                    status.getReasonPhrase(),
                    path,
                    null
            );
        }

        return ResponseEntity
                .status(statusCode)
                .headers(headers)
                .body(errorResponse);
    }

    // ---------- Helpers ----------

    private ResponseEntity<ErrorResponse> respond(
            HttpStatus status,
            String code,
            String message,
            String path,
            Map<String, String> fieldErrors) {

        ErrorResponse response = errorResponseFactory.create(
                status,
                code,
                message,
                path,
                fieldErrors
        );

        return ResponseEntity.status(status).body(response);
    }

    private String pathOf(WebRequest request) {
        return request instanceof ServletWebRequest servletWebRequest
                ? servletWebRequest.getRequest().getRequestURI()
                : "";
    }

    private String methodOf(WebRequest request) {
        return request instanceof ServletWebRequest servletWebRequest
                ? servletWebRequest.getRequest().getMethod()
                : "UNKNOWN";
    }
}
