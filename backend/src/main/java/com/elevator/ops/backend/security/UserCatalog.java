package com.elevator.ops.backend.security;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class UserCatalog {
  public UserPrincipal authenticate(String username, String password) {
    if (username == null || username.isBlank() || password == null || password.isBlank()) {
      return null;
    }

    // demo password rule: allow "dev" (and keep it flexible for local testing)
    if (!"dev".equals(password)) {
      return null;
    }

    String normalized = username.trim().toLowerCase();
    if (normalized.contains("national")) {
      return new UserPrincipal(
          "1003",
          username,
          "全国管理员",
          UserRole.NATIONAL_ADMIN,
          "全国",
          null,
          List.of("dashboard:view", "stat:view", "alarm:view", "device:view", "device:edit"),
          null,
          null);
    }

    if (normalized.contains("province")) {
      return new UserPrincipal(
          "1002",
          username,
          "湖北省督办员",
          UserRole.PROVINCE_SUPERVISOR,
          "湖北",
          null,
          List.of("alarm:view", "alarm:supervise", "device:view", "device:edit"),
          1,
          10);
    }

    return new UserPrincipal(
        "1001",
        username,
        "武汉执行员",
        UserRole.CITY_OPERATOR,
        "湖北",
        "武汉",
        List.of("alarm:view", "alarm:handle", "device:monitor:realtime", "device:control:emergency", "workorder:execute:city"),
        1,
        5);
  }
}

