#!/usr/bin/env bash
# Publishes a batch of randomized telemetry readings over MQTT to exercise the
# MQTT -> SQS -> DynamoDB pipeline at higher volume (useful for demos/load checks).
#
# Run this from a WSL / Linux shell where `docker` is on PATH (the same shell you use
# for `docker compose up`). It does NOT work in native Windows PowerShell unless Docker
# Desktop's PowerShell integration is installed - use the WSL Ubuntu terminal instead.
#
# Usage:
#   ./scripts/generate-telemetry-batch.sh [COUNT] [CUSTOMER_ID] [DEVICE_ID] [CRITICAL_RATIO] [DELAY_SECONDS]
#
# Example:
#   ./scripts/generate-telemetry-batch.sh 50 101 VIN-HEAVY-TRUCK-99 0.15 0.1

set -euo pipefail

COUNT="${1:-20}"
CUSTOMER_ID="${2:-101}"
DEVICE_ID="${3:-VIN-HEAVY-TRUCK-99}"
CRITICAL_RATIO="${4:-0.1}"
DELAY_SECONDS="${5:-0.2}"

TOPIC="machines/${CUSTOMER_ID}/${DEVICE_ID}/telemetry"
CONTAINER="mosquitto_mqtt_broker"

echo "Publishing ${COUNT} telemetry messages to topic '${TOPIC}' (container: ${CONTAINER})..."

for ((i = 1; i <= COUNT; i++)); do
    # Decide (via awk, since bash has no floating point) if this reading is "critical".
    is_critical=$(awk -v r="$RANDOM" -v ratio="$CRITICAL_RATIO" 'BEGIN { print (r/32767 < ratio) ? 1 : 0 }')

    timestamp=$(date -u -d "-${i} seconds" +"%Y-%m-%dT%H:%M:%S.000Z")
    fuel_level=$(awk -v seed="$RANDOM" 'BEGIN { srand(seed); printf "%.1f", 10 + rand()*90 }')
    latitude=$(awk -v seed="$RANDOM" 'BEGIN { srand(seed); printf "%.5f", 50.0 + rand()*0.5 }')
    longitude=$(awk -v seed="$RANDOM" 'BEGIN { srand(seed); printf "%.5f", 19.9 + rand()*0.5 }')

    if [ "$is_critical" -eq 1 ]; then
        pressure=$((3001 + RANDOM % 500))
        tag="CRITICAL"
    else
        pressure=$((1500 + RANDOM % 1500))
        tag="normal"
    fi

    payload=$(printf '{"timestamp":"%s","fuelLevel":%s,"latitude":%s,"longitude":%s,"hydraulicPressurePsi":%s}' \
        "$timestamp" "$fuel_level" "$latitude" "$longitude" "$pressure")

    docker exec "$CONTAINER" mosquitto_pub -h localhost -t "$TOPIC" -m "$payload"

    echo "[$i/$COUNT] $tag -> pressure=${pressure} psi"
    sleep "$DELAY_SECONDS"
done

echo "Done. Check DynamoDB / app logs / SQS queue depth as described in README.md."
