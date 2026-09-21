package com.gk3.demo.serverless.telemetry.engine;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Lightweight smoke test that only verifies the Spring context assembles correctly
 * (wiring of all beans). We disable the SQS listener auto-start because without the
 * 'local'/LocalStack profile there's no access to real AWS here - the full end-to-end flow
 * (SQS -> DynamoDB) is covered by TelemetryStreamIntegrationTest with Testcontainers.
 */
@SpringBootTest(properties = "app.sqs.listener.auto-startup=false")
class ServerlessApplicationTests {

    @Test
    void contextLoads() {
    }

}
