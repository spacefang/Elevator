package com.elevator.ops.backend.persistence.alarm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "alarm_actions")
public class AlarmAction {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "alarm_id", nullable = false)
  private Long alarmId;

  @Enumerated(EnumType.STRING)
  @Column(name = "action_type", nullable = false, length = 32)
  private AlarmActionType actionType;

  @Column(name = "note", columnDefinition = "text")
  private String note;

  @Column(name = "operator_user_id", nullable = false, length = 64)
  private String operatorUserId;

  @Column(name = "operator_name", nullable = false, length = 128)
  private String operatorName;

  @Column(name = "operator_role", nullable = false, length = 64)
  private String operatorRole;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public Long getId() {
    return id;
  }

  public Long getAlarmId() {
    return alarmId;
  }

  public void setAlarmId(Long alarmId) {
    this.alarmId = alarmId;
  }

  public AlarmActionType getActionType() {
    return actionType;
  }

  public void setActionType(AlarmActionType actionType) {
    this.actionType = actionType;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public String getOperatorUserId() {
    return operatorUserId;
  }

  public void setOperatorUserId(String operatorUserId) {
    this.operatorUserId = operatorUserId;
  }

  public String getOperatorName() {
    return operatorName;
  }

  public void setOperatorName(String operatorName) {
    this.operatorName = operatorName;
  }

  public String getOperatorRole() {
    return operatorRole;
  }

  public void setOperatorRole(String operatorRole) {
    this.operatorRole = operatorRole;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}

