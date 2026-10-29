package com.adminvisitor.exception;

import com.adminvisitor.enums.ErrorCode;
import com.adminvisitor.exception.ErrorResponse;
import com.adminvisitor.exception.ErrorResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class ErrorResponseWriter {

    private final JsonMapper jsonMapper;
    private final ErrorResponseFactory errorResponseFactory;

    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            ErrorCode errorCode
    ) throws IOException {

        var status = errorCode.getHttpStatus();

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErrorResponse errorResponse = errorResponseFactory.create(
                errorCode,
                request.getRequestURI()
        );

        jsonMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
