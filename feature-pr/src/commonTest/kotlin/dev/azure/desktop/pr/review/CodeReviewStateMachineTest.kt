package dev.azure.desktop.pr.review

import dev.azure.desktop.domain.pr.GetPullRequestDetailUseCase
import dev.azure.desktop.domain.pr.GetPullRequestFileDiffUseCase
import dev.azure.desktop.domain.pr.GetPullRequestLineStatsUseCase
import dev.azure.desktop.domain.pr.PullRequestChange
import dev.azure.desktop.pr.StubPullRequestRepository
import dev.azure.desktop.pr.samplePullRequestDetail
import dev.azure.desktop.pr.samplePullRequestSummary
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CodeReviewStateMachineTest {
    @Test
    fun initialLoadSelectsFirstFileWithDiff() =
        runBlocking {
            val repo = codeReviewRepo()
            val machine = createMachine(repo)

            val content = machine.state.first { it is CodeReviewState.Content }
            val c = assertIs<CodeReviewState.Content>(content)
            assertEquals("src/A.kt", c.selectedPath)
            assertEquals(false, c.isDiffLoading)
            assertEquals(2, c.changes.size)
        }

    @Test
    fun selectFileLoadsDiffAsynchronously() =
        runBlocking {
            val repo = codeReviewRepo()
            val machine = createMachine(repo)
            machine.state.first { it is CodeReviewState.Content }

            launch { machine.dispatch(CodeReviewAction.SelectFile("src/B.kt")) }

            val loaded =
                machine.state
                    .filter {
                        it is CodeReviewState.Content &&
                            (it as CodeReviewState.Content).selectedPath == "src/B.kt" &&
                            !it.isDiffLoading &&
                            it.diffLines.isNotEmpty()
                    }.first()
            assertIs<CodeReviewState.Content>(loaded)
        }

    @Test
    fun refreshReturnsToContent() =
        runBlocking {
            val repo = codeReviewRepo()
            val machine = createMachine(repo)
            machine.state.first { it is CodeReviewState.Content }

            launch { machine.dispatch(CodeReviewAction.Refresh) }

            machine.state.first { it is CodeReviewState.Loading }
            val again = machine.state.first { it is CodeReviewState.Content }
            assertIs<CodeReviewState.Content>(again)
        }

    private fun createMachine(repo: StubPullRequestRepository): CodeReviewStateMachine {
        val summary = samplePullRequestSummary()
        val lineStats = GetPullRequestLineStatsUseCase(repo)
        val detailUseCase = GetPullRequestDetailUseCase(repo, lineStats)
        val diffUseCase = GetPullRequestFileDiffUseCase(repo)
        val changes =
            listOf(
                PullRequestChange("src/A.kt", "edit", false),
                PullRequestChange("src/B.kt", "edit", false),
            )
        return CodeReviewStateMachine(
            organization = "org",
            summary = summary,
            getPullRequestDetailUseCase = detailUseCase,
            getPullRequestFileDiffUseCase = diffUseCase,
            getPullRequestChanges = { _, _, _, _, _, _ -> Result.success(changes) },
        )
    }

    private fun codeReviewRepo(): StubPullRequestRepository {
        val base = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
        val target = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        return object : StubPullRequestRepository() {
            init {
                detailResult =
                    Result.success(
                        samplePullRequestDetail(
                            summary = samplePullRequestSummary(),
                            lastMergeTargetCommitId = base,
                            lastMergeSourceCommitId = target,
                        ),
                    )
                changesResult =
                    Result.success(
                        listOf(
                            PullRequestChange("src/A.kt", "edit", false),
                            PullRequestChange("src/B.kt", "edit", false),
                        ),
                    )
            }

            override suspend fun getFileContentAtCommit(
                organization: String,
                projectName: String,
                repositoryId: String,
                path: String,
                commitId: String,
            ): Result<String?> =
                when (commitId) {
                    base ->
                        when {
                            path.endsWith("A.kt") -> Result.success("common")
                            path.endsWith("B.kt") -> Result.success("oldB")
                            else -> Result.success("")
                        }
                    target ->
                        when {
                            path.endsWith("A.kt") -> Result.success("common\nleft")
                            path.endsWith("B.kt") -> Result.success("oldB\nnewB")
                            else -> Result.success("")
                        }
                    else -> Result.success("")
                }
        }
    }
}
