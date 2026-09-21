package com.gk3.demo.serverless.telemetry.engine.repository.mapper;

import com.gk3.demo.serverless.telemetry.engine.domain.Customer;
import com.gk3.demo.serverless.telemetry.engine.domain.Device;
import com.gk3.demo.serverless.telemetry.engine.domain.Telemetry;
import com.gk3.demo.serverless.telemetry.engine.repository.model.CustomerEntity;
import com.gk3.demo.serverless.telemetry.engine.repository.model.DeviceEntity;
import com.gk3.demo.serverless.telemetry.engine.repository.model.TelemetryEntity;

import java.time.Instant;

public class FleetMapper {

    public static final String CUSTOMER = "CUSTOMER#";
    public static final String METADATA = "METADATA";
    public static final String DEVICE = "DEVICE#";
    public static final String TELEMETRY = "TELEMETRY#";

    public static CustomerEntity toEntity(Customer customer) {
        CustomerEntity entity = new CustomerEntity();
        // Single-Table Design primary keys
        entity.setPk(CUSTOMER + customer.getId());
        entity.setSk(METADATA);
        // Business attributes
        entity.setName(customer.getName());
        entity.setEmail(customer.getEmail());
        entity.setPremiumSupportActive(customer.isPremiumSupportActive());
        return entity;
    }

    public static DeviceEntity toEntity(Device device) {
        DeviceEntity entity = new DeviceEntity();
        // The machine lives in the customer's partition - shares its PK
        entity.setPk(CUSTOMER + device.getCustomerId());
        entity.setSk(DEVICE + device.getId());
        // Business attributes
        entity.setModel(device.getModel());
        entity.setStatus(device.getStatus());
        return entity;
    }

    public static TelemetryEntity toEntity(Telemetry telemetry, String deviceId) {
        TelemetryEntity entity = new TelemetryEntity();
        // Telemetry lives in a separate machine partition (VIN/ID) to avoid hot spots
        entity.setPk(DEVICE + deviceId);
        // ISO-8601 string guarantees perfect alphabetical/chronological sort order on AWS disk
        entity.setSk(TELEMETRY + telemetry.timestamp().toString());
        // Business attributes
        entity.setFuelLevel(telemetry.fuelLevel());
        entity.setLatitude(telemetry.latitude());
        entity.setLongitude(telemetry.longitude());
        entity.setHydraulicPressurePsi(telemetry.hydraulicPressurePsi());
        return entity;
    }

    public static Customer toDomain(CustomerEntity entity) {
        // Extract the clean business ID by stripping the technical prefix
        String id = entity.getPk().replace(CUSTOMER, "");
        return new Customer(
                id,
                entity.getName(),
                entity.getEmail(),
                entity.getPremiumSupportActive() != null && entity.getPremiumSupportActive()
        );
    }

    public static Device toDomain(DeviceEntity entity) {
        String customerId = entity.getPk().replace(CUSTOMER, "");
        String id = entity.getSk().replace(DEVICE, "");
        return new Device(
                id,
                customerId,
                entity.getModel(),
                entity.getStatus()
        );
    }

    public static Telemetry toDomain(TelemetryEntity entity) {
        String timestampStr = entity.getSk().replace(TELEMETRY, "");
        return new Telemetry(
                Instant.parse(timestampStr),
                entity.getFuelLevel(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getHydraulicPressurePsi()
        );
    }
}
