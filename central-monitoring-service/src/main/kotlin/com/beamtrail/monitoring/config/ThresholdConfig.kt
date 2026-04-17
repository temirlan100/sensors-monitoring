package com.beamtrail.monitoring.config

import io.smallrye.config.ConfigMapping

@ConfigMapping(prefix = "threshold")
interface ThresholdConfig {
    /** Map of sensor type name → max allowed scalar value, e.g. TEMPERATURE → 35.0. */
    fun sensors(): Map<String, Double>
}
