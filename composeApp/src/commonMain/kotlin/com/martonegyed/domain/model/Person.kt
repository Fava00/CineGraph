package com.martonegyed.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Person (
    val name: String? = null,
    val profilePath: String? = null,
    val character: String? = null,
    val job: String? = null
)