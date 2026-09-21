package com.gk3.demo.serverless.telemetry.engine.application;

/**
 * Thrown when telemetry arrives for a (customerId, deviceId) pair that was never previously
 * registered via {@code POST /api/customers/{customerId}/devices}. We treat this as a
 * security/data-integrity error, not a transient infrastructure failure: we have no guarantee
 * that the data comes from a trusted, known source.
 * <p>
 * In "consumer" mode ({@code SqsTelemetryListener}), throwing this exception causes
 * spring-cloud-aws to NOT acknowledge the SQS message - after {@code maxReceiveCount} attempts it
 * lands in the DLQ (see {@code template.yaml}), where it can be reviewed for security purposes
 * instead of silently discarding data from an untrusted source.
 */
public class UnregisteredDeviceException extends RuntimeException {

    public UnregisteredDeviceException(String customerId, String deviceId) {
        super("Rejecting telemetry from unregistered device: customerId=%s, deviceId=%s. "
                .formatted(customerId, deviceId)
                + "Register the customer and device first via POST /api/customers and "
                + "POST /api/customers/{customerId}/devices.");
    }
}
