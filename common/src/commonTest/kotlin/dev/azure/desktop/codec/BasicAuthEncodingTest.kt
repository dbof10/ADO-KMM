package dev.azure.desktop.codec

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BasicAuthEncodingTest {
    @Test
    fun buildsExpectedHeader() {
        val header = basicAuthHeader("abc123")

        assertEquals("Basic OmFiYzEyMw==", header)
    }

    @Test
    fun trimsPatBeforeEncoding() {
        val header = basicAuthHeader("  abc  ")

        assertEquals("Basic OmFiYw==", header)
    }

    @Test
    fun rejectsBlankPat() {
        val error = assertFailsWith<IllegalArgumentException> { basicAuthHeader("   ") }

        assertEquals("Session token is missing. Please sign in again.", error.message)
    }
}
