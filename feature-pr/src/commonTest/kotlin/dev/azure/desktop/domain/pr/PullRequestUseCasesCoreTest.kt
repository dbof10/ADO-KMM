package dev.azure.desktop.domain.pr

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PullRequestUseCasesCoreTest {
    @Test
    fun trimsAndDelegatesArguments() = runBlocking {
        val repository = RecordingPullRequestRepository()

        assertTrue(ListProjectsUseCase(repository)(" org ").isSuccess)
        assertEquals("org", repository.lastOrganization)

        assertTrue(GetMyPullRequestsUseCase(repository)("org", "   ").isSuccess)
        assertNull(repository.lastProjectName)

        assertTrue(GetActivePullRequestsUseCase(repository)("org", " project ").isSuccess)
        assertEquals("project", repository.lastProjectName)

        assertTrue(GetPullRequestSummaryByIdUseCase(repository)(" org ", " project ", 42).isSuccess)
        assertEquals(42, repository.lastPullRequestId)

        assertTrue(SetMyPullRequestVoteUseCase(repository)(" org ", "project", "repo", 7, 10).isSuccess)
        assertEquals(10, repository.lastVote)

        assertTrue(AbandonPullRequestUseCase(repository)(" org ", "project", "repo", 7).isSuccess)
        assertEquals(7, repository.lastAbandonPullRequestId)
    }
}

private class RecordingPullRequestRepository : PullRequestRepository {
    var lastOrganization: String? = null
    var lastProjectName: String? = null
    var lastPullRequestId: Int? = null
    var lastAbandonPullRequestId: Int? = null
    var lastVote: Int? = null

    override suspend fun listProjects(organization: String): Result<List<DevOpsProject>> {
        lastOrganization = organization
        return Result.success(emptyList())
    }

    override suspend fun getMyPullRequests(organization: String, projectName: String?): Result<List<PullRequestSummary>> {
        lastOrganization = organization
        lastProjectName = projectName
        return Result.success(emptyList())
    }

    override suspend fun getActivePullRequests(organization: String, projectName: String?): Result<List<PullRequestSummary>> {
        lastOrganization = organization
        lastProjectName = projectName
        return Result.success(emptyList())
    }

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
        Result.failure(NotImplementedError("unused"))

    override suspend fun getPullRequestSummaryById(
        organization: String,
        projectName: String,
        pullRequestId: Int,
    ): Result<PullRequestSummary> {
        lastOrganization = organization
        lastProjectName = projectName
        lastPullRequestId = pullRequestId
        return Result.success(
            PullRequestSummary(
                id = pullRequestId,
                title = "title",
                status = "active",
                creatorDisplayName = "Dev",
                creatorUniqueName = null,
                sourceRefName = "refs/heads/a",
                targetRefName = "refs/heads/main",
                repositoryName = "repo",
                repositoryId = "repo",
                projectName = projectName,
                projectId = "project",
                creationDateIso = null,
            ),
        )
    }

    override suspend fun getPullRequestDetail(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
    ): Result<PullRequestDetail> = Result.failure(NotImplementedError("unused"))

    override suspend fun getPullRequestChanges(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
        baseCommitId: String,
        targetCommitId: String,
    ): Result<List<PullRequestChange>> = Result.failure(NotImplementedError("unused"))

    override suspend fun getFileContentAtCommit(
        organization: String,
        projectName: String,
        repositoryId: String,
        path: String,
        commitId: String,
    ): Result<String?> = Result.failure(NotImplementedError("unused"))

    override suspend fun setMyPullRequestVote(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
        vote: Int,
    ): Result<Unit> {
        lastOrganization = organization
        lastProjectName = projectName
        lastPullRequestId = pullRequestId
        lastVote = vote
        return Result.success(Unit)
    }

    override suspend fun abandonPullRequest(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
    ): Result<Unit> {
        lastOrganization = organization
        lastProjectName = projectName
        lastPullRequestId = pullRequestId
        lastAbandonPullRequestId = pullRequestId
        return Result.success(Unit)
    }

    override suspend fun fetchAuthenticatedDevOpsResource(url: String): Result<ByteArray> =
        Result.failure(NotImplementedError("unused"))
}
