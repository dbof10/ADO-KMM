package dev.azure.desktop.pr.detail

import com.freeletics.flowredux.dsl.FlowReduxStateMachine
import dev.azure.desktop.domain.pr.AbandonPullRequestUseCase
import dev.azure.desktop.domain.pr.GetPullRequestDetailUseCase
import dev.azure.desktop.domain.pr.PullRequestDetail
import dev.azure.desktop.domain.pr.PullRequestSummary
import dev.azure.desktop.domain.pr.PullRequestReviewerVote
import dev.azure.desktop.domain.pr.SetMyPullRequestVoteUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi

sealed class PrDetailState {
    data object Loading : PrDetailState()

    data class Content(
        val detail: PullRequestDetail,
        val isVoting: Boolean = false,
        val voteErrorMessage: String? = null,
        val isClosing: Boolean = false,
        val closeErrorMessage: String? = null,
    ) : PrDetailState()

    data class Error(val message: String) : PrDetailState()
}

sealed class PrDetailAction {
    data object Refresh : PrDetailAction()
    data object Approve : PrDetailAction()
    data object Reject : PrDetailAction()
    data object Close : PrDetailAction()
}

@OptIn(ExperimentalCoroutinesApi::class)
class PrDetailStateMachine(
    private val organization: String,
    private val summary: PullRequestSummary,
    private val getPullRequestDetailUseCase: GetPullRequestDetailUseCase,
    private val setMyPullRequestVoteUseCase: SetMyPullRequestVoteUseCase,
    private val abandonPullRequestUseCase: AbandonPullRequestUseCase,
) : FlowReduxStateMachine<PrDetailState, PrDetailAction>(PrDetailState.Loading) {
    init {
        spec {
            inState<PrDetailState.Loading> {
                onEnter { state ->
                    getPullRequestDetailUseCase(
                        organization = organization,
                        projectName = summary.projectName,
                        repositoryId = summary.repositoryId,
                        pullRequestId = summary.id,
                    ).fold(
                        onSuccess = { state.override { PrDetailState.Content(it) } },
                        onFailure = {
                            state.override {
                                PrDetailState.Error(it.message ?: "Failed to load pull request detail.")
                            }
                        },
                    )
                }
                on<PrDetailAction.Refresh> { _, state ->
                    getPullRequestDetailUseCase(
                        organization = organization,
                        projectName = summary.projectName,
                        repositoryId = summary.repositoryId,
                        pullRequestId = summary.id,
                    ).fold(
                        onSuccess = { state.override { PrDetailState.Content(it) } },
                        onFailure = {
                            state.override {
                                PrDetailState.Error(it.message ?: "Failed to load pull request detail.")
                            }
                        },
                    )
                }
            }
            inState<PrDetailState.Content> {
                on<PrDetailAction.Refresh> { _, state ->
                    state.override { PrDetailState.Loading }
                }
                on<PrDetailAction.Approve> { action, state ->
                    return@on voteAndRefresh(state, vote = PullRequestReviewerVote.APPROVED)
                }
                on<PrDetailAction.Reject> { action, state ->
                    return@on voteAndRefresh(state, vote = PullRequestReviewerVote.REJECTED)
                }
                on<PrDetailAction.Close> { _, state ->
                    return@on abandonAndRefresh(state)
                }
            }
            inState<PrDetailState.Error> {
                on<PrDetailAction.Refresh> { _, state ->
                    state.override { PrDetailState.Loading }
                }
            }
        }
    }

    private suspend fun voteAndRefresh(
        state: com.freeletics.flowredux.dsl.State<PrDetailState.Content>,
        vote: Int,
    ): com.freeletics.flowredux.dsl.ChangedState<PrDetailState> {
        val current = state.snapshot
        if (current.isVoting || current.isClosing) return state.noChange()
        state.override { current.copy(isVoting = true, voteErrorMessage = null) }

        val org = organization.trim()
        val setVoteResult =
            setMyPullRequestVoteUseCase(
                organization = org,
                projectName = summary.projectName,
                repositoryId = summary.repositoryId,
                pullRequestId = summary.id,
                vote = vote,
            )
        if (setVoteResult.isFailure) {
            return state.override {
                current.copy(
                    isVoting = false,
                    voteErrorMessage = setVoteResult.exceptionOrNull()?.message ?: "Failed to update vote.",
                )
            }
        }

        return getPullRequestDetailUseCase(
            organization = org,
            projectName = summary.projectName,
            repositoryId = summary.repositoryId,
            pullRequestId = summary.id,
        ).fold(
            onSuccess = { updated ->
                state.override {
                    current.copy(
                        detail = updated,
                        isVoting = false,
                        voteErrorMessage = null,
                    )
                }
            },
            onFailure = {
                state.override {
                    current.copy(
                        isVoting = false,
                        voteErrorMessage = it.message ?: "Vote updated, but failed to refresh detail.",
                    )
                }
            },
        )
    }

    private suspend fun abandonAndRefresh(
        state: com.freeletics.flowredux.dsl.State<PrDetailState.Content>,
    ): com.freeletics.flowredux.dsl.ChangedState<PrDetailState> {
        val current = state.snapshot
        if (current.isClosing || current.isVoting) return state.noChange()
        if (!current.detail.summary.status.equals("active", ignoreCase = true)) return state.noChange()

        state.override { current.copy(isClosing = true, closeErrorMessage = null) }

        val org = organization.trim()
        val abandonResult =
            abandonPullRequestUseCase(
                organization = org,
                projectName = summary.projectName,
                repositoryId = summary.repositoryId,
                pullRequestId = summary.id,
            )
        if (abandonResult.isFailure) {
            return state.override {
                current.copy(
                    isClosing = false,
                    closeErrorMessage = abandonResult.exceptionOrNull()?.message ?: "Failed to close pull request.",
                )
            }
        }

        return getPullRequestDetailUseCase(
            organization = org,
            projectName = summary.projectName,
            repositoryId = summary.repositoryId,
            pullRequestId = summary.id,
        ).fold(
            onSuccess = { updated ->
                state.override {
                    current.copy(
                        detail = updated,
                        isClosing = false,
                        closeErrorMessage = null,
                    )
                }
            },
            onFailure = {
                state.override {
                    current.copy(
                        isClosing = false,
                        closeErrorMessage = it.message ?: "Pull request closed, but failed to refresh detail.",
                    )
                }
            },
        )
    }
}
