package dev.azure.desktop.ui.screens

import androidx.compose.runtime.Composable
import dev.azure.desktop.release.detail.ReleaseDetailState
import dev.azure.desktop.release.detail.ReleaseDetailStateMachine
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun ReleaseDetailScreenDesktop(
    stateMachine: ReleaseDetailStateMachine,
    state: ReleaseDetailState,
    scope: CoroutineScope,
    onBack: () -> Unit,
) {
    ReleaseDetailScreenContent(stateMachine, state, scope, onBack)
}
