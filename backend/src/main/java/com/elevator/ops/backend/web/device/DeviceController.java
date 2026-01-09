package com.elevator.ops.backend.web.device;

import com.elevator.ops.backend.persistence.device.DeviceState;
import com.elevator.ops.backend.persistence.device.DeviceStateChange;
import com.elevator.ops.backend.persistence.device.DeviceStateChangeRepository;
import com.elevator.ops.backend.persistence.device.DeviceStateRepository;
import com.elevator.ops.backend.web.common.PageResponse;
import com.elevator.ops.backend.web.error.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {
  private static final String REDIS_KEY_STATE_PREFIX = "device:state:";

  private final DeviceStateRepository deviceStateRepository;
  private final DeviceStateChangeRepository deviceStateChangeRepository;
  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;

  public DeviceController(
      DeviceStateRepository deviceStateRepository,
      DeviceStateChangeRepository deviceStateChangeRepository,
      StringRedisTemplate stringRedisTemplate,
      ObjectMapper objectMapper) {
    this.deviceStateRepository = deviceStateRepository;
    this.deviceStateChangeRepository = deviceStateChangeRepository;
    this.stringRedisTemplate = stringRedisTemplate;
    this.objectMapper = objectMapper;
  }

  @GetMapping
  public PageResponse<DeviceStateDto> list(@PageableDefault(size = 20) Pageable pageable) {
    Page<DeviceStateDto> page = deviceStateRepository.findAll(pageable).map(DeviceController::toDto);
    return new PageResponse<>(page.getContent(), page.getTotalElements(), page.getTotalPages(), page.getNumber(), page.getSize());
  }

  @GetMapping("/{deviceId}/realtime")
  public DeviceStateDto realtime(@PathVariable String deviceId) {
    String json = stringRedisTemplate.opsForValue().get(REDIS_KEY_STATE_PREFIX + deviceId);
    if (json != null && !json.isBlank()) {
      try {
        return objectMapper.readValue(json, DeviceStateDto.class);
      } catch (Exception ignored) {
        // fallback to db
      }
    }

    DeviceState state =
        deviceStateRepository
            .findByDeviceId(deviceId)
            .orElseThrow(() -> ApiException.notFound("Device not found: " + deviceId));
    return toDto(state);
  }

  @GetMapping("/{deviceId}/events")
  public PageResponse<DeviceStateChangeDto> events(
      @PathVariable String deviceId, @PageableDefault(size = 20) Pageable pageable) {
    Page<DeviceStateChangeDto> page =
        deviceStateChangeRepository
            .findByDeviceIdOrderByOccurredAtDesc(deviceId, pageable)
            .map(DeviceController::toDto);
    List<DeviceStateChangeDto> content = page.getContent();
    return new PageResponse<>(content, page.getTotalElements(), page.getTotalPages(), page.getNumber(), page.getSize());
  }

  private static DeviceStateDto toDto(DeviceState state) {
    return new DeviceStateDto(
        state.getDeviceId(),
        state.isOnline(),
        state.getLastSeenAt(),
        state.getFloor(),
        state.getDirection(),
        state.getDoorStatus(),
        state.getSpeed(),
        state.getLoad(),
        state.getTemperature(),
        state.getVibration(),
        state.getPower());
  }

  private static DeviceStateChangeDto toDto(DeviceStateChange change) {
    return new DeviceStateChangeDto(
        change.getId(), change.getDeviceId(), change.getEventType(), change.getDetails(), change.getOccurredAt());
  }
}

