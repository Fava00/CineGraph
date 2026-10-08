package com.martonegyed.core.util

import android.net.Uri
import com.martonegyed.domain.repository.BackupOutput
import io.github.vinceglb.filekit.core.PlatformFile

actual fun backupFileOutput(file: PlatformFile): BackupOutput {
    val uriField = file::class.java.getDeclaredField("uri")
    uriField.isAccessible = true
    val uri = uriField.get(file) as Uri
    val stream = (appContext.contentResolver.openOutputStream(uri, "wt")
        ?: error("Could not open backup destination")).buffered()
    return object : BackupOutput {
        override fun write(bytes: ByteArray) = stream.write(bytes)
        override fun close() = stream.close()
    }
}
