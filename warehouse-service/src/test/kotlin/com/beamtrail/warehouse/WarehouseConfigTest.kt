package com.beamtrail.warehouse

import com.beamtrail.warehouse.config.WarehouseConfig
import com.beamtrail.warehouse.config.warehouseId
import com.beamtrail.warehouse.model.SensorType
import com.beamtrail.warehouse.model.WarehouseId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WarehouseConfigTest {

    @Test
    fun `warehouseId extension should wrap id() into WarehouseId value class`() {
        val config: WarehouseConfig = mock()
        whenever(config.id()).thenReturn("warehouse-42")

        assertThat(config.warehouseId).isEqualTo(WarehouseId("warehouse-42"))
    }

    @Test
    fun `sensors map should provide port and type per sensor`() {
        val tempSensor: WarehouseConfig.SensorPortConfig = mock()
        whenever(tempSensor.port()).thenReturn(3344)
        whenever(tempSensor.type()).thenReturn(SensorType.TEMPERATURE)

        val humiditySensor: WarehouseConfig.SensorPortConfig = mock()
        whenever(humiditySensor.port()).thenReturn(3355)
        whenever(humiditySensor.type()).thenReturn(SensorType.HUMIDITY)

        val config: WarehouseConfig = mock()
        whenever(config.sensors()).thenReturn(mapOf("temperature" to tempSensor, "humidity" to humiditySensor))

        val sensors = config.sensors()
        assertThat(sensors).hasSize(2)
        assertThat(sensors["temperature"]!!.port()).isEqualTo(3344)
        assertThat(sensors["temperature"]!!.type()).isEqualTo(SensorType.TEMPERATURE)
        assertThat(sensors["humidity"]!!.port()).isEqualTo(3355)
        assertThat(sensors["humidity"]!!.type()).isEqualTo(SensorType.HUMIDITY)
    }
}
