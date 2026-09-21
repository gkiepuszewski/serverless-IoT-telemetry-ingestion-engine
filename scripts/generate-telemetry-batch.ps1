<#
.SYNOPSIS
    Publishes a batch of randomized telemetry readings over MQTT to exercise the
    MQTT -> SQS -> DynamoDB pipeline at higher volume (useful for demos/load checks).

.PARAMETER Count
    Number of telemetry messages to publish. Default: 20.

.PARAMETER CustomerId
    Customer id used in the MQTT topic. Default: 101.

.PARAMETER DeviceId
    Device id used in the MQTT topic. Default: VIN-HEAVY-TRUCK-99.

.PARAMETER CriticalRatio
    Fraction (0.0-1.0) of messages that should trip the CRITICAL_ERROR business rule
    (hydraulic pressure > 3000 PSI). Default: 0.1 (roughly 1 in 10).

.PARAMETER DelayMs
    Delay in milliseconds between publishes. Default: 200.

.EXAMPLE
    .\scripts\generate-telemetry-batch.ps1 -Count 50 -CriticalRatio 0.2 -DelayMs 100
#>
param(
    [int]$Count = 20,
    [string]$CustomerId = "101",
    [string]$DeviceId = "VIN-HEAVY-TRUCK-99",
    [double]$CriticalRatio = 0.1,
    [int]$DelayMs = 200
)

$topic = "machines/$CustomerId/$DeviceId/telemetry"
Write-Host "Publishing $Count telemetry messages to topic '$topic' (~$([math]::Round($CriticalRatio*100))% critical)..." -ForegroundColor Cyan

for ($i = 1; $i -le $Count; $i++) {
    $isCritical = (Get-Random -Minimum 0.0 -Maximum 1.0) -lt $CriticalRatio
    $timestamp = (Get-Date).ToUniversalTime().AddSeconds(-$i).ToString("yyyy-MM-ddTHH:mm:ss.fffZ")

    $fuelLevel = [math]::Round((Get-Random -Minimum 10.0 -Maximum 100.0), 1)
    $latitude = [math]::Round((Get-Random -Minimum 50.0 -Maximum 50.5), 5)
    $longitude = [math]::Round((Get-Random -Minimum 19.9 -Maximum 20.4), 5)
    $hydraulicPressure = if ($isCritical) {
        Get-Random -Minimum 3001 -Maximum 3500
    } else {
        Get-Random -Minimum 1500 -Maximum 2999
    }

    $payload = @{
        timestamp            = $timestamp
        fuelLevel            = $fuelLevel
        latitude              = $latitude
        longitude             = $longitude
        hydraulicPressurePsi = $hydraulicPressure
    } | ConvertTo-Json -Compress

    # mosquitto_pub -m takes the payload directly - no file copy into the container needed.
    docker compose exec -T mosquitto mosquitto_pub -h localhost -t $topic -m $payload

    $tag = if ($isCritical) { "CRITICAL" } else { "normal" }
    Write-Host ("[{0}/{1}] {2} -> pressure={3} psi" -f $i, $Count, $tag, $hydraulicPressure)

    if ($DelayMs -gt 0) {
        Start-Sleep -Milliseconds $DelayMs
    }
}

Write-Host "Done. Check DynamoDB / app logs / SQS queue depth as described in README.md." -ForegroundColor Green
