package com.martonegyed.core.util

import com.martonegyed.domain.repository.BackupSource
import io.github.vinceglb.filekit.core.PlatformFile

expect fun backupFileSource(file: PlatformFile): BackupSource
