package com.martonegyed.core

actual object AppLogger {
    actual fun exception(tag: String, throwable: Throwable, message: String?) {
        val logger = org.slf4j.LoggerFactory.getLogger(tag)
        logger.error(message ?: "Unexpected error", throwable)
    }
}