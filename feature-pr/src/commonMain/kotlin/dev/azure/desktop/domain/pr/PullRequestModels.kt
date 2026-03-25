package dev.azure.desktop.domain.pr

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
)

data class PullRequestReviewer(
    val displayName: String,
    val uniqueName: String?,
    val vote: Int,
)
