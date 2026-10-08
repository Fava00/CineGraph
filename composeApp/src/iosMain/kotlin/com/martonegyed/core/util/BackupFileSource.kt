package com.martonegyed.core.util

import com.martonegyed.domain.repository.BackupInput
import com.martonegyed.domain.repository.BackupSource
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.Foundation.NSInputStream

@OptIn(ExperimentalForeignApi::class)
actual fun backupFileSource(file: PlatformFile): BackupSource = BackupSource {
    val scoped = file.nsUrl.startAccessingSecurityScopedResource()
    val stream = NSInputStream(file.nsUrl)
    stream.open()
    object : BackupInput {
        override fun read(buffer: ByteArray): Int = buffer.usePinned {
            val count = stream.read(it.addressOf(0).reinterpret(), buffer.size.toULong()).toInt()
            check(count >= 0) { "Could not read backup: ${stream.streamError}" }
            if (count == 0) -1 else count
        }
        override fun close() {
            stream.close()
            if (scoped) file.nsUrl.stopAccessingSecurityScopedResource()
        }
    }
}
