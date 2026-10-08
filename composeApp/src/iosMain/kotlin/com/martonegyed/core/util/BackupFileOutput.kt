package com.martonegyed.core.util

import com.martonegyed.domain.repository.BackupOutput
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.Foundation.NSOutputStream

@OptIn(ExperimentalForeignApi::class)
actual fun backupFileOutput(file: PlatformFile): BackupOutput {
    val scoped = file.nsUrl.startAccessingSecurityScopedResource()
    val stream = NSOutputStream(file.nsUrl, append = false)
    stream.open()
    return object : BackupOutput {
        override fun write(bytes: ByteArray) {
            var offset = 0
            while (offset < bytes.size) {
                val written = bytes.usePinned {
                    stream.write(it.addressOf(offset).reinterpret(), (bytes.size - offset).toULong()).toInt()
                }
                check(written > 0) { "Could not write backup: ${stream.streamError}" }
                offset += written
            }
        }

        override fun close() {
            stream.close()
            if (scoped) file.nsUrl.stopAccessingSecurityScopedResource()
        }
    }
}
