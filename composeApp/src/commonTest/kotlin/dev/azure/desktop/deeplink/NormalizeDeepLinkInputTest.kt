package dev.azure.desktop.deeplink

import kotlin.test.Test
import kotlin.test.assertEquals

class NormalizeDeepLinkInputTest {
    @Test
    fun leavesPlainHttpsUntouched() {
        val raw = "  https://dev.azure.com/o/p/_git/r/pullrequest/1  "
        assertEquals(raw.trim(), normalizeDeepLinkInput(raw))
    }

    @Test
    fun unwrapsUrlQueryParameter() {
        val inner = "https://dev.azure.com/o/p/_git/r/pullrequest/2"
        val wrapped = "adodesktop://open?url=" + inner.replace(":", "%3A").replace("/", "%2F")
        assertEquals(inner, normalizeDeepLinkInput(wrapped))
    }

    @Test
    fun unwrapsLinkAlias() {
        val inner = "https://dev.azure.com/o/p/_git/r/pullrequest/3"
        val encoded = inner.replace(":", "%3A").replace("/", "%2F")
        assertEquals(inner, normalizeDeepLinkInput("adodesktop://route?link=$encoded"))
    }

    @Test
    fun returnsFullSchemeWhenNoRecognizedQueryKey() {
        val s = "adodesktop://open?foo=bar"
        assertEquals(s, normalizeDeepLinkInput(s))
    }
}
