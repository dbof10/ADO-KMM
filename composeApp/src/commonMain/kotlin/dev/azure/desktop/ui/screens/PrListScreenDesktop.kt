package dev.azure.desktop.ui.screens

import androidx.compose.runtime.Composable
import dev.azure.desktop.domain.pr.CreatePullRequestParams
import dev.azure.desktop.domain.pr.CreatedPullRequest
import dev.azure.desktop.domain.pr.PullRequestRepositoryRef
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
    organization: String,
    showCreatePrDialog: Boolean,
    onOpenCreatePr: () -> Unit,
    onDismissCreatePr: () -> Unit,
    listPullRequestRepositories: suspend (organization: String, projectName: String) -> Result<List<PullRequestRepositoryRef>>,
    createPullRequest: suspend (CreatePullRequestParams) -> Result<CreatedPullRequest>,
    onOpenPullRequest: (PullRequestSummary) -> Unit,
    scope: CoroutineScope,
) {
    PrListScreenContent(
        stateMachine = stateMachine,
        state = state,
        prNumberInput = prNumberInput,
        onPrNumberInputChange = onPrNumberInputChange,
        organization = organization,
        showCreatePrDialog = showCreatePrDialog,
        onOpenCreatePr = onOpenCreatePr,
        onDismissCreatePr = onDismissCreatePr,
        listPullRequestRepositories = listPullRequestRepositories,
        createPullRequest = createPullRequest,
        onOpenPullRequest = onOpenPullRequest,
        scope = scope,
        compactLayout = false,
    )
}
