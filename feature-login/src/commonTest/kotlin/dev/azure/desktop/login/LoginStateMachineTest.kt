package dev.azure.desktop.login

import dev.azure.desktop.domain.auth.PatStorage
import dev.azure.desktop.domain.auth.PatVerifier
import dev.azure.desktop.domain.auth.VerifyAndStorePatUseCase
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LoginStateMachineTest {
    @Test
    fun submitPatEndsInSuccess() =
        runBlocking {
            val useCase =
                VerifyAndStorePatUseCase(
                    verifier = RecordingVerifier(Result.success(Unit)),
                    storage = RecordingStorage(),
                )
            val machine = LoginStateMachine(useCase)

            launch { machine.dispatch(LoginMachineAction.SubmitPat("org", "pat")) }

            val end = machine.state.first { it is LoginMachineState.Success }
            assertIs<LoginMachineState.Success>(end)
        }

    @Test
    fun submitPatReturnsToIdleWithErrorWhenVerificationFails() =
        runBlocking {
            val useCase =
                VerifyAndStorePatUseCase(
                    verifier = RecordingVerifier(Result.failure(IllegalStateException("bad token"))),
                    storage = RecordingStorage(),
                )
            val machine = LoginStateMachine(useCase)

            launch { machine.dispatch(LoginMachineAction.SubmitPat("org", "pat")) }

            val errIdle =
                machine.state
                    .filter { it is LoginMachineState.Idle && (it as LoginMachineState.Idle).error != null }
                    .first()
            assertEquals("bad token", (errIdle as LoginMachineState.Idle).error)
        }
}

private class RecordingVerifier(
    private val result: Result<Unit>,
) : PatVerifier {
    override suspend fun verify(organization: String, pat: String): Result<Unit> = result
}

private class RecordingStorage : PatStorage {
    override fun savePat(pat: String): Result<Unit> = Result.success(Unit)
    override fun loadPat(): String? = null
    override fun saveOrganization(name: String): Result<Unit> = Result.success(Unit)
    override fun loadOrganization(): String? = null
    override fun clearCredentials() = Unit
    override fun clearPatOnly() = Unit
}
