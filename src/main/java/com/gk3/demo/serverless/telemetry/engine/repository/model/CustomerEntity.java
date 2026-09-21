package com.gk3.demo.serverless.telemetry.engine.repository.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class CustomerEntity extends BaseEntity {
    private String name;
    private String email;
    private Boolean premiumSupportActive; // Nowe pole bazodanowe

    public CustomerEntity() {
        this.entityType = "CUSTOMER";
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

    @DynamoDbAttribute("CustomerName")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @DynamoDbAttribute("CustomerEmail")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @DynamoDbAttribute("PremiumSupportActive")
    public Boolean getPremiumSupportActive() {
        return premiumSupportActive;
    }

    public void setPremiumSupportActive(Boolean premiumSupportActive) {
        this.premiumSupportActive = premiumSupportActive;
    }
}
