package com.gk3.demo.serverless.telemetry.engine.application;

public class DuplicateDeviceException extends RuntimeException {
    public DuplicateDeviceException(String customerId, String deviceId) {
        super("Device already registered: customerId=%s, deviceId=%s".formatted(customerId, deviceId));
    }
}
