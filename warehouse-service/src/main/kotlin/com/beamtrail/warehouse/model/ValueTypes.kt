package com.beamtrail.warehouse.model

import com.fasterxml.jackson.annotation.JsonValue

@JvmInline
value class SensorId(@JsonValue val value: String) {
    init {
        require(value.isNotBlank()) { "SensorId must not be blank" }
    }

    override fun toString(): String = value
}

@JvmInline
value class WarehouseId(@JsonValue val value: String) {
    init {
        require(value.isNotBlank()) { "WarehouseId must not be blank" }
    }

    override fun toString(): String = value
}
