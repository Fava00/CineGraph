package com.martonegyed.core

private val logger = co.touchlab.kermit.Logger.withTag("App")

actual object AppLogger {
    actual fun exception(tag: String, throwable: Throwable, message: String?) {
        logger.withTag(tag).e(throwable) { message ?: "Unexpected error" }
    }
}