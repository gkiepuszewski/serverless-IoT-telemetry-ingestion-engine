package com.gk3.demo.serverless.telemetry.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gk3.demo.serverless.telemetry.engine.domain.Customer;
import com.gk3.demo.serverless.telemetry.engine.domain.Device;
import com.gk3.demo.serverless.telemetry.engine.domain.Telemetry;
import com.gk3.demo.serverless.telemetry.engine.ingestion.TelemetryMessage;
import com.gk3.demo.serverless.telemetry.engine.repository.FleetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = "mqtt.enabled=false") // This test verifies the SQS -> DynamoDB path;
// the MQTT bridge (Mosquitto) is a separate architecture piece, no Mosquitto container here.
@Testcontainers
@ActiveProfiles("local") // Wczytuje application-local.yml
class TelemetryStreamIntegrationTest {

    private static final String TABLE_NAME = "FleetManagement";
    private static final String QUEUE_NAME = "telemetry-ingestion-queue";
    private static final String CUSTOMER_ID = "101";
    private static final String DEVICE_ID = "VIN-HEAVY-TRUCK-99";

    @Container
    private static final LocalStackContainer localStack =
            // Pinned to a specific version: "latest" in newer LocalStack builds requires a paid
            // LOCALSTACK_AUTH_TOKEN even for free services (SQS/DynamoDB), and the container
            // exits with code 55 on startup. 3.8 is the last known, fully free
            // Community version supporting both services we need.
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8"))
                    .withServices(LocalStackContainer.Service.SQS, LocalStackContainer.Service.DYNAMODB);

    @Autowired
    private FleetRepository fleetRepository;

    @Autowired
    private DynamoDbClient dynamoDbClient;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // Dynamicznie wstrzykujemy losowe porty kontenera LocalStack do Spring Boota
    @DynamicPropertySource
    static void overrideAwsProperties(DynamicPropertyRegistry registry) {
        String endpoint = localStack.getEndpointOverride(LocalStackContainer.Service.SQS).toString();
        registry.add("spring.cloud.aws.endpoint", () -> endpoint);
        registry.add("spring.cloud.aws.region.static", localStack::getRegion);
        registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
    }

    @BeforeEach
    void setUp() {
        ensureTableExists();
        // Register a customer and a machine so the processing service can update its status
        fleetRepository.saveCustomer(new Customer(CUSTOMER_ID, "Heavy Equipment Sp. z o.o.", "fleet@example.com", true));
        fleetRepository.saveDevice(new Device(DEVICE_ID, CUSTOMER_ID, "CAT-797F", "OPERATIONAL"));
    }

    private void ensureTableExists() {
        try {
            dynamoDbClient.describeTable(r -> r.tableName(TABLE_NAME));
        } catch (ResourceNotFoundException e) {
            dynamoDbClient.createTable(r -> r
                    .tableName(TABLE_NAME)
                    .attributeDefinitions(
                            AttributeDefinition.builder().attributeName("PK").attributeType(ScalarAttributeType.S).build(),
                            AttributeDefinition.builder().attributeName("SK").attributeType(ScalarAttributeType.S).build())
                    .keySchema(
                            KeySchemaElement.builder().attributeName("PK").keyType(KeyType.HASH).build(),
                            KeySchemaElement.builder().attributeName("SK").keyType(KeyType.RANGE).build())
                    .billingMode(BillingMode.PAY_PER_REQUEST));
            dynamoDbClient.waiter().waitUntilTableExists(r -> r.tableName(TABLE_NAME));
        }
    }

    @Test
    void shouldIngestSqsTelemetryStreamAndPersistToDynamoDbInBatches() throws Exception {
        // GIVEN: prepare the SQS client and make sure the queue exists
        SqsClient sqsClient = SqsClient.builder()
                .endpointOverride(localStack.getEndpointOverride(LocalStackContainer.Service.SQS))
                .region(Region.of(localStack.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())
                ))
                .build();

        sqsClient.createQueue(CreateQueueRequest.builder().queueName(QUEUE_NAME).build());
        String queueUrl = localStack.getEndpointOverride(LocalStackContainer.Service.SQS) + "/000000000000/" + QUEUE_NAME;

        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        int totalMessages = 40; // 4 full SQS batches (of 10) or DynamoDB chunks (max 25)

        List<SendMessageBatchRequestEntry> batchEntries = new ArrayList<>();

        // WHEN: generate the stream and bulk-send it to SQS (IoT simulation)
        for (int i = 0; i < totalMessages; i++) {
            Telemetry testTelemetry = new Telemetry(
                    now.minusSeconds(i),
                    85.0 - (i * 0.5),
                    19.4234, 50.1234,
                    2200.0 + (i * 25.0) // The last records will exceed the 3000 PSI threshold
            );
            TelemetryMessage message = new TelemetryMessage(CUSTOMER_ID, DEVICE_ID, testTelemetry);

            batchEntries.add(SendMessageBatchRequestEntry.builder()
                    .id(UUID.randomUUID().toString())
                    .messageBody(objectMapper.writeValueAsString(message))
                    .build());

            if (batchEntries.size() == 10 || i == totalMessages - 1) {
                sqsClient.sendMessageBatch(SendMessageBatchRequest.builder()
                        .queueUrl(queueUrl)
                        .entries(batchEntries)
                        .build());
                batchEntries.clear();
            }
        }

        // THEN: asynchronous assertion via Awaitility
        // Dajemy systemowi czas na przetworzenie kolejki i wykonanie zapisu do DynamoDB
        await().atMost(20, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    // Query our production repository for the machine's telemetry history
                    List<Telemetry> savedHistory = fleetRepository.findTelemetryHistory(
                            DEVICE_ID, now.minusSeconds(totalMessages), now
                    );

                    // Verify the database saved exactly as many records as we sent
                    assertThat(savedHistory).hasSize(totalMessages);

                    // DDD business assertion: critical hydraulic pressure (>3000 PSI) should
                    // flip the machine's status to CRITICAL_ERROR (Device.recordTelemetry)
                    assertThat(fleetRepository.findDevice(CUSTOMER_ID, DEVICE_ID))
                            .isPresent()
                            .get()
                            .extracting(Device::getStatus)
                            .isEqualTo("CRITICAL_ERROR");
                });
    }
}
