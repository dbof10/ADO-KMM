package dev.azure.desktop.domain.pr

interface PullRequestRepository {
    suspend fun listProjects(organization: String): Result<List<DevOpsProject>>

    suspend fun getMyPullRequests(organization: String, projectName: String?): Result<List<PullRequestSummary>>

    suspend fun getActivePullRequests(organization: String, projectName: String?): Result<List<PullRequestSummary>>

    suspend fun listRepositories(
        organization: String,
        projectName: String,
    ): Result<List<PullRequestRepositoryRef>>

    suspend fun listBranches(
        organization: String,
        projectName: String,
        repositoryId: String,
    ): Result<List<PullRequestBranchRef>>

    suspend fun findCreatePullRequestSuggestion(
        organization: String,
        projectName: String?,
    ): Result<PullRequestSuggestion?>

    suspend fun createPullRequest(params: CreatePullRequestParams): Result<CreatedPullRequest>

    /**
     * Fetch a pull request directly by id (works for completed/abandoned too).
     *
     * Azure DevOps endpoint: `/_apis/git/pullrequests/{pullRequestId}` (project-scoped).
     */
    suspend fun getPullRequestSummaryById(
        organization: String,
        projectName: String,
        pullRequestId: Int,
    ): Result<PullRequestSummary>

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

    /**
     * Sets the authenticated user's vote on a pull request.
     *
     * Common values:
     * - 10: Approved
     * - -10: Rejected
     * - 0: Reset / waiting
     */
    suspend fun setMyPullRequestVote(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
        vote: Int,
    ): Result<Unit>
}
