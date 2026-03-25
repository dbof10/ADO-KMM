package dev.azure.desktop.data.release

import dev.azure.desktop.domain.release.CreateReleaseParams
import dev.azure.desktop.domain.release.CreatedRelease
import dev.azure.desktop.domain.release.ReleaseArtifactInfo
import dev.azure.desktop.domain.release.ReleaseDefinitionDetail
import dev.azure.desktop.domain.release.ReleaseDefinitionEnvironmentStage
import dev.azure.desktop.domain.release.ReleaseDefinitionSummary
import dev.azure.desktop.domain.release.ReleaseDeploymentStatus
import dev.azure.desktop.domain.release.ReleaseDetail
import dev.azure.desktop.domain.release.ReleaseEnvironmentInfo
import dev.azure.desktop.domain.release.ReleaseRepository
import dev.azure.desktop.domain.release.ReleaseStagePill
import dev.azure.desktop.domain.release.ReleaseSummary
import dev.azure.desktop.domain.release.ReleaseTimelineEntry
import dev.azure.desktop.domain.release.ReleaseVariableRow
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Fetches release definitions and releases from Azure DevOps **Release** REST APIs on
 * [vsrm.dev.azure.com](https://learn.microsoft.com/en-us/rest/api/azure/devops/release/).
 * No fixtures or offline mock data — all rows are parsed from HTTP responses.
 */
private const val ApiVersion = "7.1"
private const val VsrmHost = "vsrm.dev.azure.com"
private const val PageTop = 100
private const val MaxContinuationPages = 50

class AdoReleaseRepository(
    private val httpClient: HttpClient,
    private val patProvider: () -> String?,
) : ReleaseRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun listReleaseDefinitions(
        organization: String,
        projectName: String,
    ): Result<List<ReleaseDefinitionSummary>> =
        runCatching {
            require(organization.isNotBlank()) { "Missing organization." }
            require(projectName.isNotBlank()) { "Missing project." }
            val org = organization.trim()
            val proj = projectName.trim()
            val accumulator = mutableListOf<ReleaseDefinitionSummary>()
            var continuation: String? = null
            var pages = 0
            do {
                val url =
                    buildString {
                        append("https://")
                        append(VsrmHost)
                        append("/")
                        append(org.encodeURLPathPart())
                        append("/")
                        append(proj.encodeURLPathPart())
                        append("/_apis/release/definitions")
                        append("?api-version=")
                        append(ApiVersion)
                        append("&${'$'}top=$PageTop")
                        append("&${'$'}expand=environments")
                        if (!continuation.isNullOrBlank()) {
                            append("&continuationToken=${continuation!!.encodeURLParameter()}")
                        }
                    }
                val response = httpClient.get(url) {
                    headers.append(HttpHeaders.Authorization, basicAuth())
                }
                require(response.status.isSuccess()) {
                    "Unable to load release definitions (${response.status.value})."
                }
                accumulator += parseDefinitionPage(response.bodyAsText())
                continuation =
                    response.headers["x-ms-continuationtoken"]
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                pages++
            } while (continuation != null && pages < MaxContinuationPages)
            accumulator.distinctBy { it.id }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        }

    override suspend fun listReleasesForDefinition(
        organization: String,
        projectName: String,
        definitionId: Int,
        top: Int,
    ): Result<List<ReleaseSummary>> =
        runCatching {
            require(organization.isNotBlank()) { "Missing organization." }
            require(projectName.isNotBlank()) { "Missing project." }
            val org = organization.trim()
            val proj = projectName.trim()
            val perPage = top.coerceIn(1, 100)
            val accumulator = mutableListOf<ReleaseSummary>()
            var continuation: String? = null
            var pages = 0
            do {
                val url =
                    buildString {
                        append("https://")
                        append(VsrmHost)
                        append("/")
                        append(org.encodeURLPathPart())
                        append("/")
                        append(proj.encodeURLPathPart())
                        append("/_apis/release/releases")
                        append("?api-version=")
                        append(ApiVersion)
                        append("&definitionId=")
                        append(definitionId)
                        append("&${'$'}top=")
                        append(perPage)
                        append("&${'$'}expand=environments")
                        if (!continuation.isNullOrBlank()) {
                            append("&continuationToken=${continuation!!.encodeURLParameter()}")
                        }
                    }
                val response = httpClient.get(url) {
                    headers.append(HttpHeaders.Authorization, basicAuth())
                }
                require(response.status.isSuccess()) {
                    "Unable to load releases (${response.status.value})."
                }
                accumulator += parseReleasePage(response.bodyAsText(), proj)
                continuation =
                    response.headers["x-ms-continuationtoken"]
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                pages++
                if (accumulator.size >= top) break
            } while (continuation != null && pages < MaxContinuationPages)
            accumulator
                .distinctBy { it.id }
                .sortedByDescending { it.createdOnIso.orEmpty() }
                .take(top)
        }

    override suspend fun getRelease(
        organization: String,
        projectName: String,
        releaseId: Int,
    ): Result<ReleaseDetail> =
        runCatching {
            require(organization.isNotBlank()) { "Missing organization." }
            require(projectName.isNotBlank()) { "Missing project." }
            val url =
                buildString {
                    append("https://")
                    append(VsrmHost)
                    append("/")
                    append(organization.trim().encodeURLPathPart())
                    append("/")
                    append(projectName.trim().encodeURLPathPart())
                    append("/_apis/release/releases/")
                    append(releaseId)
                    append("?api-version=")
                    append(ApiVersion)
                    append("&${'$'}expand=environments")
                }
            val response = httpClient.get(url) {
                headers.append(HttpHeaders.Authorization, basicAuth())
            }
            require(response.status.isSuccess()) {
                "Unable to load release (${response.status.value})."
            }
            parseReleaseDetail(response.bodyAsText(), projectName.trim())
        }

    override suspend fun getReleaseDefinition(
        organization: String,
        projectName: String,
        definitionId: Int,
    ): Result<ReleaseDefinitionDetail> =
        runCatching {
            require(organization.isNotBlank()) { "Missing organization." }
            require(projectName.isNotBlank()) { "Missing project." }
            val url =
                buildString {
                    append("https://")
                    append(VsrmHost)
                    append("/")
                    append(organization.trim().encodeURLPathPart())
                    append("/")
                    append(projectName.trim().encodeURLPathPart())
                    append("/_apis/release/definitions/")
                    append(definitionId)
                    append("?api-version=")
                    append(ApiVersion)
                }
            val response = httpClient.get(url) {
                headers.append(HttpHeaders.Authorization, basicAuth())
            }
            require(response.status.isSuccess()) {
                "Unable to load release definition (${response.status.value})."
            }
            parseReleaseDefinitionDetail(response.bodyAsText())
        }

    override suspend fun createRelease(params: CreateReleaseParams): Result<CreatedRelease> =
        runCatching {
            require(params.organization.isNotBlank()) { "Missing organization." }
            require(params.projectName.isNotBlank()) { "Missing project." }
            val url =
                buildString {
                    append("https://")
                    append(VsrmHost)
                    append("/")
                    append(params.organization.trim().encodeURLPathPart())
                    append("/")
                    append(params.projectName.trim().encodeURLPathPart())
                    append("/_apis/release/releases?api-version=")
                    append(ApiVersion)
                }
            val body =
                buildJsonObject {
                    put("definitionId", params.definitionId)
                    put("description", params.description)
                    put("isDraft", false)
                    put("reason", "manual")
                    if (params.manualEnvironmentNames.isNotEmpty()) {
                        putJsonArray("manualEnvironments") {
                            params.manualEnvironmentNames.forEach { add(JsonPrimitive(it)) }
                        }
                    }
                }
            val response =
                httpClient.post(url) {
                    headers.append(HttpHeaders.Authorization, basicAuth())
                    headers.append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(json.encodeToString(body))
                }
            val raw = response.bodyAsText()
            require(response.status.isSuccess()) {
                "Create release failed (${response.status.value}): ${raw.take(400)}"
            }
            parseCreatedRelease(raw)
        }

    override suspend fun deployReleaseEnvironment(
        organization: String,
        projectName: String,
        releaseId: Int,
        environmentId: Int,
    ): Result<Unit> =
        runCatching {
            require(organization.isNotBlank()) { "Missing organization." }
            require(projectName.isNotBlank()) { "Missing project." }
            require(environmentId > 0) { "Missing release environment id." }
            val url =
                buildString {
                    append("https://")
                    append(VsrmHost)
                    append("/")
                    append(organization.trim().encodeURLPathPart())
                    append("/")
                    append(projectName.trim().encodeURLPathPart())
                    append("/_apis/release/releases/")
                    append(releaseId)
                    append("/environments/")
                    append(environmentId)
                    append("?api-version=")
                    append(ApiVersion)
                }
            val body =
                buildJsonObject {
                    put("status", "inProgress")
                    put("scheduledDeploymentTime", JsonNull)
                    put("comment", JsonNull)
                    putJsonObject("variables") { }
                }
            val response =
                httpClient.patch(url) {
                    headers.append(HttpHeaders.Authorization, basicAuth())
                    headers.append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(json.encodeToString(body))
                }
            val raw = response.bodyAsText()
            require(response.status.isSuccess()) {
                "Deploy failed (${response.status.value}): ${raw.take(400)}"
            }
        }

    private fun basicAuth(): String {
        val pat = patProvider()?.trim().orEmpty()
        require(pat.isNotBlank()) { "Session token is missing. Please sign in again." }
        val token = Base64.getEncoder().encodeToString(":$pat".toByteArray(Charsets.UTF_8))
        return "Basic $token"
    }

    private fun parseDefinitionPage(raw: String): List<ReleaseDefinitionSummary> {
        val root = json.parseToJsonElement(raw).jsonObject
        val items = root["value"].asArray()
        return items.mapNotNull { it.asObjectOrNull()?.toDefinitionSummary() }
    }

    private fun parseReleasePage(raw: String, projectName: String): List<ReleaseSummary> {
        val root = json.parseToJsonElement(raw).jsonObject
        val items = root["value"].asArray()
        return parsedReleasesToSummaries(items, projectName)
    }

    private fun parseReleaseDetail(raw: String, projectName: String): ReleaseDetail {
        val root = json.parseToJsonElement(raw).jsonObject
        val id = root["id"].intOrNull() ?: error("Invalid release.")
        val name = root["name"].stringOrNull().orEmpty()
        val rd = root["releaseDefinition"].asObjectOrNull()
        val defName = rd?.get("name").stringOrNull().orEmpty()
        val created = root["createdOn"].stringOrNull()
        val requestedFor = root["requestedFor"].asObjectOrNull()?.get("displayName").stringOrNull()
        val description = root["description"].stringOrNull() ?: root["reason"].stringOrNull()
        val artifacts = root["artifacts"].asArray().mapNotNull { it.asObjectOrNull()?.toArtifactInfo() }
        val envObjects = root["environments"].asArray().mapNotNull { it.asObjectOrNull() }
        val environments = envObjects.map { it.toEnvironmentDetail() }.sortedBy { it.rank }
        val variables = root.parseReleaseVariables()
        val timeline = root.buildReleaseTimeline(envObjects)
        return ReleaseDetail(
            id = id,
            name = name,
            definitionName = defName,
            projectName = projectName,
            createdOnIso = created,
            triggerDescription = description,
            requestedForDisplay = requestedFor,
            artifacts = artifacts,
            environments = environments,
            variables = variables,
            timeline = timeline,
        )
    }

    private fun parseReleaseDefinitionDetail(raw: String): ReleaseDefinitionDetail {
        val root = json.parseToJsonElement(raw).jsonObject
        val id = root["id"].intOrNull() ?: error("Invalid release definition.")
        val name = root["name"].stringOrNull().orEmpty()
        val stages =
            root["environments"].asArray()
                .mapNotNull { it.asObjectOrNull()?.toDefinitionEnvironmentStage() }
                .sortedWith(compareBy({ it.rank }, { it.name.lowercase() }))
        return ReleaseDefinitionDetail(id = id, name = name, stages = stages)
    }

    private fun JsonObject.toDefinitionEnvironmentStage(): ReleaseDefinitionEnvironmentStage? {
        val id = this["id"].intOrNull() ?: return null
        val name = this["name"].stringOrNull()?.trim().orEmpty().ifBlank { return null }
        val rank = this["rank"].intOrNull() ?: 0
        return ReleaseDefinitionEnvironmentStage(id = id, name = name, rank = rank)
    }

    private fun parseCreatedRelease(raw: String): CreatedRelease {
        val root = json.parseToJsonElement(raw).jsonObject
        val id = root["id"].intOrNull() ?: error("Create release response missing id.")
        val name = root["name"].stringOrNull()
        return CreatedRelease(id = id, name = name)
    }

    private fun JsonObject.toDefinitionSummary(): ReleaseDefinitionSummary? {
        val id = this["id"].intOrNull() ?: return null
        val name = this["name"].stringOrNull()?.trim().orEmpty().ifBlank { return null }
        val subtitle = extractDefinitionSubtitle(this)
        return ReleaseDefinitionSummary(id = id, name = name, subtitle = subtitle)
    }

    private fun extractDefinitionSubtitle(obj: JsonObject): String? {
        val envs = obj["environments"].asArray().mapNotNull { it.asObjectOrNull() }
        if (envs.isEmpty()) return null
        val ranked =
            envs.mapNotNull { e ->
                val n = e["name"].stringOrNull() ?: return@mapNotNull null
                val rank = e["rank"].intOrNull() ?: 0
                n to rank
            }
        if (ranked.isEmpty()) return null
        return ranked.maxByOrNull { it.second }?.first
    }

    private fun parsedReleasesToSummaries(items: JsonArray, projectName: String): List<ReleaseSummary> {
        return items.mapNotNull { el ->
            val root = el.asObjectOrNull() ?: return@mapNotNull null
            val id = root["id"].intOrNull() ?: return@mapNotNull null
            val name = root["name"].stringOrNull().orEmpty()
            val status = root["status"].stringOrNull()
            val rd = root["releaseDefinition"].asObjectOrNull()
            val defName = rd?.get("name").stringOrNull().orEmpty()
            val defId = rd?.get("id").intOrNull() ?: return@mapNotNull null
            val created = root["createdOn"].stringOrNull()
            val (commit, branch) = root.extractPrimaryArtifactHints()
            val stages =
                root["environments"].asArray()
                    .mapNotNull { it.asObjectOrNull() }
                    .sortedBy { it["rank"].intOrNull() ?: 0 }
                    .mapNotNull { eo ->
                        val n = eo["name"].stringOrNull()?.trim().orEmpty().ifBlank { return@mapNotNull null }
                        ReleaseStagePill(n, mapEnvStatus(eo["status"].stringOrNull()))
                    }
            ReleaseSummary(
                id = id,
                name = name,
                status = status,
                definitionId = defId,
                definitionName = defName,
                projectName = projectName,
                createdOnIso = created,
                commitShort = commit,
                branchLabel = branch,
                stages = stages,
            )
        }
    }

    private fun JsonObject.toArtifactInfo(): ReleaseArtifactInfo? {
        val alias = this["alias"].stringOrNull()?.takeIf { it.isNotBlank() } ?: return null
        val (commit, branch) = extractArtifactCommitAndBranch()
        return ReleaseArtifactInfo(alias = alias, branch = branch, commitShort = commit)
    }

    private fun JsonObject.toEnvironmentDetail(): ReleaseEnvironmentInfo {
        val id = this["id"].intOrNull() ?: 0
        val name = this["name"].stringOrNull().orEmpty()
        val rank = this["rank"].intOrNull() ?: 0
        val status = mapEnvStatus(this["status"].stringOrNull())
        val rawStatus = this["status"].stringOrNull().orEmpty()
        val detail = firstDeployStepName(this)
        return ReleaseEnvironmentInfo(
            id = id,
            name = name,
            rank = rank,
            status = status,
            statusLabel = rawStatus.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
            detailLine = detail,
        )
    }

    private fun firstDeployStepName(env: JsonObject): String? {
        val steps = env["deploySteps"].asArray()
        val named =
            steps.mapNotNull { s ->
                val o = s.asObjectOrNull() ?: return@mapNotNull null
                o["name"].stringOrNull()?.takeIf { it.isNotBlank() }
            }
        return named.lastOrNull() ?: named.firstOrNull()
    }

    private fun JsonObject.extractPrimaryArtifactHints(): Pair<String?, String?> {
        val first = this["artifacts"].asArray().firstOrNull()?.asObjectOrNull() ?: return null to null
        return first.extractArtifactCommitAndBranch()
    }

    /** Resolves commit/branch using the same `definitionReference` shape as the Azure DevOps REST samples. */
    private fun JsonObject.extractArtifactCommitAndBranch(): Pair<String?, String?> {
        val defRef = this["definitionReference"].asObjectOrNull() ?: return null to null
        val branchObj = defRef["branch"].asObjectOrNull()
        val branch =
            refBranchDisplay(branchObj?.get("name").stringOrNull())
                ?: refBranchDisplay(branchObj?.get("id").stringOrNull())
                ?: refBranchDisplay(defRef["sourceBranch"].stringOrNull())
        val sourceVersionObj = defRef["sourceVersion"].asObjectOrNull()
        val commitFromSource =
            sourceVersionObj?.get("id").stringOrNull()?.let { sha ->
                if (sha.length >= 7) sha.take(7) else sha
            }
        val version = defRef["version"].asObjectOrNull()
        val buildOrVersionId = version?.get("id").stringOrNull()
        val commit =
            commitFromSource
                ?: buildOrVersionId?.let { id -> if (id.length >= 7 && id.all { it.isLetterOrDigit() }) id.take(7) else null }
                ?: this["buildId"].stringOrNull()
        return commit to branch
    }

    private fun refBranchDisplay(ref: String?): String? {
        if (ref.isNullOrBlank()) return null
        return ref.removePrefix("refs/heads/").trim().ifBlank { null } ?: ref
    }

    private fun JsonObject.parseReleaseVariables(): List<ReleaseVariableRow> {
        val varsEl = this["variables"] ?: return emptyList()
        val vars = varsEl as? JsonObject ?: return emptyList()
        return vars
            .mapNotNull { (key, el) ->
                val o = el as? JsonObject ?: return@mapNotNull null
                val isSecret = o["isSecret"].jsonBooleanOrNull() == true
                val raw = o["value"].stringOrNull().orEmpty()
                ReleaseVariableRow(
                    name = key,
                    value = if (isSecret) "********" else raw,
                    isSecret = isSecret,
                )
            }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    private fun JsonObject.buildReleaseTimeline(envRaw: List<JsonObject>): List<ReleaseTimelineEntry> {
        val lines = mutableListOf<ReleaseTimelineEntry>()
        fun add(ts: String?, description: String) {
            val t = ts?.trim().orEmpty()
            if (t.isNotEmpty()) {
                lines += ReleaseTimelineEntry(timestampIso = t, description = description)
            }
        }
        val createdBy = this["createdBy"].asObjectOrNull()?.get("displayName").stringOrNull()
        add(this["createdOn"].stringOrNull(), "Release created${createdBy?.let { " by $it" } ?: ""}")
        val modBy = this["modifiedBy"].asObjectOrNull()?.get("displayName").stringOrNull()
        add(this["modifiedOn"].stringOrNull(), "Release modified${modBy?.let { " by $it" } ?: ""}")
        for (env in envRaw) {
            val ename = env["name"].stringOrNull().orEmpty()
            add(env["createdOn"].stringOrNull(), "Environment \"$ename\" created")
            add(env["timeDeployed"].stringOrNull(), "Environment \"$ename\" deployed")
            add(env["scheduledDeploymentTime"].stringOrNull(), "Environment \"$ename\" scheduled")
            val steps = env["deploySteps"].asArray()
            for (step in steps) {
                val o = step.asObjectOrNull() ?: continue
                val stepName = o["name"].stringOrNull()?.takeIf { it.isNotBlank() } ?: continue
                val st = o["status"].stringOrNull() ?: o["operationStatus"].stringOrNull()
                val t =
                    o["completedOn"].stringOrNull()
                        ?: o["lastModifiedOn"].stringOrNull()
                        ?: o["startedOn"].stringOrNull()
                if (t != null) {
                    add(t, "$ename — $stepName (${st ?: "—"})")
                }
            }
        }
        return lines.sortedWith(compareBy({ it.timestampIso }, { it.description }))
    }
}

private fun mapEnvStatus(raw: String?): ReleaseDeploymentStatus {
    return when (raw?.lowercase().orEmpty()) {
        "notstarted",
        "scheduled",
        -> ReleaseDeploymentStatus.NotStarted
        "inprogress",
        "queued",
        "queueing",
        "scheduledfordeployment",
        "cancelling",
        "pendingforagents",
        "scheduledforpartiallysucceededdeployment",
            -> ReleaseDeploymentStatus.InProgress
        "succeeded",
        "partiallysucceeded",
            -> ReleaseDeploymentStatus.Succeeded
        "failed",
        "rejected",
            -> ReleaseDeploymentStatus.Failed
        "canceled",
        "cancelled",
        "superseded",
            -> ReleaseDeploymentStatus.Cancelled
        else -> ReleaseDeploymentStatus.Unknown
    }
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
