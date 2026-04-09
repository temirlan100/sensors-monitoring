package com.beamtrail.warehouse.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class SensorMeasurement @JsonCreator constructor(
    @param:JsonProperty("sensorId") val sensorId: SensorId,
    @param:JsonProperty("type") val type: SensorType,
    @param:JsonProperty("value") val value: Double,
    @param:JsonProperty("warehouseId") val warehouseId: WarehouseId
)
