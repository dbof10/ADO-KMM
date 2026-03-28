package dev.azure.desktop.data.release

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

class AdoReleaseRepositoryTest {
    @Test
    fun listReleaseDefinitionsParsesDefinitions() =
        runBlocking {
            val body =
                """
                {"value":[{
                  "id": 12,
                  "name": "Deploy app",
                  "environments": [
                    { "name": "Production", "rank": 2 },
                    { "name": "Staging", "rank": 1 }
                  ]
                }]}
                """.trimIndent()
            val client =
                HttpClient(
                    MockEngine { request ->
                        assertEquals("vsrm.dev.azure.com", request.url.host)
                        assertTrue(request.url.encodedPath.contains("_apis/release/definitions"))
                        respond(
                            body,
                            HttpStatusCode.OK,
                            headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    },
                )
            val repo = AdoReleaseRepository(client) { "pat" }
            val defs = repo.listReleaseDefinitions("fab", "Proj").getOrThrow()
            assertEquals(12, defs.single().id)
            assertEquals("Deploy app", defs.single().name)
            assertEquals("Production", defs.single().subtitle)
        }

    @Test
    fun getReleaseParsesCoreFields() =
        runBlocking {
            val body =
                """
                {
                  "id": 55,
                  "name": "Release-55",
                  "createdOn": "2024-01-02T00:00:00Z",
                  "releaseDefinition": { "id": 1, "name": "CD" },
                  "environments": [
                    {
                      "id": 900,
                      "name": "Dev",
                      "rank": 1,
                      "status": "succeeded",
                      "deploySteps": []
                    }
                  ],
                  "artifacts": [],
                  "variables": {}
                }
                """.trimIndent()
            val client =
                HttpClient(
                    MockEngine { request ->
                        assertTrue(request.url.encodedPath.contains("_apis/release/releases/55"))
                        respond(
                            body,
                            HttpStatusCode.OK,
                            headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    },
                )
            val repo = AdoReleaseRepository(client) { "pat" }
            val detail = repo.getRelease("fab", "Proj", 55).getOrThrow()
            assertEquals(55, detail.id)
            assertEquals("Release-55", detail.name)
            assertEquals("CD", detail.definitionName)
            assertEquals("Proj", detail.projectName)
            assertEquals(900, detail.environments.single().id)
        }
}
