package com.elevator.ops.backend.mqtt;

import java.util.Optional;

public class AlarmRuleEngine {
  public Optional<DerivedAlarm> evaluate(DeviceTelemetry telemetry) {
    if (telemetry == null || telemetry.deviceId() == null || telemetry.deviceId().isBlank()) {
      return Optional.empty();
    }

    if ("stuck".equalsIgnoreCase(telemetry.doorStatus())) {
      return Optional.of(new DerivedAlarm("orange", "door_fault", "门系统异常（模拟）"));
    }

    if (telemetry.temperature() >= 60) {
      return Optional.of(new DerivedAlarm("yellow", "temperature_high", "温度偏高（模拟）"));
    }

    if (telemetry.vibration() >= 0.6) {
      return Optional.of(new DerivedAlarm("yellow", "vibration_high", "振动偏高（模拟）"));
    }

    if (telemetry.vibration() >= 1.2) {
      return Optional.of(new DerivedAlarm("orange", "vibration_high", "振动超标（模拟）"));
    }

    return Optional.empty();
  }

  public record DerivedAlarm(String level, String type, String description) {}
}

