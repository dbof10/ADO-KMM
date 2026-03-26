package dev.azure.desktop.ui.screens

import androidx.compose.runtime.Composable
import dev.azure.desktop.domain.release.CreateReleaseParams
import dev.azure.desktop.domain.release.CreatedRelease
import dev.azure.desktop.domain.release.ReleaseDefinitionDetail
import dev.azure.desktop.domain.release.ReleaseSummary
import dev.azure.desktop.release.list.ReleaseListState
import dev.azure.desktop.release.list.ReleaseListStateMachine
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun ReleaseListScreenMobile(
    organization: String,
    stateMachine: ReleaseListStateMachine,
    listState: ReleaseListState,
    onOpenRelease: (ReleaseSummary) -> Unit,
    getReleaseDefinition: suspend (String, Int) -> Result<ReleaseDefinitionDetail>,
    createRelease: suspend (CreateReleaseParams) -> Result<CreatedRelease>,
    scope: CoroutineScope,
) {
    ReleaseListScreenContent(
        organization = organization,
        stateMachine = stateMachine,
        listState = listState,
        onOpenRelease = onOpenRelease,
        getReleaseDefinition = getReleaseDefinition,
        createRelease = createRelease,
        scope = scope,
        compactLayout = true,
    )
}
