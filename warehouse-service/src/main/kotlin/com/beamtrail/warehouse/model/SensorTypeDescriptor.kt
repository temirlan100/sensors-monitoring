package com.beamtrail.warehouse.model

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Binds a sensor type to its value parser.
 *
 * Implementations are created from config at startup — no need to write
 * a new class for each sensor type. The supported value kinds are:
 * `scalar`, `location`, `textual`, `composite`.
 */
interface SensorTypeDescriptor<out V : SensorValue> {
    val type: SensorType
    fun parseValue(raw: String): V?
}

// ── Config-driven implementations ─────────────────────────────────────────────

class ScalarDescriptor(
    override val type: SensorType,
    private val unit: String?
) : SensorTypeDescriptor<SensorValue.Scalar> {

    override fun parseValue(raw: String): SensorValue.Scalar? =
        raw.toDoubleOrNull()?.let { SensorValue.Scalar(it, unit) }
}

class LocationDescriptor(
    override val type: SensorType
) : SensorTypeDescriptor<SensorValue.Location> {

    override fun parseValue(raw: String): SensorValue.Location? {
        val parts = raw.split(",")
        if (parts.size != 2) return null
        val lat = parts[0].trim().toDoubleOrNull() ?: return null
        val lon = parts[1].trim().toDoubleOrNull() ?: return null
        return SensorValue.Location(lat, lon)
    }
}

class TextualDescriptor(
    override val type: SensorType
) : SensorTypeDescriptor<SensorValue.Textual> {

    override fun parseValue(raw: String): SensorValue.Textual =
        SensorValue.Textual(raw)
}

class CompositeDescriptor(
    override val type: SensorType,
    private val objectMapper: ObjectMapper
) : SensorTypeDescriptor<SensorValue.Composite> {

    override fun parseValue(raw: String): SensorValue.Composite? = try {
        val map: Map<String, Any> = objectMapper.readValue(raw, object : TypeReference<Map<String, Any>>() {})
        SensorValue.Composite(convertMap(map))
    } catch (_: Exception) {
        null
    }

    private fun convertMap(map: Map<String, Any>): Map<String, SensorValue> =
        map.mapValues { (_, v) -> convertValue(v) }

    @Suppress("UNCHECKED_CAST")
    private fun convertValue(value: Any): SensorValue = when (value) {
        is Number -> SensorValue.Scalar(value.toDouble())
        is String -> SensorValue.Textual(value)
        is Boolean -> SensorValue.Textual(value.toString())
        is Map<*, *> -> SensorValue.Composite(convertMap(value as Map<String, Any>))
        is List<*> -> SensorValue.Textual(value.toString())
        else -> SensorValue.Textual(value.toString())
    }
}
