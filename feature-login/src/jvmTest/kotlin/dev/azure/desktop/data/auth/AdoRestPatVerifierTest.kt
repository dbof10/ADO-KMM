package dev.azure.desktop.data.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdoRestPatVerifierTest {
    @Test
    fun verifySucceedsForValidJsonConnectionDataResponse() =
        runBlocking {
            val client =
                HttpClient(
                    MockEngine { request ->
                        assertEquals("dev.azure.com", request.url.host)
                        respond(
                            content = """{"authenticatedUser":{}}""",
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    },
                )
            val verifier = AdoRestPatVerifier(client)
            val result = verifier.verify("fabrikam", "pat")
            assertTrue(result.isSuccess)
        }

    @Test
    fun verifyFailsWhenResponseIsNotJson() =
        runBlocking {
            val client =
                HttpClient(
                    MockEngine {
                        respond(
                            content = "plain",
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "text/plain"),
                        )
                    },
                )
            val verifier = AdoRestPatVerifier(client)
            val result = verifier.verify("fabrikam", "pat")
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("Azure DevOps did not accept") == true)
        }

    @Test
    fun verifyFailsForBlankOrganizationBeforeNetwork() =
        runBlocking {
            val client = HttpClient(MockEngine { respond("", HttpStatusCode.OK) })
            val verifier = AdoRestPatVerifier(client)
            val result = verifier.verify("   ", "pat")
            assertTrue(result.isFailure)
            assertEquals("Enter an Azure DevOps organization.", result.exceptionOrNull()?.message)
        }

    @Test
    fun verifyFailsWhenOrganizationContainsSlash() =
        runBlocking {
            val client = HttpClient(MockEngine { respond("", HttpStatusCode.OK) })
            val verifier = AdoRestPatVerifier(client)
            val result = verifier.verify("bad/org", "pat")
            assertTrue(result.isFailure)
            assertEquals("Organization name is invalid.", result.exceptionOrNull()?.message)
        }
}
