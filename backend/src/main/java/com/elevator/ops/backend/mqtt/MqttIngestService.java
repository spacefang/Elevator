package com.elevator.ops.backend.mqtt;

import com.elevator.ops.backend.persistence.alarm.Alarm;
import com.elevator.ops.backend.persistence.alarm.AlarmRepository;
import com.elevator.ops.backend.persistence.device.DeviceState;
import com.elevator.ops.backend.persistence.device.DeviceStateChange;
import com.elevator.ops.backend.persistence.device.DeviceStateChangeRepository;
import com.elevator.ops.backend.persistence.device.DeviceStateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "elevator.mqtt", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(MqttProperties.class)
public class MqttIngestService implements MqttCallbackExtended, SmartLifecycle {
  private static final Logger log = LoggerFactory.getLogger(MqttIngestService.class);

  private static final Duration OFFLINE_THRESHOLD = Duration.ofSeconds(15);
  private static final String REDIS_KEY_STATE_PREFIX = "device:state:";

  private final MqttProperties mqttProperties;
  private final ObjectMapper objectMapper;
  private final StringRedisTemplate stringRedisTemplate;
  private final AlarmRepository alarmRepository;
  private final DeviceStateRepository deviceStateRepository;
  private final DeviceStateChangeRepository deviceStateChangeRepository;
  private final AlarmRuleEngine alarmRuleEngine;

  private final MqttClientManager mqttClientManager;
  private volatile boolean running;

  public MqttIngestService(
      MqttProperties mqttProperties,
      ObjectMapper objectMapper,
      StringRedisTemplate stringRedisTemplate,
      AlarmRepository alarmRepository,
      DeviceStateRepository deviceStateRepository,
      DeviceStateChangeRepository deviceStateChangeRepository) {
    this.mqttProperties = mqttProperties;
    this.objectMapper = objectMapper;
    this.stringRedisTemplate = stringRedisTemplate;
    this.alarmRepository = alarmRepository;
    this.deviceStateRepository = deviceStateRepository;
    this.deviceStateChangeRepository = deviceStateChangeRepository;
    this.alarmRuleEngine = new AlarmRuleEngine();

    String clientId =
        (mqttProperties.clientId() == null || mqttProperties.clientId().isBlank())
            ? "elevator-backend-dev"
            : mqttProperties.clientId();
    this.mqttClientManager = new MqttClientManager(mqttProperties, clientId, this);
    this.running = false;
  }

  @Override
  public void start() {
    mqttClientManager.start();
    running = true;
  }

  @Override
  public void stop() {
    running = false;
    mqttClientManager.stop();
  }

  @Override
  public boolean isRunning() {
    return running;
  }

  @Override
  public boolean isAutoStartup() {
    return true;
  }

  @Override
  public int getPhase() {
    return 0;
  }

  @Override
  public void connectComplete(boolean reconnect, String serverURI) {
    log.info("MQTT connectComplete: reconnect={}, serverURI={}", reconnect, serverURI);
    mqttClientManager.subscribe(topicData(), 1);
    mqttClientManager.subscribe(topicAlert(), 1);
  }

  @Override
  public void connectionLost(Throwable cause) {
    log.warn("MQTT connectionLost", cause);
  }

  @Override
  public void messageArrived(String topic, MqttMessage message) {
    String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
    try {
      if (topic.endsWith("/data")) {
        DeviceTelemetry telemetry = objectMapper.readValue(payload, DeviceTelemetry.class);
        handleTelemetry(telemetry);
        return;
      }
      if (topic.endsWith("/alert")) {
        DeviceAlert alert = objectMapper.readValue(payload, DeviceAlert.class);
        handleAlert(alert);
        return;
      }
      log.debug("MQTT ignored topic={}, payload={}", topic, payload);
    } catch (Exception e) {
      log.warn("MQTT parse/handle failed: topic={}, payload={}", topic, payload, e);
    }
  }

  @Override
  public void deliveryComplete(IMqttDeliveryToken token) {}

  private String topicData() {
    return mqttProperties.topicData() == null || mqttProperties.topicData().isBlank()
        ? "elevator/+/data"
        : mqttProperties.topicData();
  }

  private String topicAlert() {
    return mqttProperties.topicAlert() == null || mqttProperties.topicAlert().isBlank()
        ? "elevator/+/alert"
        : mqttProperties.topicAlert();
  }

