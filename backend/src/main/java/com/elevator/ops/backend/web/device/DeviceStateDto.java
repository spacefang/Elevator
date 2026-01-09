package com.elevator.ops.backend.web.device;

import java.time.Instant;

public record DeviceStateDto(
    String deviceId,
    boolean online,
    Instant lastSeenAt,
    Integer floor,
    String direction,
    String doorStatus,
    Double speed,
    Double load,
    Double temperature,
    Double vibration,
    Double power) {}

