package com.martonegyed.domain.model

data class EntityStat (
    val name: String,
    val count: Int,
    val avgRating: Double,
    val totalRuntimeMinutes: Int = 0,
    val totalRevenue: Int = 0
)