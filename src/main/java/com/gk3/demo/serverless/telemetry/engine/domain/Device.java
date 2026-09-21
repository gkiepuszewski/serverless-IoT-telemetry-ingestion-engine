package com.gk3.demo.serverless.telemetry.engine.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Device {
    private final String id;
    private final String customerId; // Link to the Customer entity (DDD: reference by ID)
    private final String model;
    private String status;           // Mutable state (e.g. OPERATIONAL, CRITICAL_ERROR)
    private final List<Telemetry> telemetryHistory; // Internal history

    public Device(String id, String customerId, String model, String status) {
        this.id = id;
        this.customerId = customerId;
        this.model = model;
        this.status = status;
        this.telemetryHistory = new ArrayList<>();
    }

    /**
     * DDD business method (behavior instead of a setter).
     * The machine processes a new sensor reading and decides its own state.
     */
    public void recordTelemetry(Telemetry telemetry) {
        this.telemetryHistory.add(telemetry);

        // Delegates to the business method on the 'Telemetry' Value Object
        if (telemetry.hasCriticalHydraulicPressure()) {
            this.status = "CRITICAL_ERROR";
        }
    }

    // Getters (no setters - state is protected inside the aggregate)
    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getModel() { return model; }
    public String getStatus() { return status; }

    // Returned as an immutable list so nobody outside can modify the history behind the aggregate's back
    public List<Telemetry> getTelemetryHistory() {
        return Collections.unmodifiableList(telemetryHistory);
    }
}
