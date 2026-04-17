package com.beamtrail.warehouse.udp

import com.beamtrail.warehouse.model.SensorMessageParser
import com.beamtrail.warehouse.model.SensorTypeDescriptor
import com.beamtrail.warehouse.model.ScalarDescriptor
import com.beamtrail.warehouse.model.LocationDescriptor
import com.beamtrail.warehouse.model.TextualDescriptor
import com.beamtrail.warehouse.model.CompositeDescriptor
import com.beamtrail.warehouse.model.SensorValue
import com.beamtrail.warehouse.model.WarehouseId
import com.beamtrail.warehouse.model.onFailure
import com.beamtrail.warehouse.model.onSuccess
import com.beamtrail.warehouse.config.WarehouseConfig
import com.beamtrail.warehouse.config.warehouseId
import com.beamtrail.warehouse.config.sensorType
import com.beamtrail.warehouse.kafka.MeasurementEmitter
import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.runtime.StartupEvent
import io.vertx.core.Vertx
import io.vertx.core.datagram.DatagramSocket
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jboss.logging.Logger

@ApplicationScoped
class UdpSensorListener(
    private val vertx: Vertx,
    private val emitter: MeasurementEmitter,
    private val config: WarehouseConfig,
    private val objectMapper: ObjectMapper
) {

    private val log = Logger.getLogger(UdpSensorListener::class.java)
    private val scope by lazy { CoroutineScope(vertx.dispatcher() + SupervisorJob()) }

    fun onStart(@Observes event: StartupEvent) {
        config.sensors().forEach { (name, sensorConfig) ->
            val descriptor = createDescriptor(sensorConfig)
            if (descriptor == null) {
                log.error("Unknown value-kind '${sensorConfig.valueKind()}' for sensor '$name' — skipping")
                return@forEach
            }
            scope.launch {
                startListener(name, sensorConfig.port(), descriptor, config.warehouseId)
            }
        }
    }

    private fun createDescriptor(sensorConfig: WarehouseConfig.SensorPortConfig): SensorTypeDescriptor<*>? {
        val type = sensorConfig.sensorType
        val unit = sensorConfig.unit().orElse(null)
        return when (sensorConfig.valueKind()) {
            "scalar" -> ScalarDescriptor(type, unit)
            "location" -> LocationDescriptor(type)
            "textual" -> TextualDescriptor(type)
            "composite" -> CompositeDescriptor(type, objectMapper)
            else -> null
        }
    }

    private suspend fun startListener(
        name: String,
        port: Int,
        descriptor: SensorTypeDescriptor<*>,
        warehouseId: WarehouseId
    ) {
        val socket: DatagramSocket = vertx.createDatagramSocket()
            .listen(port, "0.0.0.0")
            .coAwait()

        log.info("UDP listener '$name' started on port $port for ${descriptor.type.name} sensors")

        socket.handler { packet ->
            val raw = packet.data().toString(Charsets.UTF_8)
            log.debug("Received UDP [$name:$port]: $raw")

            SensorMessageParser.parse(raw, descriptor, warehouseId)
                .onSuccess { measurement ->
                    scope.launch { emitter.send(measurement) }
                }
                .onFailure { reason, rawInput ->
                    log.warn("Parse failed [$name]: $reason | input='$rawInput'")
                }
        }
    }
}
