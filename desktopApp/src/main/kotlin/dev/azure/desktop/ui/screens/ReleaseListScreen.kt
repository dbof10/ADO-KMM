package dev.azure.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.azure.desktop.domain.pr.DevOpsProject
import dev.azure.desktop.domain.release.CreateReleaseParams
import dev.azure.desktop.domain.release.CreatedRelease
import dev.azure.desktop.domain.release.ReleaseDefinitionDetail
import dev.azure.desktop.domain.release.ReleaseDefinitionSummary
import dev.azure.desktop.domain.release.ReleaseDeploymentStatus
import dev.azure.desktop.domain.release.ReleaseStagePill
import dev.azure.desktop.domain.release.ReleaseSummary
import dev.azure.desktop.release.list.ReleaseListAction
import dev.azure.desktop.release.list.ReleaseListState
import dev.azure.desktop.release.list.ReleaseListStateMachine
import dev.azure.desktop.theme.EditorialColors
import kotlinx.coroutines.launch

@Composable
fun ReleaseListScreen(
    organization: String,
    stateMachine: ReleaseListStateMachine,
    listState: ReleaseListState,
    onOpenRelease: (ReleaseSummary) -> Unit,
    getReleaseDefinition: suspend (String, Int) -> Result<ReleaseDefinitionDetail>,
    createRelease: suspend (CreateReleaseParams) -> Result<CreatedRelease>,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    Surface(modifier.fillMaxSize(), color = EditorialColors.surfaceContainerLow) {
        when (val current = listState) {
            ReleaseListState.LoadingProjects ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text("Loading projects…", color = EditorialColors.onSurfaceVariant)
                    }
                }

            is ReleaseListState.ProjectsError ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(current.message, color = EditorialColors.error)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { scope.launch { stateMachine.dispatch(ReleaseListAction.RetryProjects) } }) {
                            Text("Retry")
                        }
                    }
                }

            is ReleaseListState.LoadingDefinitions ->
                ReleaseListLayout(
                    organization = organization,
                    projects = current.projects,
                    selectedProjectName = current.selectedProjectName,
                    onSelectProject = { scope.launch { stateMachine.dispatch(ReleaseListAction.SelectProject(it)) } },
                    definitions = emptyList(),
                    selectedDefinitionId = null,
                    onSelectDefinition = { },
                    releases = emptyList(),
                    listBusy = true,
                    onRefresh = { scope.launch { stateMachine.dispatch(ReleaseListAction.Refresh) } },
                    onOpenRelease = onOpenRelease,
                    getReleaseDefinition = getReleaseDefinition,
                    createRelease = createRelease,
                )

            is ReleaseListState.DefinitionsError ->
                Column(Modifier.fillMaxSize().padding(24.dp)) {
                    ProjectStrip(
                        projects = current.projects,
                        selectedProjectName = current.selectedProjectName,
                        onSelectProject = { scope.launch { stateMachine.dispatch(ReleaseListAction.SelectProject(it)) } },
                        onRefresh = { scope.launch { stateMachine.dispatch(ReleaseListAction.Refresh) } },
                    )
                    Spacer(Modifier.height(24.dp))
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            current.message,
                            color = EditorialColors.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { scope.launch { stateMachine.dispatch(ReleaseListAction.Refresh) } }) {
                            Text("Retry")
                        }
                    }
                }

            is ReleaseListState.LoadingReleases,
            is ReleaseListState.Ready,
            -> {
                // Single branch keeps ReleaseListLayout in the same composition slot while
                // reloading releases (Ready → LoadingReleases → Ready), so the pipeline list
                // scroll position and search query are preserved.
                val projects =
                    when (current) {
                        is ReleaseListState.LoadingReleases -> current.projects
                        is ReleaseListState.Ready -> current.projects
                        else -> error("unreachable")
                    }
                val selectedProjectName =
                    when (current) {
                        is ReleaseListState.LoadingReleases -> current.selectedProjectName
                        is ReleaseListState.Ready -> current.selectedProjectName
                        else -> error("unreachable")
                    }
                val definitions =
                    when (current) {
                        is ReleaseListState.LoadingReleases -> current.definitions
                        is ReleaseListState.Ready -> current.definitions
                        else -> error("unreachable")
                    }
                val selectedDefinitionId =
                    when (current) {
                        is ReleaseListState.LoadingReleases -> current.selectedDefinitionId
                        is ReleaseListState.Ready -> current.selectedDefinitionId
                        else -> error("unreachable")
                    }
                val releases =
                    when (current) {
                        is ReleaseListState.LoadingReleases -> emptyList()
                        is ReleaseListState.Ready -> current.releases
                        else -> error("unreachable")
                    }
                val listBusy = current is ReleaseListState.LoadingReleases
                ReleaseListLayout(
                    organization = organization,
                    projects = projects,
                    selectedProjectName = selectedProjectName,
                    onSelectProject = { scope.launch { stateMachine.dispatch(ReleaseListAction.SelectProject(it)) } },
                    definitions = definitions,
                    selectedDefinitionId = selectedDefinitionId,
                    onSelectDefinition = {
                        scope.launch { stateMachine.dispatch(ReleaseListAction.SelectDefinition(it)) }
                    },
                    releases = releases,
                    listBusy = listBusy,
                    onRefresh = { scope.launch { stateMachine.dispatch(ReleaseListAction.Refresh) } },
                    onOpenRelease = onOpenRelease,
                    getReleaseDefinition = getReleaseDefinition,
                    createRelease = createRelease,
                )
            }

            is ReleaseListState.ReleasesError ->
                Column(Modifier.fillMaxSize().padding(24.dp)) {
                    ProjectStrip(
                        projects = current.projects,
                        selectedProjectName = current.selectedProjectName,
                        onSelectProject = { scope.launch { stateMachine.dispatch(ReleaseListAction.SelectProject(it)) } },
                        onRefresh = { scope.launch { stateMachine.dispatch(ReleaseListAction.Refresh) } },
                    )
                    Spacer(Modifier.height(16.dp))
                    DefinitionSidebarOnly(
                        definitions = current.definitions,
                        selectedDefinitionId = current.selectedDefinitionId,
                        onSelectDefinition = {
                            scope.launch { stateMachine.dispatch(ReleaseListAction.SelectDefinition(it)) }
                        },
                    )
                    Spacer(Modifier.height(16.dp))
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            current.message,
                            color = EditorialColors.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { scope.launch { stateMachine.dispatch(ReleaseListAction.Refresh) } }) {
                            Text("Retry")
                        }
                    }
                }

        }
    }
}

