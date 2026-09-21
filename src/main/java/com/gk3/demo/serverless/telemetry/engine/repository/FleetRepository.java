package com.gk3.demo.serverless.telemetry.engine.repository;

import com.gk3.demo.serverless.telemetry.engine.domain.Customer;
import com.gk3.demo.serverless.telemetry.engine.domain.Device;
import com.gk3.demo.serverless.telemetry.engine.domain.Telemetry;
import com.gk3.demo.serverless.telemetry.engine.repository.mapper.FleetMapper;
import com.gk3.demo.serverless.telemetry.engine.repository.model.BaseEntity;
import com.gk3.demo.serverless.telemetry.engine.repository.model.CustomerEntity;
import com.gk3.demo.serverless.telemetry.engine.repository.model.DeviceEntity;
import com.gk3.demo.serverless.telemetry.engine.repository.model.TelemetryEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class FleetRepository {

    private final DynamoDbTable<BaseEntity> table;
    private final DynamoDbEnhancedClient enhancedClient;

    // TableSchema<BaseEntity> is injected from the configuration class (with polymorphism)
    public FleetRepository(
            DynamoDbEnhancedClient enhancedClient,
            TableSchema<BaseEntity> polymorphicSchema,
            @Value("${app.dynamodb.table-name:FleetManagement}") String tableName) {
        this.enhancedClient = enhancedClient;
        this.table = enhancedClient.table(tableName, polymorphicSchema);
    }

    // =========================================================================
    // WRITE OPERATIONS (save the corresponding subclasses)
    // =========================================================================

    public void saveCustomer(Customer customer) {
        CustomerEntity entity = FleetMapper.toEntity(customer);
        table.putItem(entity);
    }

    public void saveDevice(Device device) {
        DeviceEntity entity = FleetMapper.toEntity(device);
        table.putItem(entity);
    }

    public void saveTelemetry(Telemetry telemetry, String deviceId) {
        TelemetryEntity entity = FleetMapper.toEntity(telemetry, deviceId);
        table.putItem(entity);
    }

    public void saveTelemetryBatch(List<Telemetry> telemetryList, String deviceId) {
        if (telemetryList == null || telemetryList.isEmpty()) {
            return;
        }

        int awsBatchLimit = 25;
        List<TelemetryEntity> entities = telemetryList.stream()
                .map(t -> FleetMapper.toEntity(t, deviceId))
                .toList();

        // Iterate over the list, cutting it into chunks of at most 25 items
        for (int i = 0; i < entities.size(); i += awsBatchLimit) {
            List<TelemetryEntity> subList = entities.subList(i, Math.min(i + awsBatchLimit, entities.size()));

            // Build the write batch for our polymorphic table
            WriteBatch.Builder<BaseEntity> writeBatchBuilder = WriteBatch.builder(BaseEntity.class)
                    .mappedTableResource(table);

            // Register each entity as a PUT operation (write/overwrite)
            subList.forEach(writeBatchBuilder::addPutItem);

            BatchWriteItemEnhancedRequest batchRequest = BatchWriteItemEnhancedRequest.builder()
                    .addWriteBatch(writeBatchBuilder.build())
                    .build();

            // One network request to AWS DynamoDB containing up to 25 telemetry rows
            enhancedClient.batchWriteItem(batchRequest);
        }
    }

    // =========================================================================
    // READ OPERATIONS (return plain domain objects)
    // =========================================================================

    /**
     * Fetches a Customer by its ID.
     */
    public Optional<Customer> findCustomerById(String customerId) {
        BaseEntity result = table.getItem(r -> r.key(k -> k.partitionValue("CUSTOMER#" + customerId).sortValue("METADATA")));
        if (result instanceof CustomerEntity customerEntity) {
            return Optional.of(FleetMapper.toDomain(customerEntity));
        }
        return Optional.empty();
    }

    /**
     * Fetches a specific machine belonging to a given customer (knowing both parts of the primary key).
     */
    public Optional<Device> findDevice(String customerId, String deviceId) {
        BaseEntity result = table.getItem(r -> r.key(
                k -> k.partitionValue("CUSTOMER#" + customerId).sortValue("DEVICE#" + deviceId)));
        if (result instanceof DeviceEntity deviceEntity) {
            return Optional.of(FleetMapper.toDomain(deviceEntity));
        }
        return Optional.empty();
    }

    /**
     * The power of Single-Table Design: fetches the customer and all of its machines in a
     * SINGLE network round-trip to the database, without a JOIN operation.
     */
    public Optional<CustomerAggregateResponse> findCustomerWithDevices(String customerId) {
        String pk = "CUSTOMER#" + customerId;

        // The query fetches everything sharing this Partition Key
        SdkIterable<BaseEntity> records = table.query(QueryConditional.keyEqualTo(k -> k.partitionValue(pk))).items();

        Customer customer = null;
        List<Device> devices = new ArrayList<>();

        for (BaseEntity record : records) {
            if (record instanceof CustomerEntity customerEntity) {
                customer = FleetMapper.toDomain(customerEntity);
            } else if (record instanceof DeviceEntity deviceEntity) {
                devices.add(FleetMapper.toDomain(deviceEntity));
            }
        }

        if (customer == null) return Optional.empty();
        return Optional.of(new CustomerAggregateResponse(customer, devices));
    }

    /**
     * Fetches the telemetry history for a specific machine within a time range.
     * Automatically handles the 1 MB response limit thanks to SdkIterable (lazy loading).
     */
    public List<Telemetry> findTelemetryHistory(String deviceId, Instant from, Instant to) {
        String pk = "DEVICE#" + deviceId;
        String startSk = "TELEMETRY#" + from.toString();
        String endSk = "TELEMETRY#" + to.toString();

        QueryConditional rangeCondition = QueryConditional.sortBetween(
                start -> start.partitionValue(pk).sortValue(startSk),
                end -> end.partitionValue(pk).sortValue(endSk)
        );

        return table.query(QueryEnhancedRequest.builder().queryConditional(rangeCondition).build())
                .items()
                .stream()
                .filter(record -> record instanceof TelemetryEntity)
                .map(record -> FleetMapper.toDomain((TelemetryEntity) record))
                .toList();
    }

    // Helper DTO record for the aggregation
    public record CustomerAggregateResponse(Customer customer, List<Device> devices) {
    }
}
