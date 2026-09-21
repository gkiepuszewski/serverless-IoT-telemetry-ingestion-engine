package com.gk3.demo.serverless.telemetry.engine.ingestion;

import com.gk3.demo.serverless.telemetry.engine.application.TelemetryProcessingService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Consumer for the "telemetry-ingestion-queue" SQS queue. Each message represents a single
 * telemetry reading from a machine; Virtual Threads (Java 21) allow handling many concurrent
 * messages without blocking OS threads.
 */
@Service
public class SqsTelemetryListener {

    private static final Logger log = LoggerFactory.getLogger(SqsTelemetryListener.class);

    private final TelemetryProcessingService telemetryProcessingService;

    public SqsTelemetryListener(TelemetryProcessingService telemetryProcessingService) {
        this.telemetryProcessingService = telemetryProcessingService;
    }

    @SqsListener("telemetry-ingestion-queue")
    public void onTelemetryMessage(TelemetryMessage message) {
        log.debug("Received telemetry reading for device {} (customer {})", message.deviceId(), message.customerId());
        telemetryProcessingService.processIncomingBatchFromMachine(
                message.customerId(), message.deviceId(), List.of(message.telemetry()));
    }
}
