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

data class PullRequestRepositoryRef(
    val id: String,
    val name: String,
    val projectName: String,
)

data class PullRequestBranchRef(
    val repositoryId: String,
    val repositoryName: String,
    val name: String,
    /** Present when ADO populates IdentityRef on the ref (matches connectiondata `authenticatedUser.id`). */
    val creatorId: String?,
    val creatorUniqueName: String?,
    val creatorMailAddress: String?,
    val latestUpdateDateIso: String?,
)

data class PullRequestSuggestion(
    val projectName: String,
    val repositoryId: String,
    val repositoryName: String,
    val sourceBranchName: String,
    val targetBranchName: String,
)

data class CreatePullRequestParams(
    val organization: String,
    val projectName: String,
    val repositoryId: String,
    val sourceBranchName: String,
    val targetBranchName: String,
    val title: String,
    val description: String,
)

data class CreatedPullRequest(
    val pullRequestId: Int,
    val projectName: String,
    val repositoryId: String,
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

/** Azure DevOps Git pull request reviewer `vote` field values (REST API). */
object PullRequestReviewerVote {
    const val APPROVED = 10
    const val APPROVED_WITH_SUGGESTIONS = 5
    const val NO_VOTE = 0
    const val WAITING_FOR_AUTHOR = -5
    const val REJECTED = -10
}

/**
 * Display label for [PullRequestReviewer.vote], matching Azure DevOps Git API values in [PullRequestReviewerVote].
 */
fun reviewerVoteDisplayLabel(vote: Int): String =
    when (vote) {
        in PullRequestReviewerVote.APPROVED..Int.MAX_VALUE -> "Approved"
        PullRequestReviewerVote.APPROVED_WITH_SUGGESTIONS -> "Approved with suggestions"
        PullRequestReviewerVote.NO_VOTE -> "No vote"
        PullRequestReviewerVote.WAITING_FOR_AUTHOR -> "Waiting for author"
        PullRequestReviewerVote.REJECTED -> "Rejected"
        else -> "No vote"
    }

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
