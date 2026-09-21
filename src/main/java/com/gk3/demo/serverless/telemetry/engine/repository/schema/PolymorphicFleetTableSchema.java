package com.gk3.demo.serverless.telemetry.engine.repository.schema;

import com.gk3.demo.serverless.telemetry.engine.repository.model.BaseEntity;
import com.gk3.demo.serverless.telemetry.engine.repository.model.CustomerEntity;
import com.gk3.demo.serverless.telemetry.engine.repository.model.DeviceEntity;
import com.gk3.demo.serverless.telemetry.engine.repository.model.TelemetryEntity;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Hand-rolled polymorphic {@link TableSchema} for the "FleetManagement" Single-Table Design.
 * <p>
 * AWS SDK v2 Enhanced Client does not natively support mapping a single DynamoDB table to several
 * Java subclasses out of the box, so this schema dispatches to the individual {@link BeanTableSchema}
 * of each concrete entity (Customer/Device/Telemetry) based on the "EntityType" discriminator attribute
 * that every {@link BaseEntity} subtype writes on save.
 */
public class PolymorphicFleetTableSchema implements TableSchema<BaseEntity> {

    private static final String ENTITY_TYPE_ATTRIBUTE = "EntityType";

    private final BeanTableSchema<CustomerEntity> customerSchema = TableSchema.fromBean(CustomerEntity.class);
    private final BeanTableSchema<DeviceEntity> deviceSchema = TableSchema.fromBean(DeviceEntity.class);
    private final BeanTableSchema<TelemetryEntity> telemetrySchema = TableSchema.fromBean(TelemetryEntity.class);

    @Override
    public BaseEntity mapToItem(Map<String, AttributeValue> attributeMap) {
        AttributeValue entityType = attributeMap.get(ENTITY_TYPE_ATTRIBUTE);
        if (entityType == null || entityType.s() == null) {
            throw new IllegalStateException(
                    "Cannot resolve polymorphic type: missing '" + ENTITY_TYPE_ATTRIBUTE + "' attribute.");
        }
        return switch (entityType.s()) {
            case "CUSTOMER" -> customerSchema.mapToItem(attributeMap);
            case "DEVICE" -> deviceSchema.mapToItem(attributeMap);
            case "TELEMETRY" -> telemetrySchema.mapToItem(attributeMap);
            default -> throw new IllegalStateException("Unknown entity type: " + entityType.s());
        };
    }

    @Override
    public Map<String, AttributeValue> itemToMap(BaseEntity item, boolean ignoreNulls) {
        if (item instanceof CustomerEntity customer) {
            return customerSchema.itemToMap(customer, ignoreNulls);
        }
        if (item instanceof DeviceEntity device) {
            return deviceSchema.itemToMap(device, ignoreNulls);
        }
        if (item instanceof TelemetryEntity telemetry) {
            return telemetrySchema.itemToMap(telemetry, ignoreNulls);
        }
        throw unsupportedType(item);
    }

    @Override
    public Map<String, AttributeValue> itemToMap(BaseEntity item, Collection<String> attributes) {
        if (item instanceof CustomerEntity customer) {
            return customerSchema.itemToMap(customer, attributes);
        }
        if (item instanceof DeviceEntity device) {
            return deviceSchema.itemToMap(device, attributes);
        }
        if (item instanceof TelemetryEntity telemetry) {
            return telemetrySchema.itemToMap(telemetry, attributes);
        }
        throw unsupportedType(item);
    }

    @Override
    public AttributeValue attributeValue(BaseEntity item, String attributeName) {
        if (item instanceof CustomerEntity customer) {
            return customerSchema.attributeValue(customer, attributeName);
        }
        if (item instanceof DeviceEntity device) {
            return deviceSchema.attributeValue(device, attributeName);
        }
        if (item instanceof TelemetryEntity telemetry) {
            return telemetrySchema.attributeValue(telemetry, attributeName);
        }
        throw unsupportedType(item);
    }

    @Override
    public TableMetadata tableMetadata() {
        // PK/SK structure (partition/sort key) is identical across all subtypes by design.
        return customerSchema.tableMetadata();
    }

    @Override
    public EnhancedType<BaseEntity> itemType() {
        return EnhancedType.of(BaseEntity.class);
    }

    @Override
    public List<String> attributeNames() {
        return Stream.of(customerSchema.attributeNames(), deviceSchema.attributeNames(), telemetrySchema.attributeNames())
                .flatMap(List::stream)
                .distinct()
                .toList();
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    private static IllegalArgumentException unsupportedType(BaseEntity item) {
        return new IllegalArgumentException("Unsupported entity type: " + item.getClass());
    }
}
