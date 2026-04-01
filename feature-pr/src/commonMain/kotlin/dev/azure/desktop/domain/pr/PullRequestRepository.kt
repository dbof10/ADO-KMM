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
     * - 5: Approved with suggestions
     * - 0: No vote / waiting
     * - -5: Waiting for author
     * - -10: Rejected
     */
    suspend fun setMyPullRequestVote(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
        vote: Int,
    ): Result<Unit>

    /**
     * Closes an active pull request by abandoning it (Azure DevOps `status`: `abandoned`).
     */
    suspend fun abandonPullRequest(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
    ): Result<Unit>

    /**
     * Enables Azure DevOps **auto-complete** for a pull request.
     *
     * ADO merges the PR once policies are satisfied using [mergeStrategy].
     *
     * Azure DevOps REST: `Pull Requests - Update` supports updating `autoCompleteSetBy.id` and `completionOptions`.
     */
    suspend fun enableAutoComplete(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
        mergeStrategy: PullRequestMergeStrategy,
    ): Result<Unit>

    /**
     * Loads binary content from a signed-in Azure DevOps HTTPS URL (e.g. PR comment image attachments).
     * Only `https://dev.azure.com/...` is accepted.
     */
    suspend fun fetchAuthenticatedDevOpsResource(url: String): Result<ByteArray>
}
