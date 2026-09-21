package com.gk3.demo.serverless.telemetry.engine.application;

import com.gk3.demo.serverless.telemetry.engine.domain.Device;
import com.gk3.demo.serverless.telemetry.engine.domain.Telemetry;
import com.gk3.demo.serverless.telemetry.engine.repository.FleetRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TelemetryProcessingService {

    private final FleetRepository fleetRepository;

    public TelemetryProcessingService(FleetRepository fleetRepository) {
        this.fleetRepository = fleetRepository;
    }

    public void processIncomingBatchFromMachine(String customerId, String deviceId, List<Telemetry> incomingData) {
        if (incomingData == null || incomingData.isEmpty()) {
            return;
        }

        // Security gate: reject telemetry from machines that never went through the
        // registration process (POST /api/customers/{customerId}/devices). Without this, any
        // device that knows the topic/queue could inject arbitrary data into the system.
        Device device = fleetRepository.findDevice(customerId, deviceId)
                .orElseThrow(() -> new UnregisteredDeviceException(customerId, deviceId));

        // 1. Bulk-save the historical readings to the database
        fleetRepository.saveTelemetryBatch(incomingData, deviceId);

        // 2. Apply the DDD business rules (Device.recordTelemetry) on the already-loaded aggregate
        incomingData.forEach(device::recordTelemetry);
        // Save it back only if the business rule actually changed the machine's state
        fleetRepository.saveDevice(device);
    }
}

