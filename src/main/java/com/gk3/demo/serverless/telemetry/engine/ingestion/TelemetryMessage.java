package com.gk3.demo.serverless.telemetry.engine.ingestion;

import com.gk3.demo.serverless.telemetry.engine.domain.Telemetry;

/**
 * Payload of the SQS message sent by the IoT gateway (Mosquitto -&gt; SQS).
 * Besides the telemetry reading itself, it carries the identifiers needed to locate the
 * record in the Single-Table Design (customer partition + machine).
 */
public record TelemetryMessage(String customerId, String deviceId, Telemetry telemetry) {
}
