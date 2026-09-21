package com.gk3.demo.serverless.telemetry.engine.api.dto;

public record DeviceResponse(
        String id,
        String customerId,
        String model,
        String status
) {
}
