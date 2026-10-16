package com.ecom.orders.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Protects /internal/** endpoints, which are called by other services (Payment Service, warehouse tools),
 * not by customers. Callers send the shared secret in the X-Internal-Token header.
 * TODO: replace with Kafka events / gateway-level service auth (HLD 2, 3.4)
 */
@Component
public class InternalApiInterceptor implements HandlerInterceptor {
    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final byte[] internalApiToken;

    public InternalApiInterceptor(@Value("${internal.api-token:}") String internalApiToken) {
        this.internalApiToken = internalApiToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (internalApiToken.length == 0) {
            // Fail closed: without a configured secret nobody can call internal endpoints
            throw new UnauthorizedException("Internal API token is not configured");
        }
        String provided = request.getHeader(INTERNAL_TOKEN_HEADER);
        if (provided == null
                || !MessageDigest.isEqual(internalApiToken, provided.getBytes(StandardCharsets.UTF_8))) {
            throw new UnauthorizedException("Invalid internal API token");
        }
        return true;
    }
}
