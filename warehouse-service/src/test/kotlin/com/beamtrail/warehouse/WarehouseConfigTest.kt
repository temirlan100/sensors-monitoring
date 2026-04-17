package com.beamtrail.warehouse

import com.beamtrail.warehouse.config.WarehouseConfig
import com.beamtrail.warehouse.config.sensorType
import com.beamtrail.warehouse.config.warehouseId
import com.beamtrail.warehouse.model.SensorType
import com.beamtrail.warehouse.model.WarehouseId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Optional

class WarehouseConfigTest {

    @Test
    fun `warehouseId extension should wrap id() into WarehouseId value class`() {
        val config: WarehouseConfig = mock()
        whenever(config.id()).thenReturn("warehouse-42")

        assertThat(config.warehouseId).isEqualTo(WarehouseId("warehouse-42"))
    }

    @Test
    fun `sensors map should provide port, type, valueKind and unit per sensor`() {
        val tempSensor: WarehouseConfig.SensorPortConfig = mock()
        whenever(tempSensor.port()).thenReturn(3344)
        whenever(tempSensor.type()).thenReturn("TEMPERATURE")
        whenever(tempSensor.valueKind()).thenReturn("scalar")
        whenever(tempSensor.unit()).thenReturn(Optional.of("°C"))

        val gpsSensor: WarehouseConfig.SensorPortConfig = mock()
        whenever(gpsSensor.port()).thenReturn(3366)
        whenever(gpsSensor.type()).thenReturn("LOCATION")
        whenever(gpsSensor.valueKind()).thenReturn("location")
        whenever(gpsSensor.unit()).thenReturn(Optional.empty())

        val config: WarehouseConfig = mock()
        whenever(config.sensors()).thenReturn(mapOf("temperature" to tempSensor, "gps" to gpsSensor))

        val sensors = config.sensors()
        assertThat(sensors).hasSize(2)
        assertThat(sensors["temperature"]!!.sensorType).isEqualTo(SensorType("TEMPERATURE"))
        assertThat(sensors["temperature"]!!.valueKind()).isEqualTo("scalar")
        assertThat(sensors["temperature"]!!.unit()).isEqualTo(Optional.of("°C"))
        assertThat(sensors["gps"]!!.sensorType).isEqualTo(SensorType("LOCATION"))
        assertThat(sensors["gps"]!!.valueKind()).isEqualTo("location")
        assertThat(sensors["gps"]!!.unit()).isEqualTo(Optional.empty<String>())
    }
}
