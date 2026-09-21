package com.gk3.demo.serverless.telemetry.engine.config;

import com.gk3.demo.serverless.telemetry.engine.repository.model.BaseEntity;
import com.gk3.demo.serverless.telemetry.engine.repository.schema.PolymorphicFleetTableSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Configuration
public class DynamoDbSchemaConfig {

    /**
     * Single polymorphic {@link TableSchema} shared by {@code FleetRepository} to read/write
     * Customer, Device and Telemetry entities living in the same "FleetManagement" table.
     */
    @Bean
    public TableSchema<BaseEntity> polymorphicSchema() {
        return new PolymorphicFleetTableSchema();
    }
}
