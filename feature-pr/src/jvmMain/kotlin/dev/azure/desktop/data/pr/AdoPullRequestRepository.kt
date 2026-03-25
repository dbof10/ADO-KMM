package dev.azure.desktop.data.pr

import dev.azure.desktop.domain.pr.DevOpsProject
import dev.azure.desktop.domain.pr.PullRequestDetail
import dev.azure.desktop.domain.pr.PullRequestChange
import dev.azure.desktop.domain.pr.PullRequestCheckState
import dev.azure.desktop.domain.pr.PullRequestCheckStatus
import dev.azure.desktop.domain.pr.PullRequestLinkedWorkItem
import dev.azure.desktop.domain.pr.PullRequestRepository
import dev.azure.desktop.domain.pr.PullRequestReviewer
import dev.azure.desktop.domain.pr.PullRequestSummary
import dev.azure.desktop.domain.pr.PullRequestTimelineItem
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import java.time.Instant
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
/** `connectiondata` rejects `7.1` with 400; same version as [dev.azure.desktop.data.auth.AdoRestPatVerifier]. */
private const val ConnectionDataApiVersion = "7.0-preview.1"
private const val Host = "dev.azure.com"

class AdoPullRequestRepository(
    private val httpClient: HttpClient,
    private val patProvider: () -> String?,
) : PullRequestRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun listProjects(organization: String): Result<List<DevOpsProject>> =
        runCatching {
            require(organization.isNotBlank()) { "Missing organization." }
            val all = mutableListOf<DevOpsProject>()
            var continuation: String? = null
            do {
                val url = buildString {
                    append("https://$Host/${organization.encodeURLPathPart()}/_apis/projects")
                    append("?api-version=$ApiVersion")
                    append("&\$top=100")
                    if (!continuation.isNullOrBlank()) {
                        append("&continuationToken=${continuation!!.encodeURLParameter()}")
                    }
                }
                val response = httpClient.get(url) {
                    headers.append(HttpHeaders.Authorization, basicAuth())
                }
                require(response.status.isSuccess()) {
                    "Unable to load projects (${response.status.value})."
                }
                all += parseProjects(response.bodyAsText())
                continuation =
                    response.headers["x-ms-continuationtoken"]
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
            } while (!continuation.isNullOrBlank())
            all.distinctBy { it.id }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        }

    override suspend fun getMyPullRequests(organization: String, projectName: String?): Result<List<PullRequestSummary>> =
        runCatching {
            require(organization.isNotBlank()) { "Missing organization." }
            val creatorId = getAuthenticatedUserId(organization)
            val url =
                gitPullRequestsListUrl(
                    organization = organization,
                    projectName = projectName,
                    searchCriteriaQuery =
                        "searchCriteria.creatorId=${creatorId.encodeURLPathPart()}&searchCriteria.status=active",
                )
            val response = httpClient.get(url) {
                headers.append(HttpHeaders.Authorization, basicAuth())
            }
            require(response.status.isSuccess()) {
                "Unable to load pull requests (${response.status.value})."
            }
            parseSummaryList(response.bodyAsText())
        }

    private suspend fun getAuthenticatedUserId(organization: String): String {
        val url =
            "https://$Host/${organization.encodeURLPathPart()}/_apis/connectiondata" +
                "?api-version=$ConnectionDataApiVersion"
        val response = httpClient.get(url) {
            headers.append(HttpHeaders.Authorization, basicAuth())
        }
        require(response.status.isSuccess()) {
            val hint = response.bodyAsText().take(200)
            "Unable to resolve current user (${response.status.value}). " +
                if (hint.isNotBlank()) hint else "Check token and organization."
        }
        val root = json.parseToJsonElement(response.bodyAsText()).jsonObject
        val authUser = root["authenticatedUser"].asObjectOrNull()
        val id = authUser?.get("id").stringOrNull().orEmpty()
        require(id.isNotBlank()) { "Unable to resolve current user id." }
        return id
    }

    override suspend fun getActivePullRequests(organization: String, projectName: String?): Result<List<PullRequestSummary>> =
        runCatching {
            require(organization.isNotBlank()) { "Missing organization." }
            val url =
                gitPullRequestsListUrl(
                    organization = organization,
                    projectName = projectName,
                    searchCriteriaQuery = "searchCriteria.status=active",
                )
            val response = httpClient.get(url) {
                headers.append(HttpHeaders.Authorization, basicAuth())
            }
            require(response.status.isSuccess()) {
                "Unable to load pull requests (${response.status.value})."
            }
            parseSummaryList(response.bodyAsText())
        }

    override suspend fun getPullRequestSummaryById(
        organization: String,
        projectName: String,
        pullRequestId: Int,
    ): Result<PullRequestSummary> =
        runCatching {
            require(organization.isNotBlank()) { "Missing organization." }
            require(projectName.isNotBlank()) { "Missing project name." }
            require(pullRequestId > 0) { "Invalid pull request id." }
            val url =
                "https://$Host/${organization.encodeURLPathPart()}/${projectName.encodeURLPathPart()}/" +
                    "_apis/git/pullrequests/$pullRequestId?api-version=$ApiVersion"
            val response = httpClient.get(url) {
                headers.append(HttpHeaders.Authorization, basicAuth())
            }
            require(response.status.isSuccess()) {
                "Unable to load pull request #$pullRequestId (${response.status.value})."
            }
            val root = json.parseToJsonElement(response.bodyAsText()).jsonObject
            root.toSummary() ?: error("Pull request payload is invalid.")
        }

    private fun gitPullRequestsListUrl(
        organization: String,
        projectName: String?,
        searchCriteriaQuery: String,
    ): String {
        val path =
            if (projectName.isNullOrBlank()) {
                "https://$Host/${organization.encodeURLPathPart()}/_apis/git/pullrequests"
            } else {
                "https://$Host/${organization.encodeURLPathPart()}/${projectName.encodeURLPathPart()}/_apis/git/pullrequests"
            }
        return "$path?$searchCriteriaQuery&api-version=$ApiVersion"
    }

    private fun parseProjects(raw: String): List<DevOpsProject> {
        val root = json.parseToJsonElement(raw).jsonObject
        return root["value"].asArray().mapNotNull { element ->
            val obj = element.asObjectOrNull() ?: return@mapNotNull null
            val id = obj["id"].stringOrNull() ?: return@mapNotNull null
            val name = obj["name"].stringOrNull() ?: return@mapNotNull null
            DevOpsProject(id = id, name = name)
        }
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
            val prUrl =
                "https://$Host/${organization.encodeURLPathPart()}/${projectName.encodeURLPathPart()}/" +
                    "_apis/git/repositories/${repositoryId.encodeURLPathPart()}/pullRequests/$pullRequestId"

            val detailResponse = httpClient.get("$prUrl?api-version=$ApiVersion") {
                headers.append(HttpHeaders.Authorization, basicAuth())
            }
            require(detailResponse.status.isSuccess()) {
                "Unable to load pull request detail (${detailResponse.status.value})."
            }
            val rawDetail = detailResponse.bodyAsText()

            val threadsResponse = httpClient.get("$prUrl/threads?api-version=$ApiVersion") {
                headers.append(HttpHeaders.Authorization, basicAuth())
            }
            val rawThreads = threadsResponse.takeIf { it.status.isSuccess() }?.bodyAsText()

            val commitsResponse = httpClient.get("$prUrl/commits?api-version=$ApiVersion") {
                headers.append(HttpHeaders.Authorization, basicAuth())
            }
            val rawCommits = commitsResponse.takeIf { it.status.isSuccess() }?.bodyAsText()

            val workItemsResponse = httpClient.get("$prUrl/workitems?api-version=$ApiVersion") {
                headers.append(HttpHeaders.Authorization, basicAuth())
            }
            val rawWorkItems = workItemsResponse.takeIf { it.status.isSuccess() }?.bodyAsText()

            val statusesResponse = httpClient.get("$prUrl/statuses?api-version=$ApiVersion") {
                headers.append(HttpHeaders.Authorization, basicAuth())
            }
            val rawStatuses = statusesResponse.takeIf { it.status.isSuccess() }?.bodyAsText()

            val workItemIds = rawWorkItems?.let(::parseWorkItemIds).orEmpty()
            val rawWorkItemDetails =
                if (workItemIds.isEmpty()) {
                    null
                } else {
                    val ids = workItemIds.distinct().joinToString(",")
                    val fields = listOf("System.Title", "System.WorkItemType", "System.State").joinToString(",")
                    val witUrl =
                        "https://$Host/${organization.encodeURLPathPart()}/${projectName.encodeURLPathPart()}/" +
                            "_apis/wit/workitems?ids=$ids&fields=$fields&api-version=$ApiVersion"
                    val witResponse = httpClient.get(witUrl) {
                        headers.append(HttpHeaders.Authorization, basicAuth())
                    }
                    witResponse.takeIf { it.status.isSuccess() }?.bodyAsText()
                }

            parseDetail(
                raw = rawDetail,
                rawThreads = rawThreads,
                rawCommits = rawCommits,
                rawWorkItems = rawWorkItems,
                rawWorkItemDetails = rawWorkItemDetails,
                rawStatuses = rawStatuses,
            )
        }

    override suspend fun getPullRequestChanges(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
        baseCommitId: String,
        targetCommitId: String,
    ): Result<List<PullRequestChange>> =
        runCatching {
            require(organization.isNotBlank()) { "Missing organization." }
            require(projectName.isNotBlank()) { "Missing project name." }
            require(repositoryId.isNotBlank()) { "Missing repository id." }
            require(baseCommitId.isNotBlank()) { "Missing base commit id." }
            require(targetCommitId.isNotBlank()) { "Missing target commit id." }

            // Commit SHAs require explicit version types; otherwise ADO treats versions as branch names → often 404.
            val diffUrl =
                "https://$Host/${organization.encodeURLPathPart()}/${projectName.encodeURLPathPart()}/" +
                    "_apis/git/repositories/${repositoryId.encodeURLPathPart()}/diffs/commits" +
                    "?baseVersion=${baseCommitId.encodeURLPathPart()}" +
                    "&targetVersion=${targetCommitId.encodeURLPathPart()}" +
                    "&baseVersionType=commit" +
                    "&targetVersionType=commit" +
                    "&api-version=$ApiVersion"

            val diffResponse = httpClient.get(diffUrl) { headers.append(HttpHeaders.Authorization, basicAuth()) }
            val raw =
                when {
                    diffResponse.status.isSuccess() -> diffResponse.bodyAsText()
                    diffResponse.status.value == 404 ->
                        fetchPullRequestIterationChangesJson(
                            organization = organization,
                            projectName = projectName,
                            repositoryId = repositoryId,
                            pullRequestId = pullRequestId,
                        )
                    else ->
                        error("Unable to load pull request changes (${diffResponse.status.value}).")
                }
            parseChanges(raw)
        }

    private suspend fun fetchPullRequestIterationChangesJson(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
    ): String {
        val iterationsUrl =
            "https://$Host/${organization.encodeURLPathPart()}/${projectName.encodeURLPathPart()}/" +
                "_apis/git/repositories/${repositoryId.encodeURLPathPart()}/pullRequests/$pullRequestId/iterations" +
                "?api-version=$ApiVersion"
        val iterationsResponse = httpClient.get(iterationsUrl) {
            headers.append(HttpHeaders.Authorization, basicAuth())
        }
        require(iterationsResponse.status.isSuccess()) {
            "Unable to load pull request iterations (${iterationsResponse.status.value})."
        }
        val iterationsRoot = json.parseToJsonElement(iterationsResponse.bodyAsText()).jsonObject
        val iterationIds =
            iterationsRoot["value"].asArray().mapNotNull { element ->
                element.asObjectOrNull()?.get("id")?.jsonPrimitive?.intOrNull
            }
        val latestIterationId = iterationIds.maxOrNull()
        require(latestIterationId != null) { "No pull request iterations found." }

        val changesUrl =
            "https://$Host/${organization.encodeURLPathPart()}/${projectName.encodeURLPathPart()}/" +
                "_apis/git/repositories/${repositoryId.encodeURLPathPart()}/pullRequests/$pullRequestId/iterations/$latestIterationId/changes" +
                "?api-version=$ApiVersion"
        val changesResponse = httpClient.get(changesUrl) {
            headers.append(HttpHeaders.Authorization, basicAuth())
        }
        require(changesResponse.status.isSuccess()) {
            "Unable to load pull request iteration changes (${changesResponse.status.value})."
        }
        return changesResponse.bodyAsText()
    }

    override suspend fun getFileContentAtCommit(
        organization: String,
        projectName: String,
        repositoryId: String,
        path: String,
        commitId: String,
    ): Result<String?> =
        runCatching {
            require(organization.isNotBlank()) { "Missing organization." }
            require(projectName.isNotBlank()) { "Missing project name." }
            require(repositoryId.isNotBlank()) { "Missing repository id." }
            require(path.isNotBlank()) { "Missing path." }
            require(commitId.isNotBlank()) { "Missing commit id." }

            val baseUrl =
                "https://$Host/${organization.encodeURLPathPart()}/${projectName.encodeURLPathPart()}/" +
                    "_apis/git/repositories/${repositoryId.encodeURLPathPart()}/items" +
                    "?path=${path.encodeURLPathPart()}" +
                    "&resolveLfs=true" +
                    "&versionDescriptor.version=$commitId" +
                    "&versionDescriptor.versionType=commit" +
                    "&api-version=$ApiVersion"

            // ADO intermittently returns 500 when includeContent=true is used on folder paths.
            // We first probe metadata to detect folders, then fetch content only for files.
            val metaResponse: HttpResponse =
                httpClient.get("$baseUrl&includeContentMetadata=true") {
                    headers.append(HttpHeaders.Authorization, basicAuth())
                }
            if (!metaResponse.status.isSuccess()) return@runCatching null

            val metaRaw = metaResponse.bodyAsText()
            val isFolder =
                runCatching {
                    json.parseToJsonElement(metaRaw).jsonObject["isFolder"].jsonBooleanOrNull() == true
                }.getOrDefault(false)
            if (isFolder) return@runCatching null

            val contentResponse: HttpResponse =
                httpClient.get("$baseUrl&includeContent=true") {
                    headers.append(HttpHeaders.Authorization, basicAuth())
                }
            if (!contentResponse.status.isSuccess()) return@runCatching null
            contentResponse.bodyAsText()
        }

    override suspend fun setMyPullRequestVote(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
        vote: Int,
    ): Result<Unit> =
        runCatching {
            require(organization.isNotBlank()) { "Missing organization." }
            require(projectName.isNotBlank()) { "Missing project name." }
            require(repositoryId.isNotBlank()) { "Missing repository id." }
            require(pullRequestId > 0) { "Invalid pull request id." }

            val reviewerId = getAuthenticatedUserId(organization)
            val url =
                "https://$Host/${organization.encodeURLPathPart()}/${projectName.encodeURLPathPart()}/" +
                    "_apis/git/repositories/${repositoryId.encodeURLPathPart()}/pullRequests/$pullRequestId/" +
                    "reviewers/${reviewerId.encodeURLPathPart()}?api-version=$ApiVersion"

            val response =
                httpClient.request(url) {
                    method = HttpMethod.Put
                    headers.append(HttpHeaders.Authorization, basicAuth())
                    body = TextContent("""{"vote":$vote}""", ContentType.Application.Json)
                }

            require(response.status.isSuccess()) {
                "Unable to update vote (${response.status.value})."
            }
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

    private fun parseDetail(
        raw: String,
        rawThreads: String?,
        rawCommits: String?,
        rawWorkItems: String?,
        rawWorkItemDetails: String?,
        rawStatuses: String?,
    ): PullRequestDetail {
        val root = json.parseToJsonElement(raw).jsonObject
        val summary = root.toSummary() ?: error("Pull request payload is invalid.")
        val reviewers = root["reviewers"].asArray().mapNotNull { it.asObjectOrNull()?.toReviewer() }
        val timeline =
            buildList {
                rawThreads?.let { addAll(parseThreadComments(it)) }
                rawCommits?.let { addAll(parseCommits(it)) }
                addAll(
                    reviewers
                        .filter { it.vote >= 10 }
                        .map {
                            PullRequestTimelineItem.Approval(
                                actorDisplayName = it.displayName,
                                createdDateIso = null,
                                vote = it.vote,
                            )
                        },
                )
            }.sortedByDescending { it.createdDateIso.asInstantOrNullOrEpoch() }

        val linkedWorkItems = parseWorkItems(rawWorkItems = rawWorkItems, rawWorkItemDetails = rawWorkItemDetails)
        val checks = parseCheckStatuses(rawStatuses)

        return PullRequestDetail(
            summary = summary,
            description = root["description"].stringOrNull()?.takeIf { it.isNotBlank() },
            reviewers = reviewers,
            timeline = timeline,
            linkedWorkItems = linkedWorkItems,
            checks = checks,
            lastMergeSourceCommitId = root["lastMergeSourceCommit"].asObjectOrNull()?.get("commitId").stringOrNull(),
            lastMergeTargetCommitId = root["lastMergeTargetCommit"].asObjectOrNull()?.get("commitId").stringOrNull(),
        )
    }

    private fun parseWorkItemIds(raw: String): List<Int> {
        val root = json.parseToJsonElement(raw).jsonObject
        val items = root["value"].asArray()
        return items.mapNotNull { it.asObjectOrNull()?.get("id")?.intOrNull() }
    }

    private fun parseWorkItems(
        rawWorkItems: String?,
        rawWorkItemDetails: String?,
    ): List<PullRequestLinkedWorkItem> {
        val ids = rawWorkItems?.let(::parseWorkItemIds).orEmpty()
        if (ids.isEmpty()) return emptyList()

        val detailsById: Map<Int, PullRequestLinkedWorkItem> =
            rawWorkItemDetails?.let { raw ->
                val root = json.parseToJsonElement(raw).jsonObject
                val value = root["value"].asArray()
                value.mapNotNull { el ->
                    val obj = el.asObjectOrNull() ?: return@mapNotNull null
                    val id = obj["id"].intOrNull() ?: return@mapNotNull null
                    val fields = obj["fields"].asObjectOrNull()
                    PullRequestLinkedWorkItem(
                        id = id,
                        title = fields?.get("System.Title").stringOrNull(),
                        type = fields?.get("System.WorkItemType").stringOrNull(),
                        state = fields?.get("System.State").stringOrNull(),
                    )
                }.associateBy { it.id }
            } ?: emptyMap()

        return ids.distinct().map { id ->
            detailsById[id] ?: PullRequestLinkedWorkItem(id = id, title = null, type = null, state = null)
        }
    }

    private fun parseCheckStatuses(raw: String?): List<PullRequestCheckStatus> {
        if (raw.isNullOrBlank()) return emptyList()
        val root = json.parseToJsonElement(raw).jsonObject
        val value = root["value"].asArray()
        return value.mapNotNull { el ->
            val obj = el.asObjectOrNull() ?: return@mapNotNull null
            val context = obj["context"].asObjectOrNull()
            val name = context?.get("name").stringOrNull()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val stateRaw = obj["state"].stringOrNull().orEmpty()
            val state =
                when (stateRaw.lowercase()) {
                    "succeeded" -> PullRequestCheckState.Succeeded
                    "failed", "error" -> PullRequestCheckState.Failed
                    "pending" -> PullRequestCheckState.Pending
                    "notapplicable", "not_applicable" -> PullRequestCheckState.NotApplicable
                    "inprogress", "in_progress", "running" -> PullRequestCheckState.Running
                    else -> PullRequestCheckState.Unknown
                }
            PullRequestCheckStatus(
                name = name,
                state = state,
                description = obj["description"].stringOrNull(),
            )
        }
    }

    private fun parseThreadComments(raw: String): List<PullRequestTimelineItem.Comment> {
        val root = json.parseToJsonElement(raw).jsonObject
        val threads = root["value"].asArray()
        return threads.flatMap { thread ->
            val threadObj = thread.asObjectOrNull() ?: return@flatMap emptyList()
            val comments = threadObj["comments"].asArray()
            comments.mapNotNull { comment ->
                val commentObj = comment.asObjectOrNull() ?: return@mapNotNull null
                val content = commentObj["content"].stringOrNull()?.trim().orEmpty()
                if (content.isBlank()) return@mapNotNull null
                val author = commentObj["author"].asObjectOrNull()
                PullRequestTimelineItem.Comment(
                    actorDisplayName = author?.get("displayName").stringOrNull().orEmpty(),
                    createdDateIso =
                        commentObj["publishedDate"].stringOrNull()
                            ?: commentObj["lastUpdatedDate"].stringOrNull(),
                    content = content,
                )
            }
        }
    }

    private fun parseCommits(raw: String): List<PullRequestTimelineItem.Commit> {
        val root = json.parseToJsonElement(raw).jsonObject
        val commits = root["value"].asArray()
        return commits.mapNotNull { commit ->
            val obj = commit.asObjectOrNull() ?: return@mapNotNull null
            val commitId = obj["commitId"].stringOrNull()?.take(7) ?: return@mapNotNull null
            val author = obj["author"].asObjectOrNull()
            PullRequestTimelineItem.Commit(
                commitId = commitId,
                message = obj["comment"].stringOrNull().orEmpty(),
                createdDateIso = author?.get("date").stringOrNull(),
                actorDisplayName = author?.get("name").stringOrNull().orEmpty(),
            )
        }
    }

    private fun parseChanges(raw: String): List<PullRequestChange> {
        val root = json.parseToJsonElement(raw).jsonObject
        // diffs/commits uses "changes"; pull request iteration changes uses "changeEntries"
        val changes = (root["changes"] ?: root["changeEntries"]).asArray()
        return changes.mapNotNull { element ->
            val obj = element.asObjectOrNull() ?: return@mapNotNull null
            val item = obj["item"].asObjectOrNull()
            val path = item?.get("path").stringOrNull() ?: return@mapNotNull null
            val changeType = obj["changeType"].stringOrNull().orEmpty()
            val isFolder = item?.get("isFolder").jsonBooleanOrNull() == true
            PullRequestChange(path = path, changeType = changeType, isFolder = isFolder)
        }
    }
}

private fun String?.asInstantOrNullOrEpoch(): Instant =
    try {
        if (this.isNullOrBlank()) Instant.EPOCH else Instant.parse(this)
    } catch (_: Throwable) {
        Instant.EPOCH
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

private fun JsonElement?.jsonBooleanOrNull(): Boolean? {
    val p = this as? JsonPrimitive ?: return null
    return when (p.contentOrNull?.lowercase()) {
        "true" -> true
        "false" -> false
        else -> null
    }
}
