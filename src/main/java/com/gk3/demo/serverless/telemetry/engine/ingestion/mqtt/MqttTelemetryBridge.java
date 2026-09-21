package com.gk3.demo.serverless.telemetry.engine.ingestion.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk3.demo.serverless.telemetry.engine.domain.Telemetry;
import com.gk3.demo.serverless.telemetry.engine.ingestion.TelemetryMessage;
import com.gk3.demo.serverless.telemetry.engine.repository.FleetRepository;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mosquitto (MQTT) -&gt; SQS bridge acting as a local, free substitute for the
 * AWS IoT Core Rules Engine (a paid service in real AWS). Real IoT devices would publish
 * telemetry directly to AWS IoT Core, whose routing rule would drop the message into the same
 * SQS queue - here the same effect is achieved by this component + the Mosquitto broker running
 * in docker-compose ("local"/demo profile).
 * <p>
 * Expected topic format: {@code machines/{customerId}/{deviceId}/telemetry}, with the message
 * body being JSON matching the {@link Telemetry} record.
 * <p>
 * Active only when {@code mqtt.enabled=true} (see application-local.yaml) - in "serverless"/Lambda
 * (production) mode this bridge doesn't exist, because AWS IoT Core posts directly to SQS.
 */
@Component
@ConditionalOnProperty(prefix = "mqtt", name = "enabled", havingValue = "true")
public class MqttTelemetryBridge implements MqttCallback {

    private static final Logger log = LoggerFactory.getLogger(MqttTelemetryBridge.class);
    private static final String INGESTION_QUEUE_NAME = "telemetry-ingestion-queue";

    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;
    private final FleetRepository fleetRepository;
    private final String brokerUrl;
    private final String clientId;
    private final String telemetryTopic;

    private MqttClient mqttClient;

    public MqttTelemetryBridge(
            SqsTemplate sqsTemplate,
            ObjectMapper objectMapper,
            FleetRepository fleetRepository,
            @Value("${mqtt.broker-url}") String brokerUrl,
            @Value("${mqtt.client-id}") String clientId,
            @Value("${mqtt.telemetry-topic}") String telemetryTopic) {
        this.sqsTemplate = sqsTemplate;
        this.objectMapper = objectMapper;
        this.fleetRepository = fleetRepository;
        this.brokerUrl = brokerUrl;
        this.clientId = clientId;
        this.telemetryTopic = telemetryTopic;
    }

    @PostConstruct
    public void connectAndSubscribe() throws Exception {
        mqttClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
        mqttClient.setCallback(this);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);

        mqttClient.connect(options);
        mqttClient.subscribe(telemetryTopic);
        log.info("MQTT bridge connected to {} and subscribed to '{}'", brokerUrl, telemetryTopic);
    }

    @PreDestroy
    public void disconnect() throws Exception {
        if (mqttClient != null && mqttClient.isConnected()) {
            mqttClient.disconnect();
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        try {
            String[] segments = topic.split("/");
            // Expected format: machines/{customerId}/{deviceId}/telemetry
            if (segments.length != 4) {
                log.warn("Ignoring MQTT message on unexpected topic '{}'", topic);
                return;
            }
            String customerId = segments[1];
            String deviceId = segments[2];

            // Security gate: reject telemetry from unregistered machines before it reaches SQS.
            // The real production enforcement also lives in TelemetryProcessingService (this path
            // is just a "fail fast" optimization for the local/dev mode).
            if (fleetRepository.findDevice(customerId, deviceId).isEmpty()) {
                log.warn("Rejecting telemetry from unregistered device {} (customer {}) - "
                        + "register it first via POST /api/customers/{}/devices", deviceId, customerId, customerId);
                return;
            }

            Telemetry telemetry = objectMapper.readValue(message.getPayload(), Telemetry.class);
            TelemetryMessage sqsPayload = new TelemetryMessage(customerId, deviceId, telemetry);

            sqsTemplate.send(to -> to.queue(INGESTION_QUEUE_NAME).payload(sqsPayload));
            log.debug("Forwarded MQTT reading from device {} (customer {}) to SQS", deviceId, customerId);
        } catch (Exception e) {
            log.error("Failed to forward MQTT message on topic '{}' to SQS: {}", topic, e.getMessage(), e);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("MQTT connection lost: {}", cause.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Unused - the bridge only subscribes and forwards, it never publishes on its own topics.
    }
}