  private void handleTelemetry(DeviceTelemetry telemetry) {
    if (telemetry.deviceId() == null || telemetry.deviceId().isBlank()) {
      return;
    }
    Instant seenAt = Instant.ofEpochMilli(telemetry.timestampMs());

    // cache latest state for realtime page
    try {
      Map<String, Object> state = new HashMap<>();
      state.put("deviceId", telemetry.deviceId());
      state.put("online", true);
      state.put("lastSeenAt", seenAt.toString());
      state.put("floor", telemetry.floor());
      state.put("direction", telemetry.direction());
      state.put("doorStatus", telemetry.doorStatus());
      state.put("speed", telemetry.speed());
      state.put("load", telemetry.load());
      state.put("temperature", telemetry.temperature());
      state.put("vibration", telemetry.vibration());
      state.put("power", telemetry.power());
      stringRedisTemplate.opsForValue().set(REDIS_KEY_STATE_PREFIX + telemetry.deviceId(), objectMapper.writeValueAsString(state));
    } catch (Exception e) {
      log.debug("Failed to write redis state: deviceId={}", telemetry.deviceId(), e);
    }

    DeviceState deviceState =
        deviceStateRepository
            .findByDeviceId(telemetry.deviceId())
            .orElseGet(
                () -> {
                  DeviceState s = new DeviceState();
                  s.setDeviceId(telemetry.deviceId());
                  s.setOnline(false);
                  s.setLastSeenAt(seenAt);
                  return s;
                });

    boolean wasOnline = deviceState.isOnline();
    deviceState.setOnline(true);
    deviceState.setLastSeenAt(seenAt);
    deviceState.setFloor(telemetry.floor());
    deviceState.setDirection(telemetry.direction());
    deviceState.setDoorStatus(telemetry.doorStatus());
    deviceState.setSpeed(telemetry.speed());
    deviceState.setLoad(telemetry.load());
    deviceState.setTemperature(telemetry.temperature());
    deviceState.setVibration(telemetry.vibration());
    deviceState.setPower(telemetry.power());
    deviceStateRepository.save(deviceState);

    if (!wasOnline) {
      deviceStateChangeRepository.save(DeviceStateChange.of(telemetry.deviceId(), "ONLINE", null));
    }

    Optional<AlarmRuleEngine.DerivedAlarm> derivedAlarm = alarmRuleEngine.evaluate(telemetry);
    derivedAlarm.ifPresent(
        alarm -> {
          Alarm entity = new Alarm();
          entity.setDeviceId(telemetry.deviceId());
          entity.setLevel(alarm.level());
          entity.setType(alarm.type());
          entity.setDescription(alarm.description());
          entity.setOccurredAt(seenAt);
          alarmRepository.save(entity);

          deviceStateChangeRepository.save(
              DeviceStateChange.of(telemetry.deviceId(), "ALARM_RAISED", alarm.type()));
        });
  }

  private void handleAlert(DeviceAlert alert) {
    if (alert.deviceId() == null || alert.deviceId().isBlank()) {
      return;
    }
    Instant occurredAt = Instant.ofEpochMilli(alert.timestampMs());
    Alarm entity = new Alarm();
    entity.setDeviceId(alert.deviceId());
    entity.setLevel(alert.alertLevel());
    entity.setType(alert.alertType());
    entity.setDescription(alert.alertDesc());
    entity.setOccurredAt(occurredAt);
    alarmRepository.save(entity);

    deviceStateChangeRepository.save(
        DeviceStateChange.of(alert.deviceId(), "ALARM_RAISED", alert.alertType()));
  }

  @Scheduled(fixedDelayString = "${elevator.mqtt.offline-check-ms:5000}")
  public void offlineCheck() {
    if (!running) {
      return;
    }
    Instant threshold = Instant.now().minus(OFFLINE_THRESHOLD);
    for (DeviceState deviceState : deviceStateRepository.findByOnlineIsTrueAndLastSeenAtBefore(threshold)) {
      deviceState.setOnline(false);
      deviceStateRepository.save(deviceState);
      deviceStateChangeRepository.save(DeviceStateChange.of(deviceState.getDeviceId(), "OFFLINE", null));
      try {
        String key = REDIS_KEY_STATE_PREFIX + deviceState.getDeviceId();
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json != null && !json.isBlank()) {
          @SuppressWarnings("unchecked")
          Map<String, Object> map = objectMapper.readValue(json, Map.class);
          map.put("online", false);
          stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(map));
        }
      } catch (Exception e) {
        log.debug("Failed to update redis online=false", e);
      }
    }
  }
}
