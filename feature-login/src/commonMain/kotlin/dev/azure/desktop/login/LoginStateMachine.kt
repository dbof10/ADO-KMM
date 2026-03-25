package dev.azure.desktop.login

import com.freeletics.flowredux.dsl.FlowReduxStateMachine
import dev.azure.desktop.domain.auth.VerifyAndStorePatUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class LoginStateMachine(
    private val verifyAndStorePat: VerifyAndStorePatUseCase,
) : FlowReduxStateMachine<LoginMachineState, LoginMachineAction>(LoginMachineState.Idle(null)) {

    init {
        spec {
            inState<LoginMachineState.Idle> {
                on<LoginMachineAction.SubmitPat> { action, state ->
                    state.override { LoginMachineState.Working(action.organization, action.pat) }
                }
            }
            inState<LoginMachineState.Working> {
                onEnter { state ->
                    verifyAndStorePat(state.snapshot.organization, state.snapshot.pat).fold(
                        onSuccess = { state.override { LoginMachineState.Success } },
                        onFailure = { e ->
                            val message = e.message?.takeIf { it.isNotBlank() } ?: "Sign-in failed."
                            state.override { LoginMachineState.Idle(message) }
                        },
                    )
                }
            }
        }
    }
}
