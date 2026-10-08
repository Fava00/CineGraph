package com.martonegyed.core.util

import com.martonegyed.domain.repository.BackupInput
import com.martonegyed.domain.repository.BackupSource
import io.github.vinceglb.filekit.core.PlatformFile

actual fun backupFileSource(file: PlatformFile): BackupSource = BackupSource {
    val stream = appContext.contentResolver.openInputStream(file.uri)
        ?: error("Could not open backup")
    object : BackupInput {
        override fun read(buffer: ByteArray): Int = stream.read(buffer)
        override fun close() = stream.close()
    }
}
