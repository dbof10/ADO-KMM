package dev.azure.desktop.pr

import dev.azure.desktop.domain.pr.CreatedPullRequest
import dev.azure.desktop.domain.pr.CreatePullRequestParams
import dev.azure.desktop.domain.pr.DevOpsProject
import dev.azure.desktop.domain.pr.PullRequestBranchRef
import dev.azure.desktop.domain.pr.PullRequestChange
import dev.azure.desktop.domain.pr.PullRequestDetail
import dev.azure.desktop.domain.pr.ProjectSelectionCountStorage
import dev.azure.desktop.domain.pr.PullRequestRepository
import dev.azure.desktop.domain.pr.PullRequestMergeStrategy
import dev.azure.desktop.domain.pr.PullRequestRepositoryRef
import dev.azure.desktop.domain.pr.PullRequestSuggestion
import dev.azure.desktop.domain.pr.PullRequestSummary

/**
 * Minimal [PullRequestRepository] for FlowRedux / use-case tests; override only what each test needs.
 */
internal open class StubPullRequestRepository : PullRequestRepository {
    var projectsResult: Result<List<DevOpsProject>> = Result.success(emptyList())
    var myPrsResult: Result<List<PullRequestSummary>> = Result.success(emptyList())
    var activePrsResult: Result<List<PullRequestSummary>> = Result.success(emptyList())
    var summaryByIdResult: Result<PullRequestSummary> =
        Result.failure(IllegalStateException("summaryById not configured"))
    var detailResult: Result<PullRequestDetail> =
        Result.failure(IllegalStateException("detail not configured"))
    var changesResult: Result<List<PullRequestChange>> = Result.success(emptyList())
    var fileAtCommitResult: Result<String?> = Result.success("")
    var setVoteResult: Result<Unit> = Result.success(Unit)
    var abandonResult: Result<Unit> = Result.success(Unit)
    var enableAutoCompleteResult: Result<Unit> = Result.success(Unit)

    override suspend fun listProjects(organization: String): Result<List<DevOpsProject>> = projectsResult

    override suspend fun getMyPullRequests(
        organization: String,
        projectName: String?,
    ): Result<List<PullRequestSummary>> = myPrsResult

    override suspend fun getActivePullRequests(
        organization: String,
        projectName: String?,
    ): Result<List<PullRequestSummary>> = activePrsResult

    override suspend fun listRepositories(
        organization: String,
        projectName: String,
    ): Result<List<PullRequestRepositoryRef>> = Result.success(emptyList())

    override suspend fun listBranches(
        organization: String,
        projectName: String,
        repositoryId: String,
    ): Result<List<PullRequestBranchRef>> = Result.success(emptyList())

    override suspend fun findCreatePullRequestSuggestion(
        organization: String,
        projectName: String?,
    ): Result<PullRequestSuggestion?> = Result.success(null)

    override suspend fun createPullRequest(params: CreatePullRequestParams): Result<CreatedPullRequest> =
        Result.failure(NotImplementedError())

    override suspend fun getPullRequestSummaryById(
        organization: String,
        projectName: String,
        pullRequestId: Int,
    ): Result<PullRequestSummary> = summaryByIdResult

    override suspend fun getPullRequestDetail(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
    ): Result<PullRequestDetail> = detailResult

    override suspend fun getPullRequestChanges(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
        baseCommitId: String,
        targetCommitId: String,
    ): Result<List<PullRequestChange>> = changesResult

    override suspend fun getFileContentAtCommit(
        organization: String,
        projectName: String,
        repositoryId: String,
        path: String,
        commitId: String,
    ): Result<String?> = fileAtCommitResult

    override suspend fun setMyPullRequestVote(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
        vote: Int,
    ): Result<Unit> = setVoteResult

    override suspend fun abandonPullRequest(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
    ): Result<Unit> = abandonResult

    override suspend fun enableAutoComplete(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
        mergeStrategy: PullRequestMergeStrategy,
    ): Result<Unit> = enableAutoCompleteResult

    override suspend fun fetchAuthenticatedDevOpsResource(url: String): Result<ByteArray> =
        Result.failure(NotImplementedError())
}

internal fun samplePullRequestSummary(
    id: Int = 42,
    projectName: String = "Proj",
    status: String = "active",
) = PullRequestSummary(
    id = id,
    title = "Test PR",
    status = status,
    creatorDisplayName = "Dev",
    creatorUniqueName = null,
    sourceRefName = "refs/heads/feature",
    targetRefName = "refs/heads/main",
    repositoryName = "repo",
    repositoryId = "repo-id",
    projectName = projectName,
    projectId = "proj-id",
    creationDateIso = null,
)

internal class InMemoryProjectSelectionStorage : ProjectSelectionCountStorage {
    private val counts = mutableMapOf<String, MutableMap<String, Int>>()

    override fun getSelectionCounts(organization: String): Map<String, Int> =
        counts[organization].orEmpty()

    override fun incrementSelection(organization: String, projectName: String): Result<Unit> {
        val m = counts.getOrPut(organization) { mutableMapOf() }
        m[projectName] = (m[projectName] ?: 0) + 1
        return Result.success(Unit)
    }

    override fun clearAllSelections(): Result<Unit> {
        counts.clear()
        return Result.success(Unit)
    }
}

internal fun samplePullRequestDetail(
    summary: PullRequestSummary = samplePullRequestSummary(),
    lastMergeTargetCommitId: String? = null,
    lastMergeSourceCommitId: String? = null,
) = PullRequestDetail(
    summary = summary,
    description = null,
    reviewers = emptyList(),
    timeline = emptyList(),
    linkedWorkItems = emptyList(),
    checks = emptyList(),
    lastMergeSourceCommitId = lastMergeSourceCommitId,
    lastMergeTargetCommitId = lastMergeTargetCommitId,
)
