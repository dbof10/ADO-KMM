package dev.azure.desktop

import kotlin.test.Test
import kotlin.test.assertTrue

class WelcomeTest {
    @Test
    fun messageIsNonEmpty() {
        assertTrue(Welcome.message().isNotEmpty())
    }
}
