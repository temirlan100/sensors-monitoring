package com.beamtrail.monitoring.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes(
    JsonSubTypes.Type(value = SensorValue.Scalar::class, name = "scalar"),
    JsonSubTypes.Type(value = SensorValue.Location::class, name = "location"),
    JsonSubTypes.Type(value = SensorValue.Textual::class, name = "textual"),
    JsonSubTypes.Type(value = SensorValue.Composite::class, name = "composite"),
)
sealed interface SensorValue {
    data class Scalar(val amount: Double, val unit: String? = null) : SensorValue
    data class Location(val latitude: Double, val longitude: Double) : SensorValue
    data class Textual(val content: String) : SensorValue
    data class Composite(val fields: Map<String, SensorValue>) : SensorValue
}
