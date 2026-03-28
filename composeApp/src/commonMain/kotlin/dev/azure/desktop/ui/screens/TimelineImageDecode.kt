package dev.azure.desktop.ui.screens

import androidx.compose.ui.graphics.ImageBitmap

internal expect fun ByteArray.decodeTimelineImageOrNull(): ImageBitmap?
