package com.beamtrail.warehouse.udp

import com.beamtrail.warehouse.model.SensorMessageParser
import com.beamtrail.warehouse.model.SensorType
import com.beamtrail.warehouse.model.WarehouseId
import com.beamtrail.warehouse.model.onFailure
import com.beamtrail.warehouse.model.onSuccess
import com.beamtrail.warehouse.config.WarehouseConfig
import com.beamtrail.warehouse.config.warehouseId
import com.beamtrail.warehouse.kafka.MeasurementEmitter
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
    private val config: WarehouseConfig
) {

    private val log = Logger.getLogger(UdpSensorListener::class.java)
    private val scope by lazy { CoroutineScope(vertx.dispatcher() + SupervisorJob()) }

    fun onStart(@Observes event: StartupEvent) {
        config.sensors().forEach { (name, sensorConfig) ->
            scope.launch {
                startListener(name, sensorConfig.port(), sensorConfig.type(), config.warehouseId)
            }
        }
    }

    private suspend fun startListener(name: String, port: Int, type: SensorType, warehouseId: WarehouseId) {
        val socket: DatagramSocket = vertx.createDatagramSocket()
            .listen(port, "0.0.0.0")
            .coAwait()

        log.info("UDP listener '$name' started on port $port for $type sensors")

        socket.handler { packet ->
            val raw = packet.data().toString(Charsets.UTF_8)
            log.debug("Received UDP [$name:$port]: $raw")

            SensorMessageParser.parse(raw, type, warehouseId)
                .onSuccess { measurement ->
                    scope.launch { emitter.send(measurement) }
                }
                .onFailure { reason, rawInput ->
                    log.warn("Parse failed [$name]: $reason | input='$rawInput'")
                }
        }
    }
}
