package dev.azure.desktop.domain.pr

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FindPullRequestSummaryByIdUseCaseTest {
    @Test
    fun findsInSpecifiedProject() =
        runBlocking {
            val repository = SearchRepository()
            repository.activeByProject["project-a"] = listOf(sampleSummary(12, "project-a"))

            val useCase =
                FindPullRequestSummaryByIdUseCase(
                    listProjectsUseCase = ListProjectsUseCase(repository),
                    getMyPullRequestsUseCase = GetMyPullRequestsUseCase(repository),
                    getActivePullRequestsUseCase = GetActivePullRequestsUseCase(repository),
                )

            val result = useCase(" org ", " project-a ", 12)

            assertTrue(result.isSuccess)
            assertEquals(12, result.getOrThrow().id)
        }

    @Test
    fun searchesAcrossProjectsWhenProjectMissing() =
        runBlocking {
            val repository = SearchRepository()
            repository.projects = listOf(DevOpsProject("1", "alpha"), DevOpsProject("2", "beta"))
            repository.myByProject["beta"] = listOf(sampleSummary(99, "beta"))

            val useCase =
                FindPullRequestSummaryByIdUseCase(
                    listProjectsUseCase = ListProjectsUseCase(repository),
                    getMyPullRequestsUseCase = GetMyPullRequestsUseCase(repository),
                    getActivePullRequestsUseCase = GetActivePullRequestsUseCase(repository),
                )

            val result = useCase("org", null, 99)

            assertTrue(result.isSuccess)
            assertEquals("beta", result.getOrThrow().projectName)
        }

    @Test
    fun returnsFailureWhenPrDoesNotExist() =
        runBlocking {
            val repository = SearchRepository()
            repository.projects = listOf(DevOpsProject("1", "alpha"))

            val useCase =
                FindPullRequestSummaryByIdUseCase(
                    listProjectsUseCase = ListProjectsUseCase(repository),
                    getMyPullRequestsUseCase = GetMyPullRequestsUseCase(repository),
                    getActivePullRequestsUseCase = GetActivePullRequestsUseCase(repository),
                )

            val result = useCase("org", null, 404)

            assertTrue(result.isFailure)
            assertEquals("Pull request #404 not found.", result.exceptionOrNull()?.message)
        }
}

private class SearchRepository : PullRequestRepository {
    var projects: List<DevOpsProject> = emptyList()
    val activeByProject = mutableMapOf<String, List<PullRequestSummary>>()
    val myByProject = mutableMapOf<String, List<PullRequestSummary>>()

    override suspend fun listProjects(organization: String): Result<List<DevOpsProject>> = Result.success(projects)

    override suspend fun getMyPullRequests(
        organization: String,
        projectName: String?,
    ): Result<List<PullRequestSummary>> = Result.success(myByProject[projectName] ?: emptyList())

    override suspend fun getActivePullRequests(
        organization: String,
        projectName: String?,
    ): Result<List<PullRequestSummary>> = Result.success(activeByProject[projectName] ?: emptyList())

    override suspend fun getPullRequestSummaryById(
        organization: String,
        projectName: String,
        pullRequestId: Int,
    ): Result<PullRequestSummary> = Result.failure(NotImplementedError("unused"))

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
    ): Result<Unit> = Result.failure(NotImplementedError("unused"))
}

private fun sampleSummary(id: Int, project: String): PullRequestSummary =
    PullRequestSummary(
        id = id,
        title = "PR-$id",
        status = "active",
        creatorDisplayName = "Dev",
        creatorUniqueName = "dev@example.com",
        sourceRefName = "refs/heads/feature",
        targetRefName = "refs/heads/main",
        repositoryName = "repo",
        repositoryId = "repo-1",
        projectName = project,
        projectId = "project-1",
        creationDateIso = null,
    )
