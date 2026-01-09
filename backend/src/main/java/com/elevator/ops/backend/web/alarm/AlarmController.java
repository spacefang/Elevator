package com.elevator.ops.backend.web.alarm;

import com.elevator.ops.backend.persistence.alarm.Alarm;
import com.elevator.ops.backend.persistence.alarm.AlarmRepository;
import com.elevator.ops.backend.web.common.PageResponse;
import com.elevator.ops.backend.web.error.ApiException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alarms")
public class AlarmController {
  private final AlarmRepository alarmRepository;

  public AlarmController(AlarmRepository alarmRepository) {
    this.alarmRepository = alarmRepository;
  }

  @GetMapping
  public PageResponse<AlarmDto> list(@PageableDefault(size = 20) Pageable pageable) {
    Page<AlarmDto> page = alarmRepository.findAllByOrderByOccurredAtDesc(pageable).map(AlarmController::toDto);
    List<AlarmDto> content = page.getContent();
    return new PageResponse<>(content, page.getTotalElements(), page.getTotalPages(), page.getNumber(), page.getSize());
  }

  @GetMapping("/{id}")
  public AlarmDto get(@PathVariable("id") long id) {
    Alarm alarm =
        alarmRepository
            .findById(id)
            .orElseThrow(() -> ApiException.notFound("Alarm not found: " + id));
    return toDto(alarm);
  }

  private static AlarmDto toDto(Alarm alarm) {
    return new AlarmDto(
        alarm.getId(),
        alarm.getDeviceId(),
        alarm.getLevel(),
        alarm.getType(),
        alarm.getDescription(),
        alarm.getOccurredAt(),
        alarm.getCreatedAt());
  }
}
