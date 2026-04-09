package com.beamtrail.monitoring.model

import com.fasterxml.jackson.annotation.JsonProperty

enum class SensorType(val unit: String) {
    @JsonProperty("TEMPERATURE")
    TEMPERATURE(unit = "°C"),

    @JsonProperty("HUMIDITY")
    HUMIDITY(unit = "%");
}
