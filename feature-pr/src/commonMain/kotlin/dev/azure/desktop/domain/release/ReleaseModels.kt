package dev.azure.desktop.domain.release

/** UX-facing deployment status for environments and stage pills. */
enum class ReleaseDeploymentStatus {
    NotStarted,
    InProgress,
    Succeeded,
    Failed,
    Cancelled,
    Unknown,
}

data class ReleaseDefinitionSummary(
    val id: Int,
    val name: String,
    /** Short hint shown under the definition name (often an environment label). */
    val subtitle: String?,
)

data class ReleaseStagePill(
    val name: String,
    val status: ReleaseDeploymentStatus,
)

data class ReleaseSummary(
    val id: Int,
    val name: String,
    /** Raw status from ADO (e.g. `active`, `abandoned` — see release list API). */
    val status: String?,
    val definitionId: Int,
    val definitionName: String,
    val projectName: String,
    val createdOnIso: String?,
    val commitShort: String?,
    val branchLabel: String?,
    val stages: List<ReleaseStagePill>,
)

data class ReleaseArtifactInfo(
    val alias: String,
    val branch: String?,
    val commitShort: String?,
)

data class ReleaseEnvironmentInfo(
    val name: String,
    val rank: Int,
    val status: ReleaseDeploymentStatus,
    val statusLabel: String,
    val detailLine: String?,
)

data class ReleaseVariableRow(
    val name: String,
    /** Masked when [isSecret]; values come from the release payload `variables` map. */
    val value: String,
    val isSecret: Boolean,
)

/** History tab rows — from ADO timestamps on the release, environments, and deploy steps (same GET payload). */
data class ReleaseTimelineEntry(
    val timestampIso: String,
    val description: String,
)

data class ReleaseDetail(
    val id: Int,
    val name: String,
    val definitionName: String,
    val projectName: String,
    val createdOnIso: String?,
    val triggerDescription: String?,
    val requestedForDisplay: String?,
    val artifacts: List<ReleaseArtifactInfo>,
    val environments: List<ReleaseEnvironmentInfo>,
    /** `variables` on the release resource from Azure DevOps. */
    val variables: List<ReleaseVariableRow>,
    /** Built from `createdOn` / `modifiedOn` / environment and deploy-step times in the API response. */
    val timeline: List<ReleaseTimelineEntry>,
)