@Composable
private fun ReleaseListLayout(
    organization: String,
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
    getReleaseDefinition: suspend (String, Int) -> Result<ReleaseDefinitionDetail>,
    createRelease: suspend (CreateReleaseParams) -> Result<CreatedRelease>,
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().padding(24.dp)) {
        Column(Modifier.fillMaxSize()) {
            ProjectStrip(
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
                        onNewRelease = {
                            if (selectedDefinitionId != null) showCreateDialog = true
                        },
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
                    )
                }
            }
        }

        val defId = selectedDefinitionId
        if (defId != null) {
            CreateReleaseDialog(
                visible = showCreateDialog,
                organization = organization,
                projectName = selectedProjectName,
                definitionId = defId,
                getReleaseDefinition = getReleaseDefinition,
                createRelease = createRelease,
                onDismiss = { showCreateDialog = false },
                onCreated = onRefresh,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ProjectStrip(
    projects: List<DevOpsProject>,
    selectedProjectName: String,
    onSelectProject: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Releases", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        ProjectPickerOnly(
            projects = projects,
            selectedProjectName = selectedProjectName,
            onSelect = onSelectProject,
        )
        OutlinedButton(onClick = onRefresh, shape = RoundedCornerShape(10.dp)) {
            Text("Refresh")
        }
    }
}

@Composable
private fun DefinitionSidebarOnly(
    definitions: List<ReleaseDefinitionSummary>,
    selectedDefinitionId: Int,
    onSelectDefinition: (Int) -> Unit,
) {
    Row(Modifier.fillMaxWidth()) {
        Surface(
            Modifier.width(320.dp).height(400.dp),
            color = EditorialColors.surfaceContainerLowest,
            shape = RoundedCornerShape(12.dp),
        ) {
            ReleaseLeftRail(
                definitions = definitions,
                selectedDefinitionId = selectedDefinitionId,
                onSelectDefinition = onSelectDefinition,
                onNewRelease = { },
            )
        }
    }
}

@Composable
private fun ProjectPickerOnly(
    projects: List<DevOpsProject>,
    selectedProjectName: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.width(220.dp)) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(40.dp),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(selectedProjectName, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(280.dp),
        ) {
            projects.forEach { p ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(p.name) },
                    onClick = {
                        expanded = false
                        onSelect(p.name)
                    },
                )
            }
        }
    }
}

