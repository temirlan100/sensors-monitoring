package com.beamtrail.warehouse.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Sealed hierarchy of all possible sensor values.
 *
 * Add a new subtype here when a new value shape is needed.
 * The sealed constraint means `when(value)` is exhaustive — the compiler
 * will warn if a new subtype is not handled in downstream consumers.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes(
    JsonSubTypes.Type(value = SensorValue.Scalar::class, name = "scalar"),
    JsonSubTypes.Type(value = SensorValue.Location::class, name = "location"),
    JsonSubTypes.Type(value = SensorValue.Textual::class, name = "textual"),
    JsonSubTypes.Type(value = SensorValue.Composite::class, name = "composite"),
)
sealed interface SensorValue {

    /** Single numeric reading, e.g. 21.5 °C or 60 %. */
    data class Scalar(val amount: Double, val unit: String? = null) : SensorValue

    /** Geographic coordinates. */
    data class Location(val latitude: Double, val longitude: Double) : SensorValue

    /** Free-form string, e.g. a status code or event label. */
    data class Textual(val content: String) : SensorValue

    /** Composite reading: named sub-values of any kind. */
    data class Composite(val fields: Map<String, SensorValue>) : SensorValue
}
