package dev.azure.desktop.data.pr

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import dev.azure.desktop.domain.pr.PullRequestMergeStrategy
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdoPullRequestRepositoryTest {
    @Test
    fun listProjectsParsesFirstPage() =
        runBlocking {
            val body =
                """{"value":[{"id":"11111111-1111-1111-1111-111111111111","name":"Fabrikam"}]}"""
            val client =
                HttpClient(
                    MockEngine { request ->
                        assertEquals("dev.azure.com", request.url.host)
                        assertTrue(request.url.encodedPath.contains("_apis/projects"))
                        respond(
                            content = body,
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    },
                )
            val repo = AdoPullRequestRepository(client) { "pat" }
            val projects = repo.listProjects("fabrikam").getOrThrow()
            assertEquals(1, projects.size)
            assertEquals("Fabrikam", projects.single().name)
        }

    @Test
    fun getActivePullRequestsParsesSummaries() =
        runBlocking {
            val body =
                """
                {"value":[{
                  "pullRequestId": 3,
                  "title": "Fix bug",
                  "status": "active",
                  "createdBy": { "displayName": "Alex", "uniqueName": "alex@fabrikam" },
                  "sourceRefName": "refs/heads/f",
                  "targetRefName": "refs/heads/main",
                  "repository": {
                    "id": "repo-guid",
                    "name": "app",
                    "project": { "id": "proj-guid", "name": "Proj" }
                  },
                  "creationDate": "2024-06-01T12:00:00Z"
                }]}
                """.trimIndent()
            val client =
                HttpClient(
                    MockEngine { request ->
                        assertTrue(request.url.encodedPath.contains("_apis/git/pullrequests"))
                        assertTrue(request.url.encodedQuery.contains("searchCriteria.status=active"))
                        respond(
                            content = body,
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    },
                )
            val repo = AdoPullRequestRepository(client) { "pat" }
            val prs = repo.getActivePullRequests("fabrikam", "Proj").getOrThrow()
            assertEquals(3, prs.single().id)
            assertEquals("Fix bug", prs.single().title)
            assertEquals("app", prs.single().repositoryName)
        }

    @Test
    fun getMyPullRequestsCallsConnectionDataThenPullRequests() =
        runBlocking {
            val connection =
                """{"authenticatedUser":{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"}}"""
            val listBody =
                """
                {"value":[{
                  "pullRequestId": 9,
                  "title": "Mine",
                  "status": "active",
                  "createdBy": { "displayName": "Alex", "uniqueName": "alex@fabrikam" },
                  "sourceRefName": "refs/heads/a",
                  "targetRefName": "refs/heads/main",
                  "repository": {
                    "id": "r",
                    "name": "app",
                    "project": { "id": "p", "name": "Proj" }
                  },
                  "creationDate": null
                }]}
                """.trimIndent()
            var sawConnection = false
            val client =
                HttpClient(
                    MockEngine { request ->
                        when {
                            request.url.encodedPath.contains("connectiondata") -> {
                                sawConnection = true
                                respond(
                                    connection,
                                    HttpStatusCode.OK,
                                    headersOf(HttpHeaders.ContentType, "application/json"),
                                )
                            }
                            request.url.encodedPath.contains("_apis/git/pullrequests") -> {
                                assertTrue(sawConnection)
                                assertTrue(
                                    request.url.encodedQuery.contains("searchCriteria.creatorId="),
                                )
                                respond(
                                    listBody,
                                    HttpStatusCode.OK,
                                    headersOf(HttpHeaders.ContentType, "application/json"),
                                )
                            }
                            else -> respond("unexpected", HttpStatusCode.NotFound)
                        }
                    },
                )
            val repo = AdoPullRequestRepository(client) { "pat" }
            val prs = repo.getMyPullRequests("fabrikam", null).getOrThrow()
            assertEquals(9, prs.single().id)
        }

    @Test
    fun enableAutoCompleteCallsConnectionDataThenPatchesAutoCompleteFields() =
        runBlocking {
            val connection =
                """{"authenticatedUser":{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"}}"""
            var seenBody: String? = null
            val client =
                HttpClient(
                    MockEngine { request ->
                        when {
                            request.url.encodedPath.contains("connectiondata") -> {
                                respond(
                                    connection,
                                    HttpStatusCode.OK,
                                    headersOf(HttpHeaders.ContentType, "application/json"),
                                )
                            }
                            request.method == HttpMethod.Patch &&
                                request.url.encodedPath.contains("/pullRequests/55") -> {
                                seenBody = (request.body as TextContent).text
                                respond(
                                    "{}",
                                    HttpStatusCode.OK,
                                    headersOf(HttpHeaders.ContentType, "application/json"),
                                )
                            }
                            else -> respond("unexpected", HttpStatusCode.NotFound)
                        }
                    },
                )
            val repo = AdoPullRequestRepository(client) { "pat" }
            repo.enableAutoComplete(
                organization = "fabrikam",
                projectName = "Proj",
                repositoryId = "repo-id",
                pullRequestId = 55,
                mergeStrategy = PullRequestMergeStrategy.Squash,
            ).getOrThrow()
            assertTrue(
                seenBody!!.contains("\"autoCompleteSetBy\":{\"id\":\"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\"}") &&
                    seenBody!!.contains("\"completionOptions\":{\"mergeStrategy\":\"squash\"}"),
            )
        }
}
