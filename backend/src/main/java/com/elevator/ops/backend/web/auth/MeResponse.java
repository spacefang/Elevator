package com.elevator.ops.backend.web.auth;

import java.util.List;

public record MeResponse(
    String userId,
    String username,
    String name,
    String role,
    String region,
    String city,
    List<String> permissions) {}
