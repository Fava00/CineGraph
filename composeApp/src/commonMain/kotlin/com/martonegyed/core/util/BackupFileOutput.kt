package com.martonegyed.core.util

import com.martonegyed.domain.repository.BackupOutput
import io.github.vinceglb.filekit.core.PlatformFile

expect fun backupFileOutput(file: PlatformFile): BackupOutput
