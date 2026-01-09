package com.elevator.ops.backend.security;

import com.elevator.ops.backend.web.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class TokenAuthFilter extends OncePerRequestFilter {
  private static final String TOKEN_PREFIX = "auth:token:";

  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;

  public TokenAuthFilter(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
    this.stringRedisTemplate = stringRedisTemplate;
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return !path.startsWith("/api/")
        || path.equals("/api/ping")
        || path.equals("/api/auth/login")
        || path.startsWith("/swagger-ui")
        || path.startsWith("/v3/api-docs")
        || path.startsWith("/actuator/health");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authorization == null || authorization.isBlank()) {
      unauthorized(response, "Missing Authorization header");
      return;
    }

    String token = authorization.startsWith("Bearer ") ? authorization.substring("Bearer ".length()) : authorization;
    String json = stringRedisTemplate.opsForValue().get(TOKEN_PREFIX + token);
    if (json == null || json.isBlank()) {
      unauthorized(response, "Invalid token");
      return;
    }

    UserPrincipal principal;
    try {
      principal = objectMapper.readValue(json, UserPrincipal.class);
    } catch (Exception e) {
      unauthorized(response, "Invalid token payload");
      return;
    }

    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
    if (principal.role() != null) {
      authorities.add(new SimpleGrantedAuthority("ROLE_" + principal.role().name()));
    }
    if (principal.permissions() != null) {
      for (String permission : principal.permissions()) {
        if (permission != null && !permission.isBlank()) {
          authorities.add(new SimpleGrantedAuthority(permission));
        }
      }
    }

    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            principal, null, authorities);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    filterChain.doFilter(request, response);
  }

  private void unauthorized(HttpServletResponse response, String message) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), new ErrorResponse("UNAUTHORIZED", message));
  }
}
