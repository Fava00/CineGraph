package com.martonegyed.core.util

import kotlin.math.roundToInt

inline fun <reified T : Number> T.roundToDecimals(decimals: Int): T {
    return when (this) {
        is Double -> {
            var factor = 1.0
            repeat(decimals) { factor *= 10.0 }
            val roundedValue = (this * factor).roundToInt()
            (roundedValue.toDouble() / factor) as T
        }
        is Float -> {
            var factor = 1f
            repeat(decimals) { factor *= 10f }
            val roundedValue = (this * factor).roundToInt()
            (roundedValue / factor) as T
        }
        else -> throw IllegalArgumentException("Unsupported number type: ${this::class}")
    }
}
