package com.elevator.ops.backend.web.alarm;

import com.elevator.ops.backend.persistence.alarm.Alarm;
import com.elevator.ops.backend.persistence.alarm.AlarmAction;
import com.elevator.ops.backend.persistence.alarm.AlarmActionRepository;
import com.elevator.ops.backend.persistence.alarm.AlarmActionType;
import com.elevator.ops.backend.persistence.alarm.AlarmRepository;
import com.elevator.ops.backend.persistence.alarm.AlarmStatus;
import com.elevator.ops.backend.security.UserPrincipal;
import com.elevator.ops.backend.web.common.PageResponse;
import com.elevator.ops.backend.web.error.ApiException;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alarms")
public class AlarmController {
  private final AlarmRepository alarmRepository;
  private final AlarmActionRepository alarmActionRepository;

  public AlarmController(AlarmRepository alarmRepository, AlarmActionRepository alarmActionRepository) {
    this.alarmRepository = alarmRepository;
    this.alarmActionRepository = alarmActionRepository;
  }

  @GetMapping
  public PageResponse<AlarmDto> list(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestParam(name = "deviceId", required = false) String deviceId,
      @RequestParam(name = "level", required = false) String level,
      @RequestParam(name = "status", required = false) String status,
      @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
    Specification<Alarm> spec = buildSpec(principal, deviceId, level, status);
    Page<AlarmDto> page = alarmRepository.findAll(spec, pageable).map(AlarmController::toDto);
    List<AlarmDto> content = page.getContent();
    return new PageResponse<>(content, page.getTotalElements(), page.getTotalPages(), page.getNumber(), page.getSize());
  }

  @GetMapping("/{id}")
  public AlarmDetailDto get(@AuthenticationPrincipal UserPrincipal principal, @PathVariable("id") long id) {
    Alarm alarm =
        alarmRepository
            .findById(id)
            .orElseThrow(() -> ApiException.notFound("Alarm not found: " + id));
    if (principal != null && !principal.canAccessDevice(alarm.getDeviceId())) {
      throw ApiException.notFound("Alarm not found: " + id);
    }
    return toDetailDto(alarm, alarmActionRepository.findByAlarmIdOrderByCreatedAtDesc(id));
  }

  private static AlarmDto toDto(Alarm alarm) {
    return new AlarmDto(
        alarm.getId(),
        alarm.getDeviceId(),
        alarm.getLocation(),
        normalizeLevel(alarm.getLevel()),
        alarm.getStatus() == null ? AlarmStatus.PENDING.name() : alarm.getStatus().name(),
        alarm.getType(),
        alarm.getDescription(),
        alarm.getOccurredAt(),
        alarm.getCreatedAt());
  }

  @PostMapping("/{id}/process")
  @Transactional
  public AlarmDetailDto process(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable("id") long id,
      @RequestBody(required = false) AlarmActionRequest request) {
    requirePermission(principal, "alarm:handle");
    Alarm alarm = loadForUpdate(principal, id);
    if (alarm.getStatus() == AlarmStatus.CLOSED) {
      throw ApiException.badRequest("Alarm already closed");
    }
    alarm.setStatus(AlarmStatus.PROCESSING);
    alarmRepository.save(alarm);
    createAction(alarm.getId(), principal, AlarmActionType.PROCESS, request == null ? null : request.note());
    return toDetailDto(alarm, alarmActionRepository.findByAlarmIdOrderByCreatedAtDesc(id));
  }

  @PostMapping("/{id}/close")
  @Transactional
  public AlarmDetailDto close(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable("id") long id,
      @RequestBody(required = false) AlarmActionRequest request) {
    requirePermission(principal, "alarm:handle");
    Alarm alarm = loadForUpdate(principal, id);
    if (alarm.getStatus() == AlarmStatus.CLOSED) {
      return toDetailDto(alarm, alarmActionRepository.findByAlarmIdOrderByCreatedAtDesc(id));
    }
    String note = request == null ? null : request.note();
    if (note == null || note.isBlank()) {
      throw ApiException.badRequest("note required");
    }
    alarm.setStatus(AlarmStatus.CLOSED);
    alarmRepository.save(alarm);
    createAction(alarm.getId(), principal, AlarmActionType.CLOSE, note);
    return toDetailDto(alarm, alarmActionRepository.findByAlarmIdOrderByCreatedAtDesc(id));
  }