@Composable
private fun ReleaseLeftRail(
    definitions: List<ReleaseDefinitionSummary>,
    selectedDefinitionId: Int?,
    onSelectDefinition: (Int) -> Unit,
    onNewRelease: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered =
        remember(definitions, query) {
            val q = query.trim().lowercase()
            if (q.isEmpty()) definitions
            else definitions.filter { it.name.lowercase().contains(q) }
        }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onNewRelease,
                enabled = selectedDefinitionId != null,
                modifier = Modifier.height(32.dp),
            ) {
                Text("+ New", style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search all pipelines", style = MaterialTheme.typography.bodySmall) },
            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = EditorialColors.outline) },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EditorialColors.primary),
        )
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = EditorialColors.outlineVariant.copy(alpha = 0.35f))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxSize()) {
            items(filtered, key = { it.id }) { def ->
                val selected = def.id == selectedDefinitionId
                Row(
                    Modifier
                        .fillMaxWidth()
                        .pipelineRowBg(selected)
                        .clickable { onSelectDefinition(def.id) }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(def.name, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                        def.subtitle?.let { sub ->
                            Text(
                                sub,
                                style = MaterialTheme.typography.bodySmall,
                                color = EditorialColors.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.pipelineRowBg(selected: Boolean): Modifier {
    val bg = if (selected) EditorialColors.primary.copy(alpha = 0.08f) else Color.Transparent
    return background(bg, RoundedCornerShape(8.dp))
}

@Composable
private fun ReleaseMainPanel(
    definitions: List<ReleaseDefinitionSummary>,
    selectedDefinitionId: Int?,
    releases: List<ReleaseSummary>,
    busy: Boolean,
    onOpenRelease: (ReleaseSummary) -> Unit,
) {
    var mainTab by remember { mutableIntStateOf(0) }
    val defName = definitions.firstOrNull { it.id == selectedDefinitionId }?.name.orEmpty()

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text(defName.ifBlank { "Pipelines" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
        Spacer(Modifier.height(16.dp))
        when (mainTab) {
            0 ->
                if (busy) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (releases.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No releases yet.", color = EditorialColors.onSurfaceVariant)
                    }
                } else {
                    ReleaseTable(releases = releases, onRowClick = onOpenRelease)
                }
            1 ->
                if (busy) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (releases.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No deployments — no releases loaded.", color = EditorialColors.onSurfaceVariant)
                    }
                } else {
                    DeploymentsFromReleasesTable(releases = releases, onOpenRelease = onOpenRelease)
                }
        }
    }
}

@Composable
private fun ReleaseTable(
    releases: List<ReleaseSummary>,
    onRowClick: (ReleaseSummary) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Releases", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.2f))
            Text("Created", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.5f))
            Text("Stages", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.3f))
        }
        HorizontalDivider(color = EditorialColors.outlineVariant.copy(alpha = 0.4f))
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(releases, key = { it.id }) { r ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onRowClick(r) }
                        .background(EditorialColors.surface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1.2f)) {
                        Text(r.name, color = EditorialColors.primary, fontWeight = FontWeight.Medium)
                        val sub =
                            buildString {
                                r.commitShort?.let { append(it); append(" • ") }
                                r.branchLabel?.let { append(it) }
                            }
                        if (sub.isNotBlank()) {
                            Text(sub, style = MaterialTheme.typography.bodySmall, color = EditorialColors.onSurfaceVariant)
                        }
                    }
                    Text(
                        formatReleaseTime(r.createdOnIso),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(0.5f),
                    )
                    Row(Modifier.weight(1.3f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        r.stages.take(6).forEach { StageMiniPill(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StageMiniPill(pill: ReleaseStagePill) {
    val (icon, tint) =
        when (pill.status) {
            ReleaseDeploymentStatus.Succeeded -> Icons.Outlined.CheckCircle to EditorialColors.primary
            ReleaseDeploymentStatus.Failed -> Icons.Outlined.Close to EditorialColors.error
            ReleaseDeploymentStatus.InProgress -> Icons.Outlined.RadioButtonUnchecked to EditorialColors.primary
            ReleaseDeploymentStatus.NotStarted -> Icons.Outlined.RadioButtonUnchecked to EditorialColors.outlineVariant
            ReleaseDeploymentStatus.Cancelled -> Icons.Outlined.RadioButtonUnchecked to EditorialColors.outline
            ReleaseDeploymentStatus.Unknown -> Icons.Outlined.RadioButtonUnchecked to EditorialColors.outline
        }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = EditorialColors.surfaceContainer,
        border = BorderStroke(1.dp, EditorialColors.outlineVariant.copy(alpha = 0.4f)),
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.width(14.dp).height(14.dp))
            Text(
                pill.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun DeploymentsFromReleasesTable(
    releases: List<ReleaseSummary>,
    onOpenRelease: (ReleaseSummary) -> Unit,
) {
    val rows =
        remember(releases) {
            releases.flatMap { rel ->
                rel.stages.map { stage ->
                    Triple(rel, stage.name, stage.status)
                }
            }
        }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Release", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("Environment", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.9f))
            Text("State", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.6f))
        }
        HorizontalDivider(color = EditorialColors.outlineVariant.copy(alpha = 0.4f))
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(rows.size, key = { idx -> "${rows[idx].first.id}-${rows[idx].second}-$idx" }) { idx ->
                val (rel, envName, st) = rows[idx]
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpenRelease(rel) }
                        .background(EditorialColors.surface.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(rel.name, color = EditorialColors.primary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(envName, modifier = Modifier.weight(0.9f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(st.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.6f))
                }
            }
        }
    }
}

private fun formatReleaseTime(iso: String?): String =
    iso?.replace('T', ' ')?.substringBefore('.').orEmpty().ifBlank { "—" }
