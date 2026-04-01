package dev.azure.desktop.domain.pr

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PullRequestExtraUseCasesTest {
    @Test
    fun listRepositoriesTrimsAndDelegates() =
        runBlocking {
            val repo = TrackingPullRequestRepository()
            assertTrue(ListPullRequestRepositoriesUseCase(repo)(" org ", " proj ").isSuccess)
            assertEquals("org", repo.lastListRepositoriesOrganization)
            assertEquals("proj", repo.lastListRepositoriesProject)
        }

    @Test
    fun listBranchesTrimsAndDelegates() =
        runBlocking {
            val repo = TrackingPullRequestRepository()
            assertTrue(
                ListPullRequestBranchesUseCase(repo)(" o ", " p ", " repo-id ").isSuccess,
            )
            assertEquals("o", repo.lastListBranchesOrganization)
            assertEquals("p", repo.lastListBranchesProject)
            assertEquals("repo-id", repo.lastListBranchesRepositoryId)
        }

    @Test
    fun findCreateSuggestionPassesNullProjectWhenBlank() =
        runBlocking {
            val repo = TrackingPullRequestRepository()
            assertTrue(FindCreatePullRequestSuggestionUseCase(repo)(" org ", "   ").isSuccess)
            assertEquals("org", repo.lastFindSuggestionOrganization)
            assertNull(repo.lastFindSuggestionProject)
        }

    @Test
    fun findCreateSuggestionTrimsProjectWhenPresent() =
        runBlocking {
            val repo = TrackingPullRequestRepository()
            assertTrue(FindCreatePullRequestSuggestionUseCase(repo)("org", " proj ").isSuccess)
            assertEquals("proj", repo.lastFindSuggestionProject)
        }

    @Test
    fun createPullRequestTrimsAllStringFields() =
        runBlocking {
            val repo = TrackingPullRequestRepository()
            val params =
                CreatePullRequestParams(
                    organization = " o ",
                    projectName = " p ",
                    repositoryId = " r ",
                    sourceBranchName = " s ",
                    targetBranchName = " t ",
                    title = " title ",
                    description = " desc ",
                )
            assertTrue(CreatePullRequestUseCase(repo)(params).isFailure)
            val passed = repo.lastCreateParams ?: error("expected createPullRequest to be called")
            assertEquals("o", passed.organization)
            assertEquals("p", passed.projectName)
            assertEquals("r", passed.repositoryId)
            assertEquals("s", passed.sourceBranchName)
            assertEquals("t", passed.targetBranchName)
            assertEquals("title", passed.title)
            assertEquals("desc", passed.description)
        }

    @Test
    fun incrementProjectSelectionDelegates() {
        val storage = FakeProjectSelectionStorage()
        assertTrue(IncrementProjectSelectionUseCase(storage)("fab", "proj").isSuccess)
        assertEquals(1, storage.counts["fab"]?.get("proj"))
    }

    @Test
    fun getMostSelectedProjectPicksHighestCount() {
        val storage = FakeProjectSelectionStorage()
        storage.counts.getOrPut("org") { mutableMapOf() }["a"] = 1
        storage.counts["org"]!!["b"] = 5
        storage.counts["org"]!!["c"] = 2
        val useCase = GetMostSelectedProjectUseCase(storage)
        assertEquals("b", useCase("org", listOf("a", "c", "b")))
    }

    @Test
    fun getMostSelectedProjectReturnsNullForEmptyList() {
        val useCase = GetMostSelectedProjectUseCase(FakeProjectSelectionStorage())
        assertNull(useCase("org", emptyList()))
    }

    @Test
    fun clearProjectSelectionCountsDelegates() {
        val storage = FakeProjectSelectionStorage()
        storage.counts["org"] = mutableMapOf("p" to 1)
        assertTrue(ClearProjectSelectionCountsUseCase(storage)().isSuccess)
        assertTrue(storage.counts.isEmpty())
    }
}

private class FakeProjectSelectionStorage : ProjectSelectionCountStorage {
    val counts = mutableMapOf<String, MutableMap<String, Int>>()

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

private class TrackingPullRequestRepository : PullRequestRepository {
    var lastListRepositoriesOrganization: String? = null
    var lastListRepositoriesProject: String? = null
    var lastListBranchesOrganization: String? = null
    var lastListBranchesProject: String? = null
    var lastListBranchesRepositoryId: String? = null
    var lastFindSuggestionOrganization: String? = null
    var lastFindSuggestionProject: String? = null
    var lastCreateParams: CreatePullRequestParams? = null

    override suspend fun listProjects(organization: String): Result<List<DevOpsProject>> =
        Result.success(emptyList())

    override suspend fun getMyPullRequests(
        organization: String,
        projectName: String?,
    ): Result<List<PullRequestSummary>> = Result.success(emptyList())

    override suspend fun getActivePullRequests(
        organization: String,
        projectName: String?,
    ): Result<List<PullRequestSummary>> = Result.success(emptyList())

    override suspend fun listRepositories(
        organization: String,
        projectName: String,
    ): Result<List<PullRequestRepositoryRef>> {
        lastListRepositoriesOrganization = organization
        lastListRepositoriesProject = projectName
        return Result.success(emptyList())
    }

    override suspend fun listBranches(
        organization: String,
        projectName: String,
        repositoryId: String,
    ): Result<List<PullRequestBranchRef>> {
        lastListBranchesOrganization = organization
        lastListBranchesProject = projectName
        lastListBranchesRepositoryId = repositoryId
        return Result.success(emptyList())
    }

    override suspend fun findCreatePullRequestSuggestion(
        organization: String,
        projectName: String?,
    ): Result<PullRequestSuggestion?> {
        lastFindSuggestionOrganization = organization
        lastFindSuggestionProject = projectName
        return Result.success(null)
    }

    override suspend fun createPullRequest(params: CreatePullRequestParams): Result<CreatedPullRequest> {
        lastCreateParams = params
        return Result.failure(NotImplementedError("test stub"))
    }

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

    override suspend fun abandonPullRequest(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
    ): Result<Unit> = Result.failure(NotImplementedError("unused"))

    override suspend fun enableAutoComplete(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
        mergeStrategy: PullRequestMergeStrategy,
    ): Result<Unit> = Result.failure(NotImplementedError("unused"))

    override suspend fun fetchAuthenticatedDevOpsResource(url: String): Result<ByteArray> =
        Result.failure(NotImplementedError("unused"))
}