  @PostMapping("/{id}/transfer")
  @Transactional
  public AlarmDetailDto transfer(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable("id") long id,
      @RequestBody(required = false) AlarmActionRequest request) {
    requirePermission(principal, "alarm:handle");
    Alarm alarm = loadForUpdate(principal, id);
    if (alarm.getStatus() == AlarmStatus.CLOSED) {
      throw ApiException.badRequest("Alarm already closed");
    }
    createAction(alarm.getId(), principal, AlarmActionType.TRANSFER, request == null ? null : request.note());
    return toDetailDto(alarm, alarmActionRepository.findByAlarmIdOrderByCreatedAtDesc(id));
  }

  @PostMapping("/{id}/supervise")
  @Transactional
  public AlarmDetailDto supervise(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable("id") long id,
      @RequestBody(required = false) AlarmActionRequest request) {
    requirePermission(principal, "alarm:supervise");
    Alarm alarm = loadForUpdate(principal, id);
    createAction(alarm.getId(), principal, AlarmActionType.SUPERVISE, request == null ? null : request.note());
    return toDetailDto(alarm, alarmActionRepository.findByAlarmIdOrderByCreatedAtDesc(id));
  }

  private Alarm loadForUpdate(UserPrincipal principal, long id) {
    Alarm alarm = alarmRepository.findById(id).orElseThrow(() -> ApiException.notFound("Alarm not found: " + id));
    if (principal != null && !principal.canAccessDevice(alarm.getDeviceId())) {
      throw ApiException.notFound("Alarm not found: " + id);
    }
    return alarm;
  }

  private void createAction(long alarmId, UserPrincipal principal, AlarmActionType type, String note) {
    AlarmAction action = new AlarmAction();
    action.setAlarmId(alarmId);
    action.setActionType(type);
    action.setNote(note);
    action.setOperatorUserId(principal.userId());
    action.setOperatorName(principal.name());
    action.setOperatorRole(principal.role().name());
    alarmActionRepository.save(action);
  }

  private static AlarmDetailDto toDetailDto(Alarm alarm, List<AlarmAction> actions) {
    List<AlarmActionDto> items = new ArrayList<>(actions == null ? 0 : actions.size());
    if (actions != null) {
      for (AlarmAction action : actions) {
        items.add(
            new AlarmActionDto(
                action.getId(),
                action.getActionType() == null ? null : action.getActionType().name(),
                action.getNote(),
                action.getOperatorUserId(),
                action.getOperatorName(),
                action.getOperatorRole(),
                action.getCreatedAt()));
      }
    }
    return new AlarmDetailDto(
        alarm.getId(),
        alarm.getDeviceId(),
        alarm.getLocation(),
        normalizeLevel(alarm.getLevel()),
        alarm.getStatus() == null ? AlarmStatus.PENDING.name() : alarm.getStatus().name(),
        alarm.getType(),
        alarm.getDescription(),
        alarm.getOccurredAt(),
        alarm.getCreatedAt(),
        items);
  }

  private static Specification<Alarm> buildSpec(
      UserPrincipal principal, String deviceId, String level, String status) {
    return (root, query, cb) -> {
      List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

      if (principal != null && principal.deviceNoFrom() != null && principal.deviceNoTo() != null) {
        List<String> scopeIds = principal.deviceIdScope();
        if (!scopeIds.isEmpty()) {
          predicates.add(root.get("deviceId").in(scopeIds));
        }
      }

      if (deviceId != null && !deviceId.isBlank()) {
        String like = "%" + deviceId.trim() + "%";
        predicates.add(
            cb.or(cb.like(root.get("deviceId"), like), cb.like(cb.coalesce(root.get("location"), ""), like)));
      }

      if (level != null && !level.isBlank()) {
        predicates.add(cb.equal(root.get("level"), normalizeLevel(level)));
      }

      if (status != null && !status.isBlank()) {
        AlarmStatus parsed = parseStatus(status);
        predicates.add(cb.equal(root.get("status"), parsed));
      }

      return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
    };
  }

  private static String normalizeLevel(String level) {
    if (level == null) {
      return null;
    }
    return level.trim().toUpperCase(Locale.ROOT);
  }

  private static AlarmStatus parseStatus(String status) {
    try {
      return AlarmStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
    } catch (Exception e) {
      throw ApiException.badRequest("Invalid status: " + status);
    }
  }

  private static void requirePermission(UserPrincipal principal, String permission) {
    if (principal == null) {
      throw ApiException.unauthorized("Invalid token/principal");
    }
    if (principal.permissions() == null || !principal.permissions().contains(permission)) {
      throw ApiException.forbidden("Missing permission: " + permission);
    }
  }
}
