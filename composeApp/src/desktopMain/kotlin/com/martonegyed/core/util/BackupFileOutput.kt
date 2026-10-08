package com.martonegyed.core.util

import com.martonegyed.domain.repository.BackupOutput
import io.github.vinceglb.filekit.core.PlatformFile
import java.io.File

actual fun backupFileOutput(file: PlatformFile): BackupOutput {
    val pathField = file::class.java.getDeclaredField("path")
    pathField.isAccessible = true
    val stream = File(pathField.get(file) as String).outputStream().buffered()
    return object : BackupOutput {
        override fun write(bytes: ByteArray) = stream.write(bytes)
        override fun close() = stream.close()
    }
}
