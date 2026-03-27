package dev.azure.desktop

import dev.azure.desktop.deeplink.AppDeepLinkBus

/** Called from Swift via `onOpenURL`; forwards into [AppDeepLinkBus]. */
@Suppress("unused")
fun handleIncomingIosUrl(urlString: String) {
    AppDeepLinkBus.emit(urlString)
}
