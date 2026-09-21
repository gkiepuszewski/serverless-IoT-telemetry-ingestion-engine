package com.gk3.demo.serverless.telemetry.engine.repository.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class TelemetryEntity extends BaseEntity {
    private Double fuelLevel;
    private Double latitude;
    private Double longitude;
    private Double hydraulicPressurePsi; // Nowe pole bazodanowe

    public TelemetryEntity() {
        this.entityType = "TELEMETRY";
    }

    @Override
    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")
    public String getPk() {
        return super.getPk();
    }

    @Override
    @DynamoDbSortKey
    @DynamoDbAttribute("SK")
    public String getSk() {
        return super.getSk();
    }

    @DynamoDbAttribute("FuelLevel")
    public Double getFuelLevel() {
        return fuelLevel;
    }

    public void setFuelLevel(Double fuelLevel) {
        this.fuelLevel = fuelLevel;
    }

    @DynamoDbAttribute("Latitude")
    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    @DynamoDbAttribute("Longitude")
    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    @DynamoDbAttribute("HydraulicPressurePsi")
    public Double getHydraulicPressurePsi() {
        return hydraulicPressurePsi;
    }

    public void setHydraulicPressurePsi(Double hydraulicPressurePsi) {
        this.hydraulicPressurePsi = hydraulicPressurePsi;
    }
}
