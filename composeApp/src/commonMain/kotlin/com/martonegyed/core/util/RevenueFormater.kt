package com.martonegyed.core.util

import kotlin.math.round

fun revenueFormater(amount: Long): String {
    val inBillions = amount / 1_000_000_000.0
    val inMillions = amount / 1_000_000.0
    val inThousands = amount / 1_000.0

    val suffix = when {
        amount >= 1_000_000_000L -> "${round(inBillions * 10.0) / 10.0}B"
        amount >= 1_000_000L     -> "${round(inMillions * 10.0) / 10.0}M"
        amount >= 1_000L         -> "${round(inThousands * 10.0) / 10.0}K"
        else                     -> amount.toString()
    }

    return "$$suffix"
}