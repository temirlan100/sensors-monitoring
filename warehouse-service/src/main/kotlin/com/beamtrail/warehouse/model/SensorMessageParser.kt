package com.beamtrail.warehouse.model

object SensorMessageParser {

    private val PATTERN = Regex("""sensor_id\s*=\s*(\S+)\s*;\s*value\s*=\s*([+-]?\d+(?:\.\d+)?)""")

    fun parse(raw: String, type: SensorType, warehouseId: WarehouseId): ParseResult {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) {
            return ParseResult.Failure(reason = "Empty message", rawInput = raw)
        }

        val match = PATTERN.find(trimmed)
            ?: return ParseResult.Failure(reason = "Message does not match expected format", rawInput = raw)

        val sensorId = match.groupValues[1]
        val value = match.groupValues[2].toDoubleOrNull()
            ?: return ParseResult.Failure(reason = "Invalid numeric value", rawInput = raw)

        return ParseResult.Success(
            SensorMeasurement(
                sensorId = SensorId(sensorId),
                type = type,
                value = value,
                warehouseId = warehouseId
            )
        )
    }
}
