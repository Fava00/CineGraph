package com.martonegyed.core.util

import io.github.vinceglb.filekit.core.PlatformFile
import java.io.File

actual suspend fun writePickedFile(file: PlatformFile, bytes: ByteArray) {
    val pathField = file::class.java.getDeclaredField("path")
    pathField.isAccessible = true
    val path = pathField.get(file) as String

    File(path).writeBytes(bytes)
}