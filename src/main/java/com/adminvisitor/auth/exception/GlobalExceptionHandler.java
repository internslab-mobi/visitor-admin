
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

/**
 * Centralized exception handling advice intercepting controller exceptions across the application.
 * <p>
 * Translates domain authentication exceptions, validation failures, access denial errors,
 * bad credentials, and unhandled runtime exceptions into standardized {@link ErrorResponse} payloads.
 * </p>
 *
 * @author Logeshwaran
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final ErrorResponseFactory errorResponseFactory;

    /**
     * Handles domain-specific {@link AuthException}s and its subclasses, translating
     * the underlying {@link AuthErrorCode} into the appropriate HTTP status and response payload.
     *
     * @param exception the intercepted domain authentication exception
     * @param request the current HTTP servlet request
     * @return a {@link ResponseEntity} wrapping the standardized {@link ErrorResponse}
     */
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

    /**
     * Handles invalid argument exceptions originating from parameter validation or bad input.
     *
     * @param exception the illegal argument exception
     * @param request the current HTTP servlet request
     * @return a {@link ResponseEntity} wrapping the standardized {@link ErrorResponse} with BAD_REQUEST status
     */
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

    /**
     * Handles bad credentials exceptions when user authentication fails due to incorrect passwords or emails.
     *
     * @param exception the bad credentials exception
     * @param request the current HTTP servlet request
     * @return a {@link ResponseEntity} wrapping the standardized {@link ErrorResponse} with UNAUTHORIZED status
     */
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

    /**
     * Handles generic Spring Security authentication exceptions.
     *
     * @param exception the authentication exception
     * @param request the current HTTP servlet request
     * @return a {@link ResponseEntity} wrapping the standardized {@link ErrorResponse} with UNAUTHORIZED status
     */
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

    /**
     * Handles authorization denial exceptions when an authenticated user lacks requisite privileges.
     *
     * @param exception the access denied exception
     * @param request the current HTTP servlet request
     * @return a {@link ResponseEntity} wrapping the standardized {@link ErrorResponse} with FORBIDDEN status
     */
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

    /**
     * Handles unexpected or unhandled general exceptions, returning a generic 500 error response.
     *
     * @param exception the unhandled exception
     * @param request the current HTTP servlet request
     * @return a {@link ResponseEntity} wrapping the standardized {@link ErrorResponse} with INTERNAL_SERVER_ERROR status
     */
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

    /**
     * Handles bean validation constraint failures occurring on {@code @Valid} annotated controller request bodies.
     *
     * @param exception the method argument not valid exception
     * @param headers the HTTP headers for the response
     * @param status the HTTP status code
     * @param request the current web request
     * @return a {@link ResponseEntity} wrapping the {@link ErrorResponse} populated with field validation details
     */
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

    /**
     * Handles standard Spring MVC framework exceptions, mapping known HTTP statuses to application error codes.
     *
     * @param exception the internal framework exception
     * @param body the response body
     * @param headers the response headers
     * @param statusCode the HTTP status code
     * @param request the current web request
     * @return a {@link ResponseEntity} wrapping the configured {@link ErrorResponse}
     */
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

    /**
     * Helper method to build a {@link ResponseEntity} enclosing an {@link ErrorResponse}.
     *
     * @param status the HTTP response status
     * @param code the machine-readable error code
     * @param message the descriptive error message
     * @param path the request URI path
     * @param fieldErrors optional field-level validation errors
     * @return the constructed {@link ResponseEntity}
     */
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

    /**
     * Extracts the request URI path from the given {@link WebRequest}.
     *
     * @param request the web request
     * @return the request URI, or empty string if unavailable
     */
    private String pathOf(WebRequest request) {
        return request instanceof ServletWebRequest servletWebRequest
                ? servletWebRequest.getRequest().getRequestURI()
                : "";
    }

    /**
     * Extracts the HTTP method name from the given {@link WebRequest}.
     *
     * @param request the web request
     * @return the HTTP method name, or {@code "UNKNOWN"} if unavailable
     */
    private String methodOf(WebRequest request) {
        return request instanceof ServletWebRequest servletWebRequest
                ? servletWebRequest.getRequest().getMethod()
                : "UNKNOWN";
    }
}
