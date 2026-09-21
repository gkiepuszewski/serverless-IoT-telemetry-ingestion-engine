package com.gk3.demo.serverless.telemetry.engine.api.dto;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<String> details
) {
}
