package com.beamtrail.warehouse.model

object SensorMessageParser {

    // Value capture group is now (.+) — the descriptor decides how to interpret it.
    private val PATTERN = Regex("""sensor_id\s*=\s*(\S+)\s*;\s*value\s*=\s*(.+)""")

    fun <V : SensorValue> parse(
        raw: String,
        descriptor: SensorTypeDescriptor<V>,
        warehouseId: WarehouseId
    ): ParseResult {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) {
            return ParseResult.Failure(reason = "Empty message", rawInput = raw)
        }

        val match = PATTERN.find(trimmed)
            ?: return ParseResult.Failure(reason = "Message does not match expected format", rawInput = raw)

        val sensorId = match.groupValues[1]
        val rawValue = match.groupValues[2].trim()

        val value = descriptor.parseValue(rawValue)
            ?: return ParseResult.Failure(
                reason = "Cannot parse value '$rawValue' for sensor type '${descriptor.type.name}'",
                rawInput = raw
            )

        return ParseResult.Success(
            SensorMeasurement(
                sensorId = SensorId(sensorId),
                type = descriptor.type,
                value = value,
                warehouseId = warehouseId
            )
        )
    }
}
