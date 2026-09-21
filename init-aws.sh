#!/bin/bash
echo "=========== INITIALIZING LOCALSTACK SERVICES ==========="

# 1. Tworzenie tabeli DynamoDB (Single-Table Design)
echo "Creating DynamoDB table: FleetManagement..."
aws --endpoint-url=http://localhost:4566 dynamodb create-table \
    --table-name FleetManagement \
    --attribute-definitions \
        AttributeName=PK,AttributeType=S \
        AttributeName=SK,AttributeType=S \
    --key-schema \
        AttributeName=PK,KeyType=HASH \
        AttributeName=SK,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --region us-east-1

# 2. Tworzenie kolejki SQS dla danych telemetrycznych IoT
echo "Creating SQS Queue: telemetry-ingestion-queue..."
aws --endpoint-url=http://localhost:4566 sqs create-queue \
    --queue-name telemetry-ingestion-queue \
    --region us-east-1

echo "=========== LOCALSTACK INITIALIZATION COMPLETED ==========="
