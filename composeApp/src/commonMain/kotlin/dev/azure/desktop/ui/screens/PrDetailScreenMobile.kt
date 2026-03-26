package dev.azure.desktop.ui.screens

import androidx.compose.runtime.Composable
import dev.azure.desktop.pr.detail.PrDetailState
import dev.azure.desktop.pr.detail.PrDetailStateMachine
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun PrDetailScreenMobile(
    stateMachine: PrDetailStateMachine,
    state: PrDetailState,
    onBack: () -> Unit,
    scope: CoroutineScope,
) {
    PrDetailScreenContent(stateMachine, state, onBack, scope)
}
