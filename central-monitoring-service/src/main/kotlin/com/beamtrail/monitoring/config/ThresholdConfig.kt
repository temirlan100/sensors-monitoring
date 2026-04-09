package com.beamtrail.monitoring.config

import io.smallrye.config.ConfigMapping

@ConfigMapping(prefix = "threshold")
interface ThresholdConfig {
    fun temperature(): Double
    fun humidity(): Double
}
