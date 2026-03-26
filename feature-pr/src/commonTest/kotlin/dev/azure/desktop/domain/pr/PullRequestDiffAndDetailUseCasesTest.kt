package dev.azure.desktop.domain.pr

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PullRequestDiffAndDetailUseCasesTest {
    @Test
    fun computeDiffLinesMarksAddedAndRemovedLines() {
        val lines =
            computePullRequestDiffLines(
                oldLines = listOf("a", "b"),
                newLines = listOf("a", "c"),
            )

        assertEquals(3, lines.size)
        assertTrue(lines[1] is PullRequestDiffLine.Removed)
        assertTrue(lines[2] is PullRequestDiffLine.Added)
    }

    @Test
    fun fileDiffStatsCountsAdditionsAndRemovals() {
        val stats =
            computePullRequestFileLineDiffStats(
                oldLines = listOf("a", "b", "c"),
                newLines = listOf("a", "x", "c", "d"),
            )

        assertFalse(stats.truncated)
        assertEquals(2, stats.additions)
        assertEquals(1, stats.removals)
    }

    @Test
    fun fileDiffUseCaseLoadsBothCommitsAndComputesDiff() =
        runBlocking {
            val repository = DiffRepository()
            repository.fileByCommit["base"] = "same\nold"
            repository.fileByCommit["target"] = "same\nnew"

            val useCase = GetPullRequestFileDiffUseCase(repository)
            val result =
                useCase(
                    organization = "org",
                    projectName = "project",
                    repositoryId = "repo",
                    baseCommitId = "base",
                    targetCommitId = "target",
                    path = "/README.md",
                )

            assertTrue(result.isSuccess)
            assertEquals(3, result.getOrThrow().size)
        }

    @Test
    fun lineStatsUseCaseAggregatesAcrossFiles() =
        runBlocking {
            val repository = DiffRepository()
            repository.changes =
                listOf(
                    PullRequestChange(path = "/a.txt", changeType = "edit"),
                    PullRequestChange(path = "/b.txt", changeType = "edit"),
                )
            repository.fileByPathAndCommit["/a.txt|base"] = "a\nb"
            repository.fileByPathAndCommit["/a.txt|target"] = "a\nx"
            repository.fileByPathAndCommit["/b.txt|base"] = "k"
            repository.fileByPathAndCommit["/b.txt|target"] = "k\nn"

            val useCase = GetPullRequestLineStatsUseCase(repository)
            val result =
                useCase(
                    organization = " org ",
                    projectName = "project",
                    repositoryId = "repo",
                    pullRequestId = 1,
                    baseCommitId = "base",
                    targetCommitId = "target",
                )

            assertTrue(result.isSuccess)
            assertEquals(2, result.getOrThrow().additions)
            assertEquals(1, result.getOrThrow().removals)
        }

    @Test
    fun getPullRequestDetailAddsComputedLineStats() =
        runBlocking {
            val repository = DiffRepository()
            repository.detail =
                sampleDetail().copy(
                    lastMergeTargetCommitId = "base",
                    lastMergeSourceCommitId = "target",
                )
            repository.changes = listOf(PullRequestChange(path = "/a.txt", changeType = "edit"))
            repository.fileByPathAndCommit["/a.txt|base"] = "a"
            repository.fileByPathAndCommit["/a.txt|target"] = "a\nb"

            val detailUseCase = GetPullRequestDetailUseCase(repository, GetPullRequestLineStatsUseCase(repository))
            val result = detailUseCase(" org ", "project", "repo", 7)

            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrThrow().linesAdded)
            assertEquals(0, result.getOrThrow().linesRemoved)
        }
}

private class DiffRepository : PullRequestRepository {
    var detail: PullRequestDetail = sampleDetail()
    var changes: List<PullRequestChange> = emptyList()
    val fileByCommit = mutableMapOf<String, String?>()
    val fileByPathAndCommit = mutableMapOf<String, String?>()

    override suspend fun listProjects(organization: String): Result<List<DevOpsProject>> = Result.failure(NotImplementedError("unused"))

    override suspend fun getMyPullRequests(
        organization: String,
        projectName: String?,
    ): Result<List<PullRequestSummary>> = Result.failure(NotImplementedError("unused"))

    override suspend fun getActivePullRequests(
        organization: String,
        projectName: String?,
    ): Result<List<PullRequestSummary>> = Result.failure(NotImplementedError("unused"))

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
    ): Result<PullRequestDetail> = Result.success(detail)

    override suspend fun getPullRequestChanges(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
        baseCommitId: String,
        targetCommitId: String,
    ): Result<List<PullRequestChange>> = Result.success(changes)

    override suspend fun getFileContentAtCommit(
        organization: String,
        projectName: String,
        repositoryId: String,
        path: String,
        commitId: String,
    ): Result<String?> {
        val key = "$path|$commitId"
        return if (fileByPathAndCommit.containsKey(key)) {
            Result.success(fileByPathAndCommit[key])
        } else {
            Result.success(fileByCommit[commitId])
        }
    }

    override suspend fun setMyPullRequestVote(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
        vote: Int,
    ): Result<Unit> = Result.failure(NotImplementedError("unused"))
}

private fun sampleDetail(): PullRequestDetail =
    PullRequestDetail(
        summary =
            PullRequestSummary(
                id = 7,
                title = "PR",
                status = "active",
                creatorDisplayName = "Dev",
                creatorUniqueName = null,
                sourceRefName = "refs/heads/source",
                targetRefName = "refs/heads/main",
                repositoryName = "repo",
                repositoryId = "repo",
                projectName = "project",
                projectId = "project",
                creationDateIso = null,
            ),
        description = null,
        reviewers = emptyList(),
        timeline = emptyList(),
        linkedWorkItems = emptyList(),
        checks = emptyList(),
        lastMergeSourceCommitId = null,
        lastMergeTargetCommitId = null,
    )
