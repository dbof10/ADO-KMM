package dev.azure.desktop.login

sealed class LoginMachineState {
    data class Idle(val error: String?) : LoginMachineState()

    data class Working(val organization: String, val pat: String) : LoginMachineState()

    data object Success : LoginMachineState()
}

sealed class LoginMachineAction {
    data class SubmitPat(val organization: String, val pat: String) : LoginMachineAction()
}
