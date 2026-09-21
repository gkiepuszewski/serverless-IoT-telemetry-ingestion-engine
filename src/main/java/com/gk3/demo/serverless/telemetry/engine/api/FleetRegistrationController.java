package com.gk3.demo.serverless.telemetry.engine.api;

import com.gk3.demo.serverless.telemetry.engine.api.dto.CustomerRegistrationRequest;
import com.gk3.demo.serverless.telemetry.engine.api.dto.CustomerResponse;
import com.gk3.demo.serverless.telemetry.engine.api.dto.DeviceRegistrationRequest;
import com.gk3.demo.serverless.telemetry.engine.api.dto.DeviceResponse;
import com.gk3.demo.serverless.telemetry.engine.application.FleetRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dev/demo-facing REST API for registering the fleet (Customer + Device). Replaces manual
 * {@code aws dynamodb put-item --cli-input-json} calls during manual testing (see README) and,
 * more importantly, is the only legitimate way to "let in" a new device - telemetry from
 * unregistered (customerId, deviceId) pairs is rejected (see
 * {@code TelemetryProcessingService} and {@code MqttTelemetryBridge}).
 * <p>
 * This API does not exist in Lambda mode ({@code TelemetryIngestionLambdaHandler} bootstraps the
 * Spring context with {@code WebApplicationType.NONE}) - in a real serverless deployment,
 * registration would go through a separate microservice/API Gateway endpoint, outside the scope
 * of this PoC.
 */
@RestController
@RequestMapping("/api/customers")
public class FleetRegistrationController {

    private final FleetRegistrationService fleetRegistrationService;

    public FleetRegistrationController(FleetRegistrationService fleetRegistrationService) {
        this.fleetRegistrationService = fleetRegistrationService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> registerCustomer(@Valid @RequestBody CustomerRegistrationRequest request) {
        CustomerResponse response = fleetRegistrationService.registerCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{customerId}")
    public CustomerResponse getCustomer(@PathVariable String customerId) {
        return fleetRegistrationService.getCustomer(customerId);
    }

    @PostMapping("/{customerId}/devices")
    public ResponseEntity<DeviceResponse> registerDevice(
            @PathVariable String customerId, @Valid @RequestBody DeviceRegistrationRequest request) {
        DeviceResponse response = fleetRegistrationService.registerDevice(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{customerId}/devices/{deviceId}")
    public DeviceResponse getDevice(@PathVariable String customerId, @PathVariable String deviceId) {
        return fleetRegistrationService.getDevice(customerId, deviceId);
    }
}
