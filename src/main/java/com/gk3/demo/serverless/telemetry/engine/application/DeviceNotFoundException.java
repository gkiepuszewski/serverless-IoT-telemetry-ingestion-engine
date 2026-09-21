package com.gk3.demo.serverless.telemetry.engine.application;

public class DeviceNotFoundException extends RuntimeException {
    public DeviceNotFoundException(String customerId, String deviceId) {
        super("Device not found: customerId=%s, deviceId=%s".formatted(customerId, deviceId));
    }
}
