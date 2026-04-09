package com.beamtrail.warehouse.config

import com.beamtrail.warehouse.model.SensorType
import com.beamtrail.warehouse.model.WarehouseId
import io.smallrye.config.ConfigMapping

@ConfigMapping(prefix = "warehouse")
interface WarehouseConfig {
    fun id(): String
    fun sensors(): Map<String, SensorPortConfig>

    interface SensorPortConfig {
        fun port(): Int
        fun type(): SensorType
    }
}

val WarehouseConfig.warehouseId: WarehouseId
    get() = WarehouseId(id())
