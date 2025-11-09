package com.prismnetai.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${prismnet.jwt.secret}")
    private String jwtSecret;

    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        log.debug("JwtAuthenticationFilter.doFilterInternal() - Processing request: {} {}",
                  request.getMethod(), request.getRequestURI());

        final String token = getTokenFromRequest(request);

        if (StringUtils.hasText(token)) {
            log.debug("JwtAuthenticationFilter.doFilterInternal() - Token found in request, attempting authentication");

            try {
                final String username = getUsernameFromToken(token);
                log.debug("JwtAuthenticationFilter.doFilterInternal() - Extracted username from token: {}", username);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    log.debug("JwtAuthenticationFilter.doFilterInternal() - No existing authentication, loading user details for: {}", username);

                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    if (validateToken(token, userDetails)) {
                        log.info("JwtAuthenticationFilter.doFilterInternal() - Token validation successful for user: {}", username);

                        UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                            );
                        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                        log.debug("JwtAuthenticationFilter.doFilterInternal() - Authentication set in security context for user: {}", username);
                    } else {
                        log.warn("JwtAuthenticationFilter.doFilterInternal() - Token validation failed for user: {}", username);
                    }
                } else {
                    log.debug("JwtAuthenticationFilter.doFilterInternal() - Authentication already exists or username is null");
                }
            } catch (Exception e) {
                log.error("JwtAuthenticationFilter.doFilterInternal() - Cannot set user authentication for token: {}", e.getMessage(), e);
            }
        } else {
            log.debug("JwtAuthenticationFilter.doFilterInternal() - No token found in request, proceeding without authentication");
        }

        log.debug("JwtAuthenticationFilter.doFilterInternal() - Filter processing completed");
        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        final String bearerToken = request.getHeader("Authorization");
        final String apiKey = request.getHeader("X-API-Key");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            log.debug("JwtAuthenticationFilter.getTokenFromRequest() - Bearer token found, length: {}", token.length());
            return token;
        }

        if (StringUtils.hasText(apiKey)) {
            log.debug("JwtAuthenticationFilter.getTokenFromRequest() - API key found, length: {}", apiKey.length());
            return apiKey;
        }

        log.debug("JwtAuthenticationFilter.getTokenFromRequest() - No authentication token found");
        return null;
    }

    private String getUsernameFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String username = claims.getSubject();
            log.debug("JwtAuthenticationFilter.getUsernameFromToken() - Successfully extracted username: {}", username);
            return username;
        } catch (Exception e) {
            log.error("JwtAuthenticationFilter.getUsernameFromToken() - Failed to extract username from token: {}", e.getMessage());
            return null;
        }
    }

    private boolean validateToken(String token, UserDetails userDetails) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String username = claims.getSubject();
            boolean isValidUsername = username.equals(userDetails.getUsername());
            boolean isNotExpired = !isTokenExpired(claims);

            log.debug("JwtAuthenticationFilter.validateToken() - Token validation for user: {}, username match: {}, not expired: {}",
                      username, isValidUsername, isNotExpired);

            return isValidUsername && isNotExpired;
        } catch (Exception e) {
            log.error("JwtAuthenticationFilter.validateToken() - Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(Claims claims) {
        boolean expired = claims.getExpiration().before(new java.util.Date());
        if (expired) {
            log.warn("JwtAuthenticationFilter.isTokenExpired() - Token has expired, expiration: {}", claims.getExpiration());
        }
        return expired;
    }
}