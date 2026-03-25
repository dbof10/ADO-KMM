package dev.azure.desktop.data.auth

import dev.azure.desktop.domain.auth.PatVerifier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.http.encodeURLPathPart
import java.util.Base64

private const val ConnectionDataPathSuffix = "/_apis/connectiondata"
private const val ExpectedHost = "dev.azure.com"

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
            val url = "https://$ExpectedHost$expectedPath?api-version=7.0"

            val response = httpClient.get(url) {
                headers.append(HttpHeaders.Authorization, "Basic $token")
            }
            val body = response.bodyAsText()
            val mediaType = response.headers[HttpHeaders.ContentType].orEmpty()
            val isJsonResponse = mediaType.startsWith(ContentType.Application.Json.toString())
            val endedAtExpectedApi =
                response.call.request.url.host.equals(ExpectedHost, ignoreCase = true) &&
                    response.call.request.url.encodedPath == expectedPath
            val accepted =
                response.status.isSuccess() &&
                    isJsonResponse &&
                    endedAtExpectedApi

            if (!accepted) {
                val hint = body.take(200)
                error(
                    "Azure DevOps did not accept this token (${response.status.value}). " +
                        if (hint.isNotBlank()) hint else "Check organization, token, and permissions.",
                )
            }
        }
}
