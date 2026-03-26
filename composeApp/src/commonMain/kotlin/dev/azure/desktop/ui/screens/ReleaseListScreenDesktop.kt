package dev.azure.desktop.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.azure.desktop.domain.pr.DevOpsProject
import dev.azure.desktop.domain.release.CreateReleaseParams
import dev.azure.desktop.domain.release.CreatedRelease
import dev.azure.desktop.domain.release.ReleaseDefinitionDetail
import dev.azure.desktop.domain.release.ReleaseDefinitionSummary
import dev.azure.desktop.domain.release.ReleaseSummary
import dev.azure.desktop.release.list.ReleaseListState
import dev.azure.desktop.release.list.ReleaseListStateMachine
import dev.azure.desktop.theme.EditorialColors
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun ReleaseListScreenDesktop(
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
        compactLayout = false,
    )
}

@Composable
internal fun ReleaseListLayoutDesktop(
    projects: List<DevOpsProject>,
    selectedProjectName: String,
    onSelectProject: (String) -> Unit,
    definitions: List<ReleaseDefinitionSummary>,
    selectedDefinitionId: Int?,
    onSelectDefinition: (Int) -> Unit,
    releases: List<ReleaseSummary>,
    listBusy: Boolean,
    onRefresh: () -> Unit,
    onOpenRelease: (ReleaseSummary) -> Unit,
    onNewRelease: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        ProjectStrip(
            compactLayout = false,
            projects = projects,
            selectedProjectName = selectedProjectName,
            onSelectProject = onSelectProject,
            onRefresh = onRefresh,
        )
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxSize()) {
            Surface(
                Modifier.width(320.dp).fillMaxHeight(),
                color = EditorialColors.surfaceContainerLowest,
                shape = RoundedCornerShape(12.dp),
            ) {
                ReleaseLeftRail(
                    definitions = definitions,
                    selectedDefinitionId = selectedDefinitionId,
                    onSelectDefinition = onSelectDefinition,
                    onNewRelease = onNewRelease,
                )
            }
            Spacer(Modifier.width(16.dp))
            Surface(
                Modifier.weight(1f).fillMaxHeight(),
                color = EditorialColors.surfaceContainerLowest,
                shape = RoundedCornerShape(12.dp),
            ) {
                ReleaseMainPanel(
                    definitions = definitions,
                    selectedDefinitionId = selectedDefinitionId,
                    releases = releases,
                    busy = listBusy,
                    onOpenRelease = onOpenRelease,
                    compactLayout = false,
                )
            }
        }
    }
}
