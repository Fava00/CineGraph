package com.martonegyed.domain.repository

fun interface BackupSource {
    fun open(): BackupInput
}

interface BackupInput {
    fun read(buffer: ByteArray): Int
    fun close()
}

interface BackupOutput {
    fun write(bytes: ByteArray)
    fun close()
}
