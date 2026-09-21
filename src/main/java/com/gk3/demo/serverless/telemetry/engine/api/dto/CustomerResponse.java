package com.gk3.demo.serverless.telemetry.engine.api.dto;

public record CustomerResponse(
        String id,
        String name,
        String email,
        boolean premiumSupportActive
) {
}
