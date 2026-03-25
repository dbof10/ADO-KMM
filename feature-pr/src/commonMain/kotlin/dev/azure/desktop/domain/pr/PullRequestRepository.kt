package dev.azure.desktop.domain.pr

interface PullRequestRepository {
    suspend fun getMyPullRequests(organization: String): Result<List<PullRequestSummary>>

    suspend fun getPullRequestDetail(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
    ): Result<PullRequestDetail>
}
