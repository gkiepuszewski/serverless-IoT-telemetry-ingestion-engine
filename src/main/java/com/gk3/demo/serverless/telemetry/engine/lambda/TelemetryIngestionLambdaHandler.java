package com.gk3.demo.serverless.telemetry.engine.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gk3.demo.serverless.telemetry.engine.ServerlessApplication;
import com.gk3.demo.serverless.telemetry.engine.application.TelemetryProcessingService;
import com.gk3.demo.serverless.telemetry.engine.ingestion.TelemetryMessage;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * The real "serverless" entry point of this PoC: an AWS Lambda subscribed to the SQS queue
 * via an Event Source Mapping. AWS itself polls the queue and delivers batches of messages to
 * {@link #handleRequest} - unlike the "consumer" mode ({@code SqsTelemetryListener}), here the
 * application doesn't maintain its own long-running process or polling loop.
 * <p>
 * The Lambda execution container is reused across "warm" invocations, so we build the Spring
 * context only once (static field) - subsequent invocations on the same instance no longer pay
 * the cold-start cost.
 * <p>
 * The handler returns an {@link SQSBatchResponse} with the list of failed records (partial batch
 * item failures), so AWS only retries delivery of those specific messages, not the whole batch.
 */
public class TelemetryIngestionLambdaHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private static volatile ConfigurableApplicationContext applicationContext;

    private final TelemetryProcessingService telemetryProcessingService;

    public TelemetryIngestionLambdaHandler() {
        this.telemetryProcessingService = applicationContext().getBean(TelemetryProcessingService.class);
    }

    private static ConfigurableApplicationContext applicationContext() {
        if (applicationContext == null) {
            synchronized (TelemetryIngestionLambdaHandler.class) {
                if (applicationContext == null) {
                    applicationContext = new SpringApplicationBuilder(ServerlessApplication.class)
                            .web(WebApplicationType.NONE)
                            .properties(
                                    // Lambda itself acts as the SQS poller (Event Source Mapping) -
                                    // disable the internal listener so we don't poll the queue twice.
                                    "app.sqs.listener.auto-startup=false",
                                    // Doesn't make sense (and there's no docker-compose.yaml) in a Lambda environment.
                                    "spring.docker.compose.enabled=false")
                            .run();
                }
            }
        }
        return applicationContext;
    }

    @Override
    public SQSBatchResponse handleRequest(SQSEvent event, Context context) {
        List<SQSBatchResponse.BatchItemFailure> failures = new ArrayList<>();

        for (SQSEvent.SQSMessage record : event.getRecords()) {
            try {
                TelemetryMessage message = OBJECT_MAPPER.readValue(record.getBody(), TelemetryMessage.class);
                telemetryProcessingService.processIncomingBatchFromMachine(
                        message.customerId(), message.deviceId(), List.of(message.telemetry()));
            } catch (Exception e) {
                context.getLogger().log("Failed to process SQS message " + record.getMessageId() + ": " + e.getMessage());
                failures.add(SQSBatchResponse.BatchItemFailure.builder()
                        .withItemIdentifier(record.getMessageId())
                        .build());
            }
        }

        return SQSBatchResponse.builder().withBatchItemFailures(failures).build();
    }
}
