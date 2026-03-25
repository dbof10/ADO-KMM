package dev.azure.desktop.pr.detail

import com.freeletics.flowredux.dsl.FlowReduxStateMachine
import dev.azure.desktop.domain.pr.GetPullRequestDetailUseCase
import dev.azure.desktop.domain.pr.PullRequestDetail
import dev.azure.desktop.domain.pr.PullRequestSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi

sealed class PrDetailState {
    data object Loading : PrDetailState()

    data class Content(val detail: PullRequestDetail) : PrDetailState()

    data class Error(val message: String) : PrDetailState()
}

sealed class PrDetailAction {
    data object Refresh : PrDetailAction()
}

@OptIn(ExperimentalCoroutinesApi::class)
class PrDetailStateMachine(
    private val organization: String,
    private val summary: PullRequestSummary,
    private val getPullRequestDetailUseCase: GetPullRequestDetailUseCase,
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
            }
            inState<PrDetailState.Error> {
                on<PrDetailAction.Refresh> { _, state ->
                    state.override { PrDetailState.Loading }
                }
            }
        }
    }
}
