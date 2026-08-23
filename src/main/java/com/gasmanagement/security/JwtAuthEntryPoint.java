package com.gasmanagement.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gasmanagement.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Xử lý khi request truy cập endpoint protected mà không có / sai token.
 * Trả về JSON 401 thay vì redirect về login page mặc định của Spring Security.
 */
@Slf4j
@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        log.warn("Unauthorized request to: {} - {}", request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> errorResponse = ApiResponse.error(
                401,
                "Bạn chưa đăng nhập hoặc token không hợp lệ. Vui lòng đăng nhập lại."
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
