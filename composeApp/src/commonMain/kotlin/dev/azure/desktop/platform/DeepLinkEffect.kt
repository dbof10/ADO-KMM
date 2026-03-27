package dev.azure.desktop.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import dev.azure.desktop.deeplink.AppDeepLinkBus

/** Invokes [onUrl] when the platform delivers a deep link (desktop, Android, or iOS). */
@Composable
fun DeepLinkEffect(onUrl: (String) -> Unit) {
    LaunchedEffect(Unit) {
        AppDeepLinkBus.urls.collect { onUrl(it) }
    }
}
