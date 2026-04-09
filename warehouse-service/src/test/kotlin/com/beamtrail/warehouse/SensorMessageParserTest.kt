package com.beamtrail.warehouse

import com.beamtrail.warehouse.model.ParseResult
import com.beamtrail.warehouse.model.SensorId
import com.beamtrail.warehouse.model.SensorMessageParser
import com.beamtrail.warehouse.model.SensorType
import com.beamtrail.warehouse.model.WarehouseId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SensorMessageParserTest {

    private val warehouseId = WarehouseId("warehouse-1")

    @Test
    fun `should parse temperature measurement`() {
        val result = SensorMessageParser.parse("sensor_id=t1; value=30", SensorType.TEMPERATURE, warehouseId)

        assertThat(result).isInstanceOf(ParseResult.Success::class.java)
        val measurement = (result as ParseResult.Success).measurement
        assertThat(measurement.sensorId).isEqualTo(SensorId("t1"))
        assertThat(measurement.value).isEqualTo(30.0)
        assertThat(measurement.type).isEqualTo(SensorType.TEMPERATURE)
        assertThat(measurement.warehouseId).isEqualTo(warehouseId)
    }

    @Test
    fun `should parse humidity measurement`() {
        val result = SensorMessageParser.parse("sensor_id=h1; value=40", SensorType.HUMIDITY, warehouseId)

        assertThat(result).isInstanceOf(ParseResult.Success::class.java)
        val measurement = (result as ParseResult.Success).measurement
        assertThat(measurement.sensorId).isEqualTo(SensorId("h1"))
        assertThat(measurement.value).isEqualTo(40.0)
        assertThat(measurement.type).isEqualTo(SensorType.HUMIDITY)
    }

    @Test
    fun `should parse decimal values`() {
        val result = SensorMessageParser.parse("sensor_id=t2; value=36.5", SensorType.TEMPERATURE, warehouseId)

        assertThat(result).isInstanceOf(ParseResult.Success::class.java)
        assertThat((result as ParseResult.Success).measurement.value).isEqualTo(36.5)
    }

    @Test
    fun `should parse with extra spaces`() {
        val result = SensorMessageParser.parse("  sensor_id = t1 ;  value = 30  ", SensorType.TEMPERATURE, warehouseId)

        assertThat(result).isInstanceOf(ParseResult.Success::class.java)
        val measurement = (result as ParseResult.Success).measurement
        assertThat(measurement.sensorId).isEqualTo(SensorId("t1"))
        assertThat(measurement.value).isEqualTo(30.0)
    }

    @Test
    fun `should return Failure for invalid message`() {
        val result = SensorMessageParser.parse("invalid message", SensorType.TEMPERATURE, warehouseId)

        assertThat(result).isInstanceOf(ParseResult.Failure::class.java)
        assertThat((result as ParseResult.Failure).reason).contains("does not match")
    }

    @Test
    fun `should return Failure for empty message`() {
        val result = SensorMessageParser.parse("", SensorType.TEMPERATURE, warehouseId)

        assertThat(result).isInstanceOf(ParseResult.Failure::class.java)
        assertThat((result as ParseResult.Failure).reason).contains("Empty")
    }

    @Test
    fun `should parse negative values`() {
        val result = SensorMessageParser.parse("sensor_id=t1; value=-5", SensorType.TEMPERATURE, warehouseId)

        assertThat(result).isInstanceOf(ParseResult.Success::class.java)
        assertThat((result as ParseResult.Success).measurement.value).isEqualTo(-5.0)
    }
}
