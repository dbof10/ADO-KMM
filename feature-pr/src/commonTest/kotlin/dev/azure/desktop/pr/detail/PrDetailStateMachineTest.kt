package dev.azure.desktop.pr.detail

import dev.azure.desktop.domain.pr.AbandonPullRequestUseCase
import dev.azure.desktop.domain.pr.GetPullRequestDetailUseCase
import dev.azure.desktop.domain.pr.GetPullRequestLineStatsUseCase
import dev.azure.desktop.domain.pr.PullRequestSummary
import dev.azure.desktop.domain.pr.SetMyPullRequestVoteUseCase
import dev.azure.desktop.pr.StubPullRequestRepository
import dev.azure.desktop.pr.samplePullRequestDetail
import dev.azure.desktop.pr.samplePullRequestSummary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
class PrDetailStateMachineTest {
    @Test
    fun initialLoadShowsContent() {
        runBlocking {
            val summary = samplePullRequestSummary()
            val detail = samplePullRequestDetail(summary = summary)
            val repo =
                StubPullRequestRepository().apply {
                    detailResult = Result.success(detail)
                }
            val machine = createMachine(repo, summary)

            val content = machine.state.first { it is PrDetailState.Content }
            assertEquals(detail, assertIs<PrDetailState.Content>(content).detail)
        }
    }

    @Test
    fun initialLoadFailureShowsError() {
        runBlocking {
            val summary = samplePullRequestSummary()
            val repo =
                StubPullRequestRepository().apply {
                    detailResult = Result.failure(Exception("missing"))
                }
            val machine = createMachine(repo, summary)

            val err = machine.state.first { it is PrDetailState.Error }
            assertEquals("missing", assertIs<PrDetailState.Error>(err).message)
        }
    }

    @Test
    fun approveRefreshesDetailAfterVote() {
        runBlocking {
            val summary = samplePullRequestSummary()
            val initial = samplePullRequestDetail(summary = summary)
            val refreshed =
                samplePullRequestDetail(
                    summary = summary.copy(title = "Updated"),
                )
            val repo =
                StubPullRequestRepository().apply {
                    detailResult = Result.success(initial)
                }
            val machine = createMachine(repo, summary)
            var dispatched = false
            val end =
                machine.state
                    .onEach { s ->
                        if (s is PrDetailState.Content) {
                            val c = s as PrDetailState.Content
                            if (!dispatched && !c.isVoting && c.detail.summary.title != "Updated") {
                                dispatched = true
                                repo.detailResult = Result.success(refreshed)
                                launch { machine.dispatch(PrDetailAction.Approve) }
                            }
                        }
                    }.first {
                        it is PrDetailState.Content &&
                            !(it as PrDetailState.Content).isVoting &&
                            (it as PrDetailState.Content).detail.summary.title == "Updated"
                    }
            assertIs<PrDetailState.Content>(end)
        }
    }

    @Test
    fun voteFailureShowsVoteError() {
        runBlocking {
            val summary = samplePullRequestSummary()
            val detail = samplePullRequestDetail(summary = summary)
            val repo =
                StubPullRequestRepository().apply {
                    detailResult = Result.success(detail)
                    setVoteResult = Result.failure(Exception("no permission"))
                }
            val machine = createMachine(repo, summary)
            var dispatched = false
            val withErr =
                machine.state
                    .onEach { s ->
                        if (s is PrDetailState.Content) {
                            val c = s as PrDetailState.Content
                            if (!dispatched && c.voteErrorMessage == null && !c.isVoting) {
                                dispatched = true
                                launch { machine.dispatch(PrDetailAction.Reject) }
                            }
                        }
                    }.first {
                        it is PrDetailState.Content &&
                            (it as PrDetailState.Content).voteErrorMessage != null
                    }
            assertEquals("no permission", assertIs<PrDetailState.Content>(withErr).voteErrorMessage)
        }
    }

    @Test
    fun reloadFromErrorReturnsToContent() {
        runBlocking {
            val summary = samplePullRequestSummary()
            val ok = samplePullRequestDetail(summary = summary)
            val repo =
                StubPullRequestRepository().apply {
                    detailResult = Result.failure(Exception("first"))
                }
            val machine = createMachine(repo, summary)
            var dispatched = false
            val content =
                machine.state
                    .onEach { s ->
                        if (s is PrDetailState.Error && !dispatched) {
                            dispatched = true
                            repo.detailResult = Result.success(ok)
                            launch { machine.dispatch(PrDetailAction.Refresh) }
                        }
                    }.first { it is PrDetailState.Content }
            assertIs<PrDetailState.Content>(content)
        }
    }

    private fun createMachine(
        repo: StubPullRequestRepository,
        summary: PullRequestSummary,
    ): PrDetailStateMachine {
        val lineStats = GetPullRequestLineStatsUseCase(repo)
        val getDetail = GetPullRequestDetailUseCase(repo, lineStats)
        return PrDetailStateMachine(
            organization = "org",
            summary = summary,
            getPullRequestDetailUseCase = getDetail,
            setMyPullRequestVoteUseCase = SetMyPullRequestVoteUseCase(repo),
            abandonPullRequestUseCase = AbandonPullRequestUseCase(repo),
        )
    }
}
