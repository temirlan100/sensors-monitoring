package com.beamtrail.warehouse.model

sealed interface ParseResult {
    data class Success(val measurement: SensorMeasurement) : ParseResult
    data class Failure(val reason: String, val rawInput: String) : ParseResult
}

inline fun ParseResult.onSuccess(action: (SensorMeasurement) -> Unit): ParseResult {
    if (this is ParseResult.Success)
        action(measurement)
    return this
}

inline fun ParseResult.onFailure(action: (reason: String, rawInput: String) -> Unit): ParseResult {
    if (this is ParseResult.Failure)
        action(reason, rawInput)
    return this
}
