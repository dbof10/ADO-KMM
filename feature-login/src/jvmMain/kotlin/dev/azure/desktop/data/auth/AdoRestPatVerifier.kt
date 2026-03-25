package dev.azure.desktop.data.auth

import dev.azure.desktop.domain.auth.PatVerifier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import java.util.Base64

private const val ConnectionDataPathSuffix = "/_apis/connectiondata"
private const val ExpectedHost = "dev.azure.com"
private const val ConnectionDataApiVersion = "7.0-preview.1"

class AdoRestPatVerifier(
    private val httpClient: HttpClient,
) : PatVerifier {
    override suspend fun verify(organization: String, pat: String): Result<Unit> =
        runCatching {
            val org = organization.trim()
            require(org.isNotEmpty()) { "Enter an Azure DevOps organization." }
            require('/' !in org) { "Organization name is invalid." }

            val token = Base64.getEncoder().encodeToString(":$pat".toByteArray(Charsets.UTF_8))
            val expectedPath = "/${org.encodeURLPathPart()}$ConnectionDataPathSuffix"
            val url = "https://$ExpectedHost$expectedPath?api-version=$ConnectionDataApiVersion"

            val response = httpClient.get(url) {
                headers.append(HttpHeaders.Authorization, "Basic $token")
            }
            val body = response.bodyAsText()
            val mediaType = response.headers[HttpHeaders.ContentType].orEmpty()
            val isJsonResponse = mediaType.startsWith(ContentType.Application.Json.toString())
            val finalPathSegments =
                response.call.request.url.encodedPath
                    .trim('/')
                    .split('/')
                    .filter { it.isNotBlank() }
            val endedAtConnectionDataApi =
                finalPathSegments.lastOrNull().equals("connectiondata", ignoreCase = true)
            val actualOrg = finalPathSegments.firstOrNull().orEmpty()
            val endedAtExpectedApi =
                response.call.request.url.host.equals(ExpectedHost, ignoreCase = true) &&
                    actualOrg.equals(org, ignoreCase = true) &&
                    endedAtConnectionDataApi
            val accepted = response.status.isSuccess() && isJsonResponse && endedAtExpectedApi

            if (!accepted) {
                val hint = body.take(200)
                error(
                    "Azure DevOps did not accept this token (${response.status.value}). " +
                        if (hint.isNotBlank()) hint else "Check organization, token, and permissions.",
                )
            }
        }
}
