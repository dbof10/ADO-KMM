package dev.azure.desktop.release.detail

import com.freeletics.flowredux.dsl.FlowReduxStateMachine
import dev.azure.desktop.domain.release.DeployReleaseEnvironmentUseCase
import dev.azure.desktop.domain.release.GetReleaseDetailUseCase
import dev.azure.desktop.domain.release.ReleaseDetail
import kotlinx.coroutines.ExperimentalCoroutinesApi

sealed class ReleaseDetailState {
    data object Loading : ReleaseDetailState()

    data class Error(val message: String) : ReleaseDetailState()

    data class Content(
        val detail: ReleaseDetail,
        val isDeploying: Boolean = false,
        val deployError: String? = null,
    ) : ReleaseDetailState()
}

sealed class ReleaseDetailAction {
    data object Reload : ReleaseDetailAction()

    data class DeployEnvironment(val environmentId: Int) : ReleaseDetailAction()
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReleaseDetailStateMachine(
    private val organization: String,
    private val projectName: String,
    private val releaseId: Int,
    private val getReleaseDetailUseCase: GetReleaseDetailUseCase,
    private val deployReleaseEnvironmentUseCase: DeployReleaseEnvironmentUseCase,
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
                on<ReleaseDetailAction.DeployEnvironment> { action, state ->
                    deployEnvironment(state, action.environmentId)
                }
            }
        }
    }

    private suspend fun deployEnvironment(
        state: com.freeletics.flowredux.dsl.State<ReleaseDetailState.Content>,
        environmentId: Int,
    ): com.freeletics.flowredux.dsl.ChangedState<ReleaseDetailState> {
        val current = state.snapshot
        if (current.isDeploying) return state.noChange()
        state.override { current.copy(isDeploying = true, deployError = null) }

        val org = organization.trim()
        val deployResult =
            deployReleaseEnvironmentUseCase(
                organization = org,
                projectName = projectName,
                releaseId = releaseId,
                environmentId = environmentId,
            )
        if (deployResult.isFailure) {
            return state.override {
                current.copy(
                    isDeploying = false,
                    deployError = deployResult.exceptionOrNull()?.message ?: "Deploy failed.",
                )
            }
        }

        return getReleaseDetailUseCase(org, projectName, releaseId).fold(
            onSuccess = { updated ->
                state.override {
                    current.copy(
                        detail = updated,
                        isDeploying = false,
                        deployError = null,
                    )
                }
            },
            onFailure = {
                state.override {
                    current.copy(
                        isDeploying = false,
                        deployError = it.message ?: "Deploy started, but failed to refresh release.",
                    )
                }
            },
        )
    }
}
