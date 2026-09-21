package com.gk3.demo.serverless.telemetry.engine.application;

import com.gk3.demo.serverless.telemetry.engine.api.dto.CustomerRegistrationRequest;
import com.gk3.demo.serverless.telemetry.engine.api.dto.CustomerResponse;
import com.gk3.demo.serverless.telemetry.engine.api.dto.DeviceRegistrationRequest;
import com.gk3.demo.serverless.telemetry.engine.api.dto.DeviceResponse;
import com.gk3.demo.serverless.telemetry.engine.domain.Customer;
import com.gk3.demo.serverless.telemetry.engine.domain.Device;
import com.gk3.demo.serverless.telemetry.engine.repository.FleetRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Application layer for the onboarding process: before any telemetry data is accepted (see
 * {@link TelemetryProcessingService} and
 * {@link com.gk3.demo.serverless.telemetry.engine.ingestion.mqtt.MqttTelemetryBridge}), a
 * company (Customer) and its machine (Device) must be explicitly registered through this REST API.
 */
@Service
public class FleetRegistrationService {

    private final FleetRepository fleetRepository;

    public FleetRegistrationService(FleetRepository fleetRepository) {
        this.fleetRepository = fleetRepository;
    }

    public CustomerResponse registerCustomer(CustomerRegistrationRequest request) {
        String customerId = (request.id() == null || request.id().isBlank())
                ? UUID.randomUUID().toString()
                : request.id();

        if (fleetRepository.findCustomerById(customerId).isPresent()) {
            throw new DuplicateCustomerException(customerId);
        }

        Customer customer = new Customer(customerId, request.name(), request.email(), request.premiumSupportActive());
        fleetRepository.saveCustomer(customer);
        return toResponse(customer);
    }

    public CustomerResponse getCustomer(String customerId) {
        Customer customer = fleetRepository.findCustomerById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
        return toResponse(customer);
    }

    public DeviceResponse registerDevice(String customerId, DeviceRegistrationRequest request) {
        // No point registering a machine for a customer that doesn't exist
        fleetRepository.findCustomerById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        if (fleetRepository.findDevice(customerId, request.id()).isPresent()) {
            throw new DuplicateDeviceException(customerId, request.id());
        }

        Device device = new Device(request.id(), customerId, request.model(), "OPERATIONAL");
        fleetRepository.saveDevice(device);
        return toResponse(device);
    }

    public DeviceResponse getDevice(String customerId, String deviceId) {
        Device device = fleetRepository.findDevice(customerId, deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(customerId, deviceId));
        return toResponse(device);
    }

    private static CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getName(), customer.getEmail(), customer.isPremiumSupportActive());
    }

    private static DeviceResponse toResponse(Device device) {
        return new DeviceResponse(device.getId(), device.getCustomerId(), device.getModel(), device.getStatus());
    }
}
