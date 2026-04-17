package com.beamtrail.warehouse

import com.beamtrail.warehouse.model.ParseResult
import com.beamtrail.warehouse.model.ScalarDescriptor
import com.beamtrail.warehouse.model.LocationDescriptor
import com.beamtrail.warehouse.model.CompositeDescriptor
import com.beamtrail.warehouse.model.SensorId
import com.beamtrail.warehouse.model.SensorMessageParser
import com.beamtrail.warehouse.model.SensorType
import com.beamtrail.warehouse.model.SensorValue
import com.beamtrail.warehouse.model.WarehouseId
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SensorMessageParserTest {

    private val warehouseId = WarehouseId("warehouse-1")
    private val temperatureDescriptor = ScalarDescriptor(SensorType("TEMPERATURE"), "°C")
    private val humidityDescriptor = ScalarDescriptor(SensorType("HUMIDITY"), "%")
    private val locationDescriptor = LocationDescriptor(SensorType("LOCATION"))
    private val compositeDescriptor = CompositeDescriptor(SensorType("HVAC_STATUS"), jacksonObjectMapper())

    @Test
    fun `should parse temperature measurement`() {
        val result = SensorMessageParser.parse("sensor_id=t1; value=30", temperatureDescriptor, warehouseId)

        assertThat(result).isInstanceOf(ParseResult.Success::class.java)
        val measurement = (result as ParseResult.Success).measurement
        assertThat(measurement.sensorId).isEqualTo(SensorId("t1"))
        assertThat(measurement.type).isEqualTo(SensorType("TEMPERATURE"))
        assertThat(measurement.warehouseId).isEqualTo(warehouseId)
        val scalar = measurement.value as SensorValue.Scalar
        assertThat(scalar.amount).isEqualTo(30.0)
        assertThat(scalar.unit).isEqualTo("°C")
    }

    @Test
    fun `should parse humidity measurement`() {
        val result = SensorMessageParser.parse("sensor_id=h1; value=40", humidityDescriptor, warehouseId)

        assertThat(result).isInstanceOf(ParseResult.Success::class.java)
        val measurement = (result as ParseResult.Success).measurement
        assertThat(measurement.sensorId).isEqualTo(SensorId("h1"))
        assertThat(measurement.type).isEqualTo(SensorType("HUMIDITY"))
        val scalar = measurement.value as SensorValue.Scalar
        assertThat(scalar.amount).isEqualTo(40.0)
        assertThat(scalar.unit).isEqualTo("%")
    }

    @Test
    fun `should parse decimal values`() {
        val result = SensorMessageParser.parse("sensor_id=t2; value=36.5", temperatureDescriptor, warehouseId)

        assertThat(result).isInstanceOf(ParseResult.Success::class.java)
        val scalar = (result as ParseResult.Success).measurement.value as SensorValue.Scalar
        assertThat(scalar.amount).isEqualTo(36.5)
    }

    @Test
    fun `should parse with extra spaces`() {
        val result = SensorMessageParser.parse("  sensor_id = t1 ;  value = 30  ", temperatureDescriptor, warehouseId)

        assertThat(result).isInstanceOf(ParseResult.Success::class.java)
        val measurement = (result as ParseResult.Success).measurement
        assertThat(measurement.sensorId).isEqualTo(SensorId("t1"))
        val scalar = measurement.value as SensorValue.Scalar
        assertThat(scalar.amount).isEqualTo(30.0)
    }

    @Test
    fun `should return Failure for invalid message`() {
        val result = SensorMessageParser.parse("invalid message", temperatureDescriptor, warehouseId)

        assertThat(result).isInstanceOf(ParseResult.Failure::class.java)
        assertThat((result as ParseResult.Failure).reason).contains("does not match")
    }

    @Test
    fun `should return Failure for empty message`() {
        val result = SensorMessageParser.parse("", temperatureDescriptor, warehouseId)

        assertThat(result).isInstanceOf(ParseResult.Failure::class.java)
        assertThat((result as ParseResult.Failure).reason).contains("Empty")
    }

    @Test
    fun `should parse negative values`() {
        val result = SensorMessageParser.parse("sensor_id=t1; value=-5", temperatureDescriptor, warehouseId)

        assertThat(result).isInstanceOf(ParseResult.Success::class.java)
        val scalar = (result as ParseResult.Success).measurement.value as SensorValue.Scalar
        assertThat(scalar.amount).isEqualTo(-5.0)
    }

    @Test
    fun `should parse location value`() {
        val result = SensorMessageParser.parse("sensor_id=gps1; value=51.5074,-0.1278", locationDescriptor, warehouseId)

        assertThat(result).isInstanceOf(ParseResult.Success::class.java)
        val measurement = (result as ParseResult.Success).measurement
        assertThat(measurement.type).isEqualTo(SensorType("LOCATION"))
        val location = measurement.value as SensorValue.Location
        assertThat(location.latitude).isEqualTo(51.5074)
        assertThat(location.longitude).isEqualTo(-0.1278)
    }

    @Test
    fun `should parse composite value`() {
        val json = """{"temperature":22.5,"mode":"cooling","fanSpeed":3}"""
        val result = SensorMessageParser.parse("sensor_id=hvac1; value=$json", compositeDescriptor, warehouseId)

        assertThat(result).isInstanceOf(ParseResult.Success::class.java)
        val measurement = (result as ParseResult.Success).measurement
        assertThat(measurement.type).isEqualTo(SensorType("HVAC_STATUS"))
        val composite = measurement.value as SensorValue.Composite
        assertThat(composite.fields).containsKey("temperature")
        assertThat(composite.fields).containsKey("mode")
        assertThat(composite.fields).containsKey("fanSpeed")
        assertThat((composite.fields["temperature"] as SensorValue.Scalar).amount).isEqualTo(22.5)
        assertThat((composite.fields["mode"] as SensorValue.Textual).content).isEqualTo("cooling")
        assertThat((composite.fields["fanSpeed"] as SensorValue.Scalar).amount).isEqualTo(3.0)
    }

    @Test
    fun `should parse composite value with nested objects`() {
        val json = """{"temperature":22.5,"filters":{"status":"clean","hoursRemaining":120}}"""
        val result = SensorMessageParser.parse("sensor_id=hvac1; value=$json", compositeDescriptor, warehouseId)

        assertThat(result).isInstanceOf(ParseResult.Success::class.java)
        val composite = (result as ParseResult.Success).measurement.value as SensorValue.Composite
        val filters = composite.fields["filters"] as SensorValue.Composite
        assertThat((filters.fields["status"] as SensorValue.Textual).content).isEqualTo("clean")
        assertThat((filters.fields["hoursRemaining"] as SensorValue.Scalar).amount).isEqualTo(120.0)
    }

    @Test
    fun `should return Failure for invalid location format`() {
        val result = SensorMessageParser.parse("sensor_id=gps1; value=not-a-location", locationDescriptor, warehouseId)

        assertThat(result).isInstanceOf(ParseResult.Failure::class.java)
        assertThat((result as ParseResult.Failure).reason).contains("Cannot parse value")
    }
}
