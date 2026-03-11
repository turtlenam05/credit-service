package com.dathq.swd302.creditservice.filter;

import com.dathq.swd302.creditservice.security.JwtClaims;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;
import java.util.UUID;


@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHeaderFilter extends OncePerRequestFilter {
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String[] parts = token.split("\\.");
                if (parts.length >= 2) {
                    String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                    JsonNode jsonNode = objectMapper.readTree(payload);
                    if (jsonNode.has("sub")) {
                        String userIdStr = jsonNode.get("sub").asText();
                        JwtClaims claims = JwtClaims.builder()
                                .userId(UUID.fromString(userIdStr))
                                .build();
                        request.setAttribute("jwtClaims", claims);
                        log.debug("Extracted User ID from JWT and set as request attribute: {}", userIdStr);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse JWT payload to extract user ID: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
