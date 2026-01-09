package com.elevator.ops.backend.web.auth;

import com.elevator.ops.backend.security.UserCatalog;
import com.elevator.ops.backend.security.UserPrincipal;
import com.elevator.ops.backend.web.error.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private static final Duration TOKEN_TTL = Duration.ofDays(1);
  private static final String TOKEN_PREFIX = "auth:token:";

  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;
  private final UserCatalog userCatalog;

  public AuthController(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper, UserCatalog userCatalog) {
    this.stringRedisTemplate = stringRedisTemplate;
    this.objectMapper = objectMapper;
    this.userCatalog = userCatalog;
  }

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    if (request.username().isBlank() || request.password().isBlank()) {
      throw ApiException.badRequest("username/password required");
    }

    UserPrincipal principal = userCatalog.authenticate(request.username(), request.password());
    if (principal == null) {
      throw ApiException.unauthorized("Invalid username/password");
    }

    String token = UUID.randomUUID().toString();
    try {
      stringRedisTemplate
          .opsForValue()
          .set(TOKEN_PREFIX + token, objectMapper.writeValueAsString(principal), TOKEN_TTL);
    } catch (Exception e) {
      throw ApiException.badRequest("Failed to create token");
    }

    return new LoginResponse(token, "Bearer", TOKEN_TTL.toSeconds(), toMeResponse(principal));
  }

  @GetMapping("/me")
  public MeResponse me(@AuthenticationPrincipal UserPrincipal principal) {
    if (principal == null) {
      throw ApiException.unauthorized("Invalid token/principal");
    }
    return toMeResponse(principal);
  }

  private static MeResponse toMeResponse(UserPrincipal principal) {
    return new MeResponse(
        principal.userId(),
        principal.username(),
        principal.name(),
        principal.role().name(),
        principal.region(),
        principal.city(),
        principal.permissions());
  }
}
