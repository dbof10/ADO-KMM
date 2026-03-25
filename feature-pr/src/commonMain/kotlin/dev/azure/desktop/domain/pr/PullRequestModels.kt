package dev.azure.desktop.domain.pr

data class DevOpsProject(
    val id: String,
    val name: String,
)

data class PullRequestSummary(
    val id: Int,
    val title: String,
    val status: String,
    val creatorDisplayName: String,
    val creatorUniqueName: String?,
    val sourceRefName: String,
    val targetRefName: String,
    val repositoryName: String,
    val repositoryId: String,
    val projectName: String,
    val projectId: String,
    val creationDateIso: String?,
)

data class PullRequestDetail(
    val summary: PullRequestSummary,
    val description: String?,
    val reviewers: List<PullRequestReviewer>,
    val timeline: List<PullRequestTimelineItem>,
    val linkedWorkItems: List<PullRequestLinkedWorkItem>,
    val checks: List<PullRequestCheckStatus>,
    val lastMergeSourceCommitId: String?,
    val lastMergeTargetCommitId: String?,
    /** Line counts vs target branch (same basis as code review); null when unavailable. */
    val linesAdded: Int? = null,
    val linesRemoved: Int? = null,
)

data class PullRequestLinkedWorkItem(
    val id: Int,
    val title: String?,
    val type: String?,
    val state: String?,
)

enum class PullRequestCheckState {
    Succeeded,
    Failed,
    Running,
    Pending,
    NotApplicable,
    Unknown,
}

data class PullRequestCheckStatus(
    val name: String,
    val state: PullRequestCheckState,
    val description: String?,
)

data class PullRequestChange(
    val path: String,
    val changeType: String,
    val isFolder: Boolean = false,
)

sealed class PullRequestDiffLine {
    abstract val text: String

    data class Context(
        override val text: String,
        val oldLineNumber: Int?,
        val newLineNumber: Int?,
    ) : PullRequestDiffLine()

    data class Added(
        override val text: String,
        val newLineNumber: Int?,
    ) : PullRequestDiffLine()

    data class Removed(
        override val text: String,
        val oldLineNumber: Int?,
    ) : PullRequestDiffLine()
}

data class PullRequestReviewer(
    val displayName: String,
    val uniqueName: String?,
    val vote: Int,
)

sealed class PullRequestTimelineItem {
    abstract val actorDisplayName: String
    abstract val createdDateIso: String?

    data class Comment(
        override val actorDisplayName: String,
        override val createdDateIso: String?,
        val content: String,
    ) : PullRequestTimelineItem()

    data class Approval(
        override val actorDisplayName: String,
        override val createdDateIso: String?,
        val vote: Int,
    ) : PullRequestTimelineItem()

    data class Commit(
        val commitId: String,
        val message: String,
        override val createdDateIso: String?,
        override val actorDisplayName: String,
    ) : PullRequestTimelineItem()
}
