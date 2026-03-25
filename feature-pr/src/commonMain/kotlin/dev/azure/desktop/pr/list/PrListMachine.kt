package dev.azure.desktop.pr.list

import com.freeletics.flowredux.dsl.FlowReduxStateMachine
import dev.azure.desktop.domain.pr.GetMyPullRequestsUseCase
import dev.azure.desktop.domain.pr.PullRequestSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi

sealed class PrListState {
    data object Loading : PrListState()

    data class Content(val items: List<PullRequestSummary>) : PrListState()

    data class Error(val message: String) : PrListState()
}

sealed class PrListAction {
    data object Refresh : PrListAction()
}

@OptIn(ExperimentalCoroutinesApi::class)
class PrListStateMachine(
    private val organization: String,
    private val getMyPullRequestsUseCase: GetMyPullRequestsUseCase,
) : FlowReduxStateMachine<PrListState, PrListAction>(PrListState.Loading) {
    init {
        spec {
            inState<PrListState.Loading> {
                onEnter { state ->
                    getMyPullRequestsUseCase(organization).fold(
                        onSuccess = { state.override { PrListState.Content(it) } },
                        onFailure = {
                            state.override {
                                PrListState.Error(it.message ?: "Failed to load pull requests.")
                            }
                        },
                    )
                }
                on<PrListAction.Refresh> { _, state ->
                    getMyPullRequestsUseCase(organization).fold(
                        onSuccess = { state.override { PrListState.Content(it) } },
                        onFailure = {
                            state.override {
                                PrListState.Error(it.message ?: "Failed to load pull requests.")
                            }
                        },
                    )
                }
            }
            inState<PrListState.Content> {
                on<PrListAction.Refresh> { _, state ->
                    state.override { PrListState.Loading }
                }
            }
            inState<PrListState.Error> {
                on<PrListAction.Refresh> { _, state ->
                    state.override { PrListState.Loading }
                }
            }
        }
    }
}
