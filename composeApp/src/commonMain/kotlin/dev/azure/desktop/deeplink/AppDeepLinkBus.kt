package dev.azure.desktop.deeplink

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Cross-platform entry for deep links. [emit] accepts:
 * - A full `https://dev.azure.com/...` PR URL
 * - An iOS/Android wrapper: `adodesktop://open?url=<percent-encoded-ado-url>` (or `link=`).
 *
 * Extraneous links are filtered out by [parseAzureDevOpsPullRequestUrl] in [App].
 */
object AppDeepLinkBus {
    private val _urls =
        MutableSharedFlow<String>(
            extraBufferCapacity = 32,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val urls = _urls.asSharedFlow()

    fun emit(raw: String) {
        val normalized = normalizeDeepLinkInput(raw.trim())
        if (normalized.isNotEmpty()) {
            _urls.tryEmit(normalized)
        }
    }
}

/**
 * Unwraps `adodesktop://open?url=...` / `link=...` into the inner Azure DevOps URL.
 */
fun normalizeDeepLinkInput(raw: String): String {
    val trimmed = raw.trim()
    if (!trimmed.substringBefore(':').equals("adodesktop", ignoreCase = true)) {
        return trimmed
    }
    val qStart = trimmed.indexOf('?')
    if (qStart < 0) return trimmed
    val query = trimmed.substring(qStart + 1)
    for (pair in query.split('&')) {
        val eq = pair.indexOf('=')
        if (eq <= 0) continue
        val key = pair.substring(0, eq)
        val value = pair.substring(eq + 1)
        if (key.equals("url", ignoreCase = true) || key.equals("link", ignoreCase = true)) {
            return percentDecode(value)
        }
    }
    return trimmed
}
