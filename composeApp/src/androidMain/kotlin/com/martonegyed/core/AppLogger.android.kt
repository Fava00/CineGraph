package com.martonegyed.core

actual object AppLogger {
    actual fun exception(tag: String, throwable: Throwable, message: String?) {
        android.util.Log.e(tag, message ?: "Unexpected error", throwable)
    }
}
