package com.martonegyed.core

expect object AppLogger {
    fun exception(tag: String, throwable: Throwable, message: String? = null)
}