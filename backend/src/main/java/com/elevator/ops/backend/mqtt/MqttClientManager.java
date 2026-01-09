package com.elevator.ops.backend.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttClientManager {
  private static final Logger log = LoggerFactory.getLogger(MqttClientManager.class);

  private final MqttProperties properties;
  private final String clientId;
  private final MqttCallbackExtended callback;
  private MqttClient client;

  public MqttClientManager(MqttProperties properties, String clientId, MqttCallbackExtended callback) {
    this.properties = properties;
    this.clientId = clientId;
    this.callback = callback;
  }

  public synchronized void start() {
    if (client != null && client.isConnected()) {
      return;
    }
    try {
      String resolvedClientId =
          (clientId == null || clientId.isBlank()) ? "elevator-" + UUID.randomUUID() : clientId;
      client = new MqttClient(properties.brokerUrl(), resolvedClientId, new MemoryPersistence());
      client.setCallback(callback);

      MqttConnectOptions options = new MqttConnectOptions();
      options.setAutomaticReconnect(true);
      options.setCleanSession(true);
      if (properties.username() != null && !properties.username().isBlank()) {
        options.setUserName(properties.username());
      }
      if (properties.password() != null && !properties.password().isBlank()) {
        options.setPassword(properties.password().toCharArray());
      }

      client.connect(options);
      log.info("MQTT connected: brokerUrl={}, clientId={}", properties.brokerUrl(), resolvedClientId);
    } catch (MqttException e) {
      throw new IllegalStateException("Failed to start MQTT client", e);
    }
  }

  public synchronized void stop() {
    if (client == null) {
      return;
    }
    try {
      client.disconnectForcibly(2_000);
      client.close();
    } catch (MqttException e) {
      log.warn("Failed to stop MQTT client cleanly", e);
    } finally {
      client = null;
    }
  }

  public synchronized void subscribe(String topicFilter, int qos) {
    try {
      if (client == null || !client.isConnected()) {
        return;
      }
      client.subscribe(topicFilter, qos);
      log.info("MQTT subscribed: topicFilter={}, qos={}", topicFilter, qos);
    } catch (MqttException e) {
      log.warn("Failed to subscribe: {}", topicFilter, e);
    }
  }

  public synchronized void publish(String topic, String payload, int qos, boolean retained) {
    try {
      if (client == null || !client.isConnected()) {
        return;
      }
      MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
      message.setQos(qos);
      message.setRetained(retained);
      client.publish(topic, message);
    } catch (MqttException e) {
      log.warn("Failed to publish: {}", topic, e);
    }
  }

  public static MqttCallbackExtended noOpCallback() {
    return new MqttCallbackExtended() {
      @Override
      public void connectComplete(boolean reconnect, String serverURI) {}

      @Override
      public void connectionLost(Throwable cause) {}

      @Override
      public void messageArrived(String topic, MqttMessage message) {}

      @Override
      public void deliveryComplete(IMqttDeliveryToken token) {}
    };
  }
}

