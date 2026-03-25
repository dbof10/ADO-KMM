package dev.azure.desktop.data.pr

import dev.azure.desktop.domain.pr.PullRequestDetail
import dev.azure.desktop.domain.pr.PullRequestRepository
import dev.azure.desktop.domain.pr.PullRequestReviewer
import dev.azure.desktop.domain.pr.PullRequestSummary
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val ApiVersion = "7.1"
private const val Host = "dev.azure.com"

class AdoPullRequestRepository(
    private val httpClient: HttpClient,
    private val patProvider: () -> String?,
) : PullRequestRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getMyPullRequests(organization: String): Result<List<PullRequestSummary>> =
        runCatching {
            require(organization.isNotBlank()) { "Missing organization." }
            val url =
                "https://$Host/${organization.encodeURLPathPart()}/_apis/git/pullrequests" +
                    "?searchCriteria.reviewerId=me" +
                    "&searchCriteria.status=active" +
                    "&api-version=$ApiVersion"
            val response = httpClient.get(url) {
                headers.append(HttpHeaders.Authorization, basicAuth())
            }
            require(response.status.isSuccess()) {
                "Unable to load pull requests (${response.status.value})."
            }
            parseSummaryList(response.bodyAsText())
        }

    override suspend fun getPullRequestDetail(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
    ): Result<PullRequestDetail> =
        runCatching {
            require(organization.isNotBlank()) { "Missing organization." }
            require(projectName.isNotBlank()) { "Missing project name." }
            require(repositoryId.isNotBlank()) { "Missing repository id." }
            val url =
                "https://$Host/${organization.encodeURLPathPart()}/${projectName.encodeURLPathPart()}/" +
                    "_apis/git/repositories/${repositoryId.encodeURLPathPart()}/pullRequests/$pullRequestId" +
                    "?api-version=$ApiVersion"
            val response = httpClient.get(url) {
                headers.append(HttpHeaders.Authorization, basicAuth())
            }
            require(response.status.isSuccess()) {
                "Unable to load pull request detail (${response.status.value})."
            }
            parseDetail(response.bodyAsText())
        }

    private fun basicAuth(): String {
        val pat = patProvider()?.trim().orEmpty()
        require(pat.isNotBlank()) { "Session token is missing. Please sign in again." }
        val token = Base64.getEncoder().encodeToString(":$pat".toByteArray(Charsets.UTF_8))
        return "Basic $token"
    }

    private fun parseSummaryList(raw: String): List<PullRequestSummary> {
        val root = json.parseToJsonElement(raw).jsonObject
        val items = root["value"].asArray()
        return items.mapNotNull { it.asObjectOrNull()?.toSummary() }
    }

    private fun parseDetail(raw: String): PullRequestDetail {
        val root = json.parseToJsonElement(raw).jsonObject
        val summary = root.toSummary() ?: error("Pull request payload is invalid.")
        val reviewers = root["reviewers"].asArray().mapNotNull { it.asObjectOrNull()?.toReviewer() }
        return PullRequestDetail(
            summary = summary,
            description = root["description"].stringOrNull()?.takeIf { it.isNotBlank() },
            reviewers = reviewers,
        )
    }
}

private fun JsonObject.toSummary(): PullRequestSummary? {
    val repository = this["repository"].asObjectOrNull() ?: return null
    val project = repository["project"].asObjectOrNull() ?: return null
    val createdBy = this["createdBy"].asObjectOrNull()
    val id = this["pullRequestId"].intOrNull() ?: return null
    return PullRequestSummary(
        id = id,
        title = this["title"].stringOrNull().orEmpty(),
        status = this["status"].stringOrNull().orEmpty(),
        creatorDisplayName = createdBy?.get("displayName").stringOrNull().orEmpty(),
        creatorUniqueName = createdBy?.get("uniqueName").stringOrNull(),
        sourceRefName = this["sourceRefName"].stringOrNull().orEmpty(),
        targetRefName = this["targetRefName"].stringOrNull().orEmpty(),
        repositoryName = repository["name"].stringOrNull().orEmpty(),
        repositoryId = repository["id"].stringOrNull().orEmpty(),
        projectName = project["name"].stringOrNull().orEmpty(),
        projectId = project["id"].stringOrNull().orEmpty(),
        creationDateIso = this["creationDate"].stringOrNull(),
    )
}

private fun JsonObject.toReviewer(): PullRequestReviewer? {
    val displayName = this["displayName"].stringOrNull() ?: return null
    return PullRequestReviewer(
        displayName = displayName,
        uniqueName = this["uniqueName"].stringOrNull(),
        vote = this["vote"].intOrNull() ?: 0,
    )
}

private fun JsonElement?.asObjectOrNull(): JsonObject? = this as? JsonObject

private fun JsonElement?.asArray(): JsonArray = (this as? JsonArray) ?: JsonArray(emptyList())

private fun JsonElement?.stringOrNull(): String? = (this as? JsonPrimitive)?.contentOrNull

private fun JsonElement?.intOrNull(): Int? = (this as? JsonPrimitive)?.intOrNull
