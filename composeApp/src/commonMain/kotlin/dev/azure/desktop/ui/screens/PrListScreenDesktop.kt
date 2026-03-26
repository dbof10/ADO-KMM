package dev.azure.desktop.ui.screens

import androidx.compose.runtime.Composable
import dev.azure.desktop.domain.pr.PullRequestSummary
import dev.azure.desktop.pr.list.PrListState
import dev.azure.desktop.pr.list.PrListStateMachine
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun PrListScreenDesktop(
    stateMachine: PrListStateMachine,
    state: PrListState,
    prNumberInput: String,
    onPrNumberInputChange: (String) -> Unit,
    onOpenPullRequest: (PullRequestSummary) -> Unit,
    scope: CoroutineScope,
) {
    PrListScreenContent(
        stateMachine = stateMachine,
        state = state,
        prNumberInput = prNumberInput,
        onPrNumberInputChange = onPrNumberInputChange,
        onOpenPullRequest = onOpenPullRequest,
        scope = scope,
        compactLayout = false,
    )
}
