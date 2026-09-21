package com.gk3.demo.serverless.telemetry.engine.repository.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class DeviceEntity extends BaseEntity {
    private String model;
    private String status;

    public DeviceEntity() {
        this.entityType = "DEVICE";
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

    @DynamoDbAttribute("DeviceModel")
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @DynamoDbAttribute("DeviceStatus")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
