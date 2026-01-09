package com.elevator.ops.backend.sim;

import com.elevator.ops.backend.mqtt.DeviceAlert;
import com.elevator.ops.backend.mqtt.DeviceTelemetry;
import com.elevator.ops.backend.mqtt.MqttClientManager;
import com.elevator.ops.backend.mqtt.MqttProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "elevator.simulator", name = "enabled", havingValue = "true")
@EnableConfigurationProperties({SimulatorProperties.class, MqttProperties.class})
public class ElevatorDataSimulator implements SmartLifecycle {
  private static final Logger log = LoggerFactory.getLogger(ElevatorDataSimulator.class);

  private final SimulatorProperties simulatorProperties;
  private final ObjectMapper objectMapper;
  private final MqttClientManager publisher;
  private final List<ElevatorSim> devices;
  private final ScheduledExecutorService executor;
  private final long intervalMs;
  private volatile boolean running;

  public ElevatorDataSimulator(
      SimulatorProperties simulatorProperties, MqttProperties mqttProperties, ObjectMapper objectMapper) {
    this.simulatorProperties = simulatorProperties;
    this.objectMapper = objectMapper;

    String clientId = "elevator-simulator-" + UUID.randomUUID();
    this.publisher = new MqttClientManager(mqttProperties, clientId, MqttClientManager.noOpCallback());

    int deviceCount = Math.max(1, simulatorProperties.deviceCount());
    this.devices = new ArrayList<>(deviceCount);
    for (int i = 1; i <= deviceCount; i++) {
      String deviceId = String.format("E%04d", i);
      devices.add(new ElevatorSim(deviceId, simulatorProperties, new Random(deviceId.hashCode())));
    }

    this.executor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "elevator-simulator"));
    this.intervalMs = Math.max(200, simulatorProperties.publishIntervalMs());
    this.running = false;
  }

  @Override
  public void start() {
    if (running) {
      return;
    }
    publisher.start();
    executor.scheduleAtFixedRate(this::tickAll, 0, intervalMs, TimeUnit.MILLISECONDS);
    running = true;
    log.info("Simulator started: deviceCount={}, intervalMs={}", devices.size(), intervalMs);
  }

  @Override
  public void stop() {
    running = false;
    executor.shutdownNow();
    publisher.stop();
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
    // start after mqtt ingest
    return 10;
  }

  private void tickAll() {
    long nowMs = System.currentTimeMillis();
    for (ElevatorSim sim : devices) {
      sim.tick(nowMs)
          .ifPresent(
              telemetry -> {
                try {
                  publisher.publish(
                      "elevator/" + sim.deviceId() + "/data",
                      objectMapper.writeValueAsString(telemetry),
                      1,
                      false);
                } catch (Exception e) {
                  log.debug("Simulator publish telemetry failed: deviceId={}", sim.deviceId(), e);
                }
              });

      sim.maybeTrappedAlert(nowMs, simulatorProperties.trappedChance())
          .ifPresent(
              alert -> {
                try {
                  publisher.publish(
                      "elevator/" + sim.deviceId() + "/alert",
                      objectMapper.writeValueAsString(alert),
                      1,
                      false);
                } catch (Exception e) {
                  log.debug("Simulator publish alert failed: deviceId={}", sim.deviceId(), e);
                }
              });
    }
  }

  private static final class ElevatorSim {
    private static final String[] LOCATIONS = {
      "南山区科技园A栋 6楼", "福田区中心广场B座 2楼", "武昌区中北路C座 18楼", "洪山区光谷D座 10楼"
    };

    private final String deviceId;
    private final SimulatorProperties properties;
    private final Random random;

    private int floor;
    private int direction; // 1 up, -1 down
    private int dwellTicks;
    private int ticksToNextFloor;
    private int stuckTicks;
    private long offlineUntilMs;

    private double temperature;
    private double vibration;

    ElevatorSim(String deviceId, SimulatorProperties properties, Random random) {
      this.deviceId = deviceId;
      this.properties = properties;
      this.random = random;

      this.floor = randomBetween(properties.minFloor(), properties.maxFloor());
      this.direction = random.nextBoolean() ? 1 : -1;
      this.dwellTicks = randomBetween(1, 4);
      this.ticksToNextFloor = randomBetween(2, 4);
      this.temperature = 25 + random.nextDouble() * 3;
      this.vibration = 0.03 + random.nextDouble() * 0.05;
    }

    String deviceId() {
      return deviceId;
    }

    java.util.Optional<DeviceTelemetry> tick(long nowMs) {
      if (offlineUntilMs > nowMs) {
        return java.util.Optional.empty();
      }

      maybeGoOffline(nowMs);
      if (offlineUntilMs > nowMs) {
        return java.util.Optional.empty();
      }

      boolean moving = dwellTicks <= 0;
      String doorStatus;
      double speed;

      if (!moving) {
        doorStatus = "open";
        speed = 0.0;
        dwellTicks--;
        if (dwellTicks == 0) {
          // start moving next tick
          ticksToNextFloor = randomBetween(2, 4);
        }
      } else {
        doorStatus = "closed";
        speed = 1.0 + random.nextDouble() * 1.5;

        if (stuckTicks > 0) {
          doorStatus = "stuck";
          speed = 0.0;
          stuckTicks--;
        }

        ticksToNextFloor--;
        if (ticksToNextFloor <= 0) {
          floor += direction;
          if (floor <= properties.minFloor()) {
            floor = properties.minFloor();
            direction = 1;
            dwellTicks = randomBetween(2, 5);
          } else if (floor >= properties.maxFloor()) {
            floor = properties.maxFloor();
            direction = -1;
            dwellTicks = randomBetween(2, 5);
          }
          ticksToNextFloor = randomBetween(2, 4);
        }

        // occasionally produce door stuck event (orange)
        if (stuckTicks == 0 && random.nextDouble() < 0.002) {
          stuckTicks = randomBetween(3, 8);
        }
      }

      double load = Math.max(0, 50 + random.nextGaussian() * 80);
      double power = 6.5 + load / 200.0 + (moving ? 1.0 : 0.2) + random.nextDouble() * 0.5;

      // temperature rises slightly with movement/load
      temperature += (moving ? 0.05 : -0.03) + load / 5000.0 + random.nextGaussian() * 0.02;
      temperature = clamp(temperature, 20, 75);

      // vibration spikes occasionally
      vibration += random.nextGaussian() * 0.01;
      if (random.nextDouble() < 0.003) {
        vibration += 0.8 + random.nextDouble() * 0.8;
      }
      vibration = clamp(vibration, 0.01, 2.5);

      long ts = nowMs;
      String dir = direction > 0 ? "up" : "down";
      if (!moving) {
        dir = "idle";
      }

      return java.util.Optional.of(
          new DeviceTelemetry(
              deviceId, ts, floor, dir, doorStatus, speed, load, temperature, vibration, power));
    }

    java.util.Optional<DeviceAlert> maybeTrappedAlert(long nowMs, double chance) {
      if (offlineUntilMs > nowMs) {
        return java.util.Optional.empty();
      }
      if (chance <= 0) {
        return java.util.Optional.empty();
      }
      if (random.nextDouble() >= chance) {
        return java.util.Optional.empty();
      }
      String alertId = "ALT-" + deviceId + "-" + nowMs;
      String location = LOCATIONS[random.nextInt(LOCATIONS.length)];
      int trappedCount = randomBetween(1, 8);
      return java.util.Optional.of(
          new DeviceAlert(
              deviceId,
              alertId,
              "red",
              "trapped",
              "困人告警（模拟）",
              nowMs,
              location,
              trappedCount));
    }

    private void maybeGoOffline(long nowMs) {
      if (properties.offlineChance() <= 0) {
        return;
      }
      if (random.nextDouble() < properties.offlineChance()) {
        int seconds =
            randomBetween(Math.max(1, properties.offlineSecondsMin()), Math.max(2, properties.offlineSecondsMax()));
        offlineUntilMs = nowMs + seconds * 1000L;
      }
    }

    private int randomBetween(int min, int max) {
      int lo = Math.min(min, max);
      int hi = Math.max(min, max);
      return lo + random.nextInt(hi - lo + 1);
    }

    private static double clamp(double v, double min, double max) {
      return Math.max(min, Math.min(max, v));
    }
  }
}
