package com.prismnetai.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);

    private final ApiKeyService apiKeyService;

    public ApiKeyAuthFilter(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        
        // Allow unauthenticated access to public/system endpoints
        if ("/".equals(path)
                || path.equals("/actuator/health")
                || path.startsWith("/actuator")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or malformed Authorization header for path {}", path);
            unauthorized(response);
            return;
        }
        String token = authHeader.substring("Bearer ".length()).trim();

        Optional<ApiKeyRecord> rec = apiKeyService.authenticate(token);
        if (rec.isEmpty()) {
            log.warn("Invalid API key presented (length {}), path {}", token.length(), path);
            unauthorized(response);
            return;
        }

        // Attach client identity for downstream handlers to use; controllers should prefer this
        ApiKeyRecord apiKeyRecord = rec.get();
        request.setAttribute("clientId", apiKeyRecord.getClientId());
        request.setAttribute("apiKeyId", apiKeyRecord.getId());

        filterChain.doFilter(request, response);
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":{\"message\":\"Unauthorized\",\"type\":\"invalid_api_key\"}}\n");
    }
}
