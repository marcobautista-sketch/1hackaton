package com.tuckersoft.branchengine.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuckersoft.branchengine.dto.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Spring Security devuelve 401 con cuerpo vacio por defecto. Este componente
 * lo intercepta para usar el formato de error del enunciado.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = new ErrorResponse(
                "UNAUTHORIZED",
                "Sin token, token invalido o vencido.",
                Instant.now(),
                request.getRequestURI());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
