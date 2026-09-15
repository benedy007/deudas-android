package com.benedy.deudas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.benedy.deudas.ui.util.ClientPhotoHelper
import java.io.File

@Composable
fun ClientAvatar(
    photoPath: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    placeholderBg: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val context = LocalContext.current
    val file: File? = ClientPhotoHelper.resolveFile(context, photoPath)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(placeholderBg),
        contentAlignment = Alignment.Center
    ) {
        if (file != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(file)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        } else {
            Icon(
                Icons.Outlined.Person,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}
