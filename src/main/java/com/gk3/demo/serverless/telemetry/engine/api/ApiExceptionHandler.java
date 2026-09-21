package com.gk3.demo.serverless.telemetry.engine.api;

import com.gk3.demo.serverless.telemetry.engine.api.dto.ApiError;
import com.gk3.demo.serverless.telemetry.engine.application.CustomerNotFoundException;
import com.gk3.demo.serverless.telemetry.engine.application.DeviceNotFoundException;
import com.gk3.demo.serverless.telemetry.engine.application.DuplicateCustomerException;
import com.gk3.demo.serverless.telemetry.engine.application.DuplicateDeviceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

/**
 * Translates application-layer exceptions into consistent HTTP responses for {@link FleetRegistrationController}.
 */
@RestControllerAdvice(assignableTypes = FleetRegistrationController.class)
public class ApiExceptionHandler {

    @ExceptionHandler({CustomerNotFoundException.class, DeviceNotFoundException.class})
    public ResponseEntity<ApiError> handleNotFound(RuntimeException e) {
        return build(HttpStatus.NOT_FOUND, e.getMessage(), List.of());
    }

    @ExceptionHandler({DuplicateCustomerException.class, DuplicateDeviceException.class})
    public ResponseEntity<ApiError> handleConflict(RuntimeException e) {
        return build(HttpStatus.CONFLICT, e.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
        List<String> details = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    private static ResponseEntity<ApiError> build(HttpStatus status, String message, List<String> details) {
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, details);
        return ResponseEntity.status(status).body(body);
    }
}
