package com.martonegyed.core.util

import android.content.Context
import android.net.Uri
import io.github.vinceglb.filekit.core.PlatformFile

lateinit var appContext: Context

actual suspend fun writePickedFile(file: PlatformFile, bytes: ByteArray) {
    val uriField = file::class.java.getDeclaredField("uri")
    uriField.isAccessible = true
    val uri = uriField.get(file) as Uri

    appContext.contentResolver.openOutputStream(uri)?.use { output ->
        output.write(bytes)
        output.flush()
    } ?: error("Could not open output stream")
}