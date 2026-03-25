package dev.azure.desktop.domain.pr

interface PullRequestRepository {
    suspend fun listProjects(organization: String): Result<List<DevOpsProject>>

    suspend fun getMyPullRequests(organization: String, projectName: String?): Result<List<PullRequestSummary>>

    suspend fun getActivePullRequests(organization: String, projectName: String?): Result<List<PullRequestSummary>>

    suspend fun getPullRequestDetail(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
    ): Result<PullRequestDetail>

    suspend fun getPullRequestChanges(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
        baseCommitId: String,
        targetCommitId: String,
    ): Result<List<PullRequestChange>>

    /**
     * Returns file content at a given commit, or null when the item doesn't exist (e.g. added/deleted).
     */
    suspend fun getFileContentAtCommit(
        organization: String,
        projectName: String,
        repositoryId: String,
        path: String,
        commitId: String,
    ): Result<String?>
}
