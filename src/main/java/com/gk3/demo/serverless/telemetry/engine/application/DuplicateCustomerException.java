package com.gk3.demo.serverless.telemetry.engine.application;

public class DuplicateCustomerException extends RuntimeException {
    public DuplicateCustomerException(String customerId) {
        super("Customer already registered: " + customerId);
    }
}
