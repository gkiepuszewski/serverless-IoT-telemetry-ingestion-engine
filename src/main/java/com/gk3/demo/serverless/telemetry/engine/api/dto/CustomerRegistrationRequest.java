package com.gk3.demo.serverless.telemetry.engine.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request to register a new customer (company). If {@code id} is blank, the service will
 * generate one automatically (UUID) - supply your own if you want a predictable ID for
 * demo/testing purposes (e.g. "101", matching the data in samples/*.json).
 */
public record CustomerRegistrationRequest(
        String id,
        @NotBlank(message = "name is required") String name,
        @NotBlank(message = "email is required") @Email(message = "email must be a valid address") String email,
        boolean premiumSupportActive
) {
}
