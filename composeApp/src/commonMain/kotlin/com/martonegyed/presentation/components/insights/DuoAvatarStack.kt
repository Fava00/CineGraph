package com.martonegyed.presentation.components.insights

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.martonegyed.presentation.components.common.PersonAvatar

@Composable
fun DuoAvatarStack(
    leftName: String,
    rightName: String,
    leftPhotoPath: String?,
    rightPhotoPath: String?
) {
    Box(
        modifier = Modifier.width(76.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        PersonAvatar(
            name = leftName,
            photoPath = leftPhotoPath,
            modifier = Modifier.offset(x = 0.dp),
            borderColor = MaterialTheme.colorScheme.primary
        )
        PersonAvatar(
            name = rightName,
            photoPath = rightPhotoPath,
            modifier = Modifier.offset(x = 28.dp),
            borderColor = MaterialTheme.colorScheme.tertiary
        )
    }
}