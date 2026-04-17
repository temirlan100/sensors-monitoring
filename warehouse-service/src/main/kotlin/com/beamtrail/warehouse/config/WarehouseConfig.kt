package com.beamtrail.warehouse.config

import com.beamtrail.warehouse.model.SensorType
import com.beamtrail.warehouse.model.WarehouseId
import io.smallrye.config.ConfigMapping
import java.util.Optional

@ConfigMapping(prefix = "warehouse")
interface WarehouseConfig {
    fun id(): String
    fun sensors(): Map<String, SensorPortConfig>

    interface SensorPortConfig {
        fun port(): Int
        fun type(): String
        /** Value kind: scalar, location, textual, composite. */
        fun valueKind(): String
        /** Unit label for scalar values (e.g. "°C", "%"). Ignored for other value kinds. */
        fun unit(): Optional<String>
    }
}

val WarehouseConfig.warehouseId: WarehouseId
    get() = WarehouseId(id())

val WarehouseConfig.SensorPortConfig.sensorType: SensorType
    get() = SensorType(type())
