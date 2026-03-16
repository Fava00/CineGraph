package com.martonegyed

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform