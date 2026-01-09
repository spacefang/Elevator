package com.elevator.ops.backend.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record UserPrincipal(
    String userId,
    String username,
    String name,
    UserRole role,
    String region,
    String city,
    List<String> permissions,
    Integer deviceNoFrom,
    Integer deviceNoTo) {

  public UserPrincipal {
    permissions = permissions == null ? List.of() : List.copyOf(permissions);
  }

  public List<String> deviceIdScope() {
    if (deviceNoFrom == null || deviceNoTo == null) {
      return Collections.emptyList();
    }
    int from = Math.min(deviceNoFrom, deviceNoTo);
    int to = Math.max(deviceNoFrom, deviceNoTo);
    List<String> ids = new ArrayList<>(Math.max(0, to - from + 1));
    for (int i = from; i <= to; i++) {
      ids.add(String.format("E%04d", i));
    }
    return ids;
  }

  public boolean canAccessDevice(String deviceId) {
    if (deviceNoFrom == null || deviceNoTo == null) {
      return true;
    }
    Integer no = parseDeviceNo(deviceId);
    if (no == null) {
      return false;
    }
    int from = Math.min(deviceNoFrom, deviceNoTo);
    int to = Math.max(deviceNoFrom, deviceNoTo);
    return no >= from && no <= to;
  }

  private static Integer parseDeviceNo(String deviceId) {
    if (deviceId == null) {
      return null;
    }
    String trimmed = deviceId.trim();
    if (trimmed.length() < 2 || !(trimmed.startsWith("E") || trimmed.startsWith("e"))) {
      return null;
    }
    try {
      return Integer.parseInt(trimmed.substring(1));
    } catch (NumberFormatException ignored) {
      return null;
    }
  }
}

