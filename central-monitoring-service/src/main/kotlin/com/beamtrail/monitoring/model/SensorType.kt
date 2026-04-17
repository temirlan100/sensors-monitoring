package com.beamtrail.monitoring.model

import com.fasterxml.jackson.annotation.JsonValue

@JvmInline
value class SensorType(@JsonValue val name: String)
