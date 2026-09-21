package com.gk3.demo.serverless.telemetry.engine.domain;

import java.time.Instant;

public record Telemetry(
        Instant timestamp,
        Double fuelLevel,
        Double latitude,
        Double longitude,
        Double hydraulicPressurePsi) {

    public boolean hasCriticalHydraulicPressure() {
        return this.hydraulicPressurePsi != null && this.hydraulicPressurePsi > 3000.0;
    }
}