package com.gk3.demo.serverless.telemetry.engine.repository.schema;

import com.gk3.demo.serverless.telemetry.engine.repository.model.BaseEntity;
import com.gk3.demo.serverless.telemetry.engine.repository.model.CustomerEntity;
import com.gk3.demo.serverless.telemetry.engine.repository.model.DeviceEntity;
import com.gk3.demo.serverless.telemetry.engine.repository.model.TelemetryEntity;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PolymorphicFleetTableSchemaTest {

    private final PolymorphicFleetTableSchema schema = new PolymorphicFleetTableSchema();

    @Test
    void shouldRoundTripCustomerEntityThroughDiscriminatedMap() {
        CustomerEntity customer = new CustomerEntity();
        customer.setPk("CUSTOMER#101");
        customer.setSk("METADATA");
        customer.setName("Acme Corp");
        customer.setEmail("acme@example.com");
        customer.setPremiumSupportActive(true);

        Map<String, AttributeValue> map = schema.itemToMap(customer, true);
        assertThat(map.get("EntityType").s()).isEqualTo("CUSTOMER");

        BaseEntity mapped = schema.mapToItem(map);
        assertThat(mapped).isInstanceOf(CustomerEntity.class);
        CustomerEntity result = (CustomerEntity) mapped;
        assertThat(result.getPk()).isEqualTo("CUSTOMER#101");
        assertThat(result.getName()).isEqualTo("Acme Corp");
    }

    @Test
    void shouldRoundTripDeviceEntityThroughDiscriminatedMap() {
        DeviceEntity device = new DeviceEntity();
        device.setPk("CUSTOMER#101");
        device.setSk("DEVICE#VIN-1");
        device.setModel("CAT-797F");
        device.setStatus("OPERATIONAL");

        Map<String, AttributeValue> map = schema.itemToMap(device, true);
        assertThat(map.get("EntityType").s()).isEqualTo("DEVICE");

        BaseEntity mapped = schema.mapToItem(map);
        assertThat(mapped).isInstanceOf(DeviceEntity.class);
        assertThat(((DeviceEntity) mapped).getStatus()).isEqualTo("OPERATIONAL");
    }

    @Test
    void shouldRoundTripTelemetryEntityThroughDiscriminatedMap() {
        TelemetryEntity telemetry = new TelemetryEntity();
        telemetry.setPk("DEVICE#VIN-1");
        telemetry.setSk("TELEMETRY#2026-01-01T00:00:00Z");
        telemetry.setFuelLevel(80.0);
        telemetry.setHydraulicPressurePsi(3200.0);

        Map<String, AttributeValue> map = schema.itemToMap(telemetry, true);
        assertThat(map.get("EntityType").s()).isEqualTo("TELEMETRY");

        BaseEntity mapped = schema.mapToItem(map);
        assertThat(mapped).isInstanceOf(TelemetryEntity.class);
        assertThat(((TelemetryEntity) mapped).getHydraulicPressurePsi()).isEqualTo(3200.0);
    }
}
