package com.martonegyed.core.util

import io.github.vinceglb.filekit.core.PlatformFile

expect suspend fun writePickedFile(file: PlatformFile, bytes: ByteArray)