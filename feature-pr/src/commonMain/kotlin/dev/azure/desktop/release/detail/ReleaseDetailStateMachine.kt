package dev.azure.desktop.release.detail

import com.freeletics.flowredux.dsl.FlowReduxStateMachine
import dev.azure.desktop.domain.release.GetReleaseDetailUseCase
import dev.azure.desktop.domain.release.ReleaseDetail
import kotlinx.coroutines.ExperimentalCoroutinesApi

sealed class ReleaseDetailState {
    data object Loading : ReleaseDetailState()

    data class Error(val message: String) : ReleaseDetailState()

    data class Content(val detail: ReleaseDetail) : ReleaseDetailState()
}

sealed class ReleaseDetailAction {
    data object Reload : ReleaseDetailAction()
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReleaseDetailStateMachine(
    private val organization: String,
    private val projectName: String,
    private val releaseId: Int,
    private val getReleaseDetailUseCase: GetReleaseDetailUseCase,
) : FlowReduxStateMachine<ReleaseDetailState, ReleaseDetailAction>(ReleaseDetailState.Loading) {
    init {
        spec {
            inState<ReleaseDetailState.Loading> {
                onEnter { state ->
                    getReleaseDetailUseCase(organization, projectName, releaseId).fold(
                        onSuccess = { state.override { ReleaseDetailState.Content(it) } },
                        onFailure = {
                            state.override {
                                ReleaseDetailState.Error(it.message ?: "Failed to load release.")
                            }
                        },
                    )
                }
            }

            inState<ReleaseDetailState.Error> {
                on<ReleaseDetailAction.Reload> { _, state ->
                    state.override { ReleaseDetailState.Loading }
                }
            }

            inState<ReleaseDetailState.Content> {
                on<ReleaseDetailAction.Reload> { _, state ->
                    state.override { ReleaseDetailState.Loading }
                }
            }
        }
    }
}
