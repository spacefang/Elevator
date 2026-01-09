package com.elevator.ops.backend.web.auth;

import com.elevator.ops.backend.web.error.ApiException;
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

  public AuthController(StringRedisTemplate stringRedisTemplate) {
    this.stringRedisTemplate = stringRedisTemplate;
  }

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    if (request.username().isBlank() || request.password().isBlank()) {
      throw ApiException.badRequest("username/password required");
    }

    String token = UUID.randomUUID().toString();
    stringRedisTemplate.opsForValue().set(TOKEN_PREFIX + token, request.username(), TOKEN_TTL);
    return new LoginResponse(token, "Bearer", TOKEN_TTL.toSeconds());
  }

  @GetMapping("/me")
  public MeResponse me(@AuthenticationPrincipal String username) {
    if (username == null || username.isBlank()) {
      throw ApiException.unauthorized("Invalid token");
    }
    return new MeResponse(username, "CITY");
  }
}
