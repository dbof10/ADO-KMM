package dev.azure.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import dev.azure.desktop.ui.components.MascotLoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

@Composable
internal fun ReleaseListLayoutMobile(
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
    var mainTab by remember { mutableIntStateOf(0) }
    val selectedDefinitionName = definitions.firstOrNull { it.id == selectedDefinitionId }?.name ?: "Select pipeline"
    var definitionMenuExpanded by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ProjectStrip(
                compactLayout = true,
                projects = projects,
                selectedProjectName = selectedProjectName,
                onSelectProject = onSelectProject,
                onRefresh = onRefresh,
            )
            Spacer(Modifier.height(12.dp))
            Surface(
                Modifier.fillMaxWidth(),
                color = EditorialColors.surfaceContainerLowest,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { definitionMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                        ) {
                            Text(selectedDefinitionName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        DropdownMenu(
                            expanded = definitionMenuExpanded,
                            onDismissRequest = { definitionMenuExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.92f),
                        ) {
                            definitions.forEach { definition ->
                                DropdownMenuItem(
                                    text = { Text(definition.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    onClick = {
                                        definitionMenuExpanded = false
                                        onSelectDefinition(definition.id)
                                    },
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                Modifier.fillMaxWidth().weight(1f),
                color = EditorialColors.surfaceContainerLowest,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            ) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    val defName = definitions.firstOrNull { it.id == selectedDefinitionId }?.name.orEmpty()
                    Text(
                        defName.ifBlank { "Pipelines" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    TabRow(
                        selectedTabIndex = mainTab,
                        containerColor = EditorialColors.surfaceContainerLowest,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[mainTab]).height(2.dp),
                                color = EditorialColors.primary,
                            )
                        },
                        divider = { },
                    ) {
                        Tab(
                            selected = mainTab == 0,
                            onClick = { mainTab = 0 },
                            text = { Text("Releases", fontWeight = if (mainTab == 0) FontWeight.Bold else FontWeight.Medium) },
                        )
                        Tab(
                            selected = mainTab == 1,
                            onClick = { mainTab = 1 },
                            text = { Text("Deployments", fontWeight = if (mainTab == 1) FontWeight.Bold else FontWeight.Medium) },
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    when (mainTab) {
                        0 -> {
                            if (listBusy) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { MascotLoadingIndicator() }
                            } else if (releases.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No releases yet.", color = EditorialColors.onSurfaceVariant)
                                }
                            } else {
                                ReleaseTable(releases = releases, onRowClick = onOpenRelease, compactLayout = true)
                            }
                        }
                        else -> {
                            if (listBusy) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { MascotLoadingIndicator() }
                            } else if (releases.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No deployments — no releases loaded.", color = EditorialColors.onSurfaceVariant)
                                }
                            } else {
                                DeploymentsFromReleasesTable(
                                    releases = releases,
                                    onOpenRelease = onOpenRelease,
                                    compactLayout = true,
                                )
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onNewRelease,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = EditorialColors.primary,
            contentColor = EditorialColors.onPrimary,
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "New release")
        }
    }
}
