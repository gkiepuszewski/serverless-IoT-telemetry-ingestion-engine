package com.gk3.demo.serverless.telemetry.engine.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to register a new device under an existing customer. {@code id} should match the
 * value used in the MQTT topic / the {@code deviceId} field in TelemetryMessage
 * (e.g. a VIN number), because telemetry from unregistered (customerId, deviceId) pairs
 * is rejected - see {@link com.gk3.demo.serverless.telemetry.engine.application.UnregisteredDeviceException}.
 */
public record DeviceRegistrationRequest(
        @NotBlank(message = "id is required") String id,
        @NotBlank(message = "model is required") String model
) {
}
