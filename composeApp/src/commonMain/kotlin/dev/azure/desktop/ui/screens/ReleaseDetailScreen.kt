package dev.azure.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.azure.desktop.domain.release.ReleaseDeploymentStatus
import dev.azure.desktop.domain.release.ReleaseDetail
import dev.azure.desktop.domain.release.ReleaseEnvironmentInfo
import dev.azure.desktop.domain.release.ReleaseTimelineEntry
import dev.azure.desktop.domain.release.ReleaseVariableRow
import dev.azure.desktop.release.detail.ReleaseDetailAction
import dev.azure.desktop.release.detail.ReleaseDetailState
import dev.azure.desktop.release.detail.ReleaseDetailStateMachine
import dev.azure.desktop.theme.EditorialColors
import dev.azure.desktop.ui.adaptive.LayoutClass
import dev.azure.desktop.ui.adaptive.layoutClassForWidth
import kotlinx.coroutines.launch

@Composable
fun ReleaseDetailScreen(
    stateMachine: ReleaseDetailStateMachine,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<ReleaseDetailState>(ReleaseDetailState.Loading) }
    LaunchedEffect(stateMachine) {
        stateMachine.state.collect { state = it }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val compactLayout = layoutClassForWidth(maxWidth) == LayoutClass.Compact
        if (compactLayout) {
            ReleaseDetailScreenMobile(
                stateMachine = stateMachine,
                state = state,
                scope = scope,
                onBack = onBack,
            )
        } else {
            ReleaseDetailScreenDesktop(
                stateMachine = stateMachine,
                state = state,
                scope = scope,
                onBack = onBack,
            )
        }
    }
}

@Composable
internal fun ReleaseDetailScreenContent(
    stateMachine: ReleaseDetailStateMachine,
    state: ReleaseDetailState,
    scope: kotlinx.coroutines.CoroutineScope,
    onBack: () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = EditorialColors.surfaceContainerLow) {
        when (val current = state) {
            ReleaseDetailState.Loading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

            is ReleaseDetailState.Error ->
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(current.message, color = EditorialColors.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { scope.launch { stateMachine.dispatch(ReleaseDetailAction.Reload) } }) {
                        Text("Retry")
                    }
                }

            is ReleaseDetailState.Content ->
                ReleaseDetailBody(
                    content = current,
                    onBack = onBack,
                    onReload = { scope.launch { stateMachine.dispatch(ReleaseDetailAction.Reload) } },
                    onDeploy = { envId ->
                        scope.launch { stateMachine.dispatch(ReleaseDetailAction.DeployEnvironment(envId)) }
                    },
                )
        }
    }
}

@Composable
private fun ReleaseDetailBody(
    content: ReleaseDetailState.Content,
    onBack: () -> Unit,
    onReload: () -> Unit,
    onDeploy: (environmentId: Int) -> Unit,
) {
    val detail = content.detail
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
            Text(
                "${detail.definitionName} › ${detail.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))
        TabRow(
            selectedTabIndex = tab,
            containerColor = EditorialColors.surfaceContainerLow,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[tab]).height(2.dp),
                    color = EditorialColors.primary,
                )
            },
            divider = { },
        ) {
            Tab(
                selected = tab == 0,
                onClick = { tab = 0 },
                text = { Text("Pipeline", fontWeight = if (tab == 0) FontWeight.Bold else FontWeight.Medium) },
            )
            Tab(
                selected = tab == 1,
                onClick = { tab = 1 },
                text = { Text("Variables", fontWeight = if (tab == 1) FontWeight.Bold else FontWeight.Medium) },
            )
            Tab(
                selected = tab == 2,
                onClick = { tab = 2 },
                text = { Text("History", fontWeight = if (tab == 2) FontWeight.Bold else FontWeight.Medium) },
            )
        }
        Spacer(Modifier.height(16.dp))
        when (tab) {
            0 ->
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val compactLayout = layoutClassForWidth(maxWidth) == LayoutClass.Compact
                    if (compactLayout) {
                        Column(Modifier.fillMaxSize()) {
                            Surface(
                                Modifier.fillMaxWidth().weight(0.38f),
                                shape = RoundedCornerShape(12.dp),
                                color = EditorialColors.surfaceContainerLowest,
                            ) {
                                ReleaseMetaColumn(detail = detail)
                            }
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                Modifier.fillMaxWidth().weight(0.62f),
                                shape = RoundedCornerShape(12.dp),
                                color = EditorialColors.surfaceContainerLowest,
                            ) {
                                StagesFlowColumn(
                                    environments = detail.environments,
                                    isDeploying = content.isDeploying,
                                    deployError = content.deployError,
                                    onReload = onReload,
                                    onDeploy = onDeploy,
                                )
                            }
                        }
                    } else {
                        Row(Modifier.fillMaxSize()) {
                            Surface(
                                Modifier.width(320.dp).fillMaxSize(),
                                shape = RoundedCornerShape(12.dp),
                                color = EditorialColors.surfaceContainerLowest,
                            ) {
                                ReleaseMetaColumn(detail = detail)
                            }
                            Spacer(Modifier.width(16.dp))
                            Surface(
                                Modifier.weight(1f).fillMaxSize(),
                                shape = RoundedCornerShape(12.dp),
                                color = EditorialColors.surfaceContainerLowest,
                            ) {
                                StagesFlowColumn(
                                    environments = detail.environments,
                                    isDeploying = content.isDeploying,
                                    deployError = content.deployError,
                                    onReload = onReload,
                                    onDeploy = onDeploy,
                                )
                            }
                        }
                    }
                }
            1 ->
                ReleaseVariablesTab(detail = detail)
            2 ->
                ReleaseHistoryTab(detail = detail)
        }
    }
}

@Composable
private fun ReleaseMetaColumn(detail: ReleaseDetail) {
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Release", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Surface(shape = RoundedCornerShape(10.dp), color = EditorialColors.surface) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                detail.triggerDescription?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                detail.requestedForDisplay?.let { who ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("for", style = MaterialTheme.typography.bodySmall, color = EditorialColors.onSurfaceVariant)
                        Text(who, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
                Text(
                    formatReleaseTime(detail.createdOnIso),
                    style = MaterialTheme.typography.bodySmall,
                    color = EditorialColors.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text("Artifacts", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                detail.artifacts.forEach { a ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(a.alias, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                            val line =
                                buildString {
                                    a.commitShort?.let { append(it); append(" • ") }
                                    a.branch?.let { append(it) }
                                }
                            if (line.isNotBlank()) {
                                Text(line, style = MaterialTheme.typography.bodySmall, color = EditorialColors.primary)
                            }
                        }
                        Icon(Icons.Outlined.CheckCircle, null, tint = EditorialColors.primary)
                    }
                }
                if (detail.artifacts.isEmpty()) {
                    Text("No artifacts linked.", style = MaterialTheme.typography.bodySmall, color = EditorialColors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun StagesFlowColumn(
    environments: List<ReleaseEnvironmentInfo>,
    isDeploying: Boolean,
    deployError: String?,
    onReload: () -> Unit,
    onDeploy: (environmentId: Int) -> Unit,
) {
    var selectedStageIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(environments.size) {
        if (environments.isEmpty()) return@LaunchedEffect
        if (selectedStageIndex >= environments.size) {
            selectedStageIndex = environments.lastIndex
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Stages", fontWeight = FontWeight.SemiBold)
                if (isDeploying) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = EditorialColors.primary,
                    )
                }
            }
            OutlinedButton(onClick = onReload, enabled = !isDeploying) {
                Text("Refresh")
            }
        }
        deployError?.takeIf { it.isNotBlank() }?.let { msg ->
            Spacer(Modifier.height(10.dp))
            Text(msg, color = EditorialColors.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(16.dp))
        environments.forEachIndexed { index, env ->
            if (index > 0) {
                Row(
                    Modifier
                        .padding(start = 24.dp, bottom = 8.dp)
                        .width(2.dp)
                        .height(16.dp)
                        .background(EditorialColors.outlineVariant),
                ) { }
            }
            EnvironmentStageCard(
                env = env,
                selected = index == selectedStageIndex,
                onSelect = { selectedStageIndex = index },
                isDeploying = isDeploying,
                onDeploy = { onDeploy(env.id) },
            )
        }
    }
}

@Composable
private fun EnvironmentStageCard(
    env: ReleaseEnvironmentInfo,
    selected: Boolean,
    onSelect: () -> Unit,
    isDeploying: Boolean,
    onDeploy: () -> Unit,
) {
    val borderColor =
        if (selected) EditorialColors.primary.copy(alpha = 0.5f)
        else EditorialColors.outlineVariant.copy(alpha = 0.4f)
    val statusLine = environmentStatusDisplayText(env)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color =
            if (selected) EditorialColors.primary.copy(alpha = 0.06f)
            else EditorialColors.surface,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.clickable(onClick = onSelect),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StageStatusGlyph(env.status)
                Column {
                    Text(env.name, fontWeight = FontWeight.SemiBold)
                    Text(statusLine, style = MaterialTheme.typography.bodySmall, color = EditorialColors.onSurfaceVariant)
                }
            }
            env.detailLine?.let { line ->
                Text(line, style = MaterialTheme.typography.bodySmall, color = EditorialColors.onSurfaceVariant)
            }
            if (selected) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (env.status == ReleaseDeploymentStatus.NotStarted) {
                        OutlinedButton(
                            onClick = onDeploy,
                            enabled = !isDeploying && env.id > 0,
                        ) {
                            Icon(
                                Icons.Outlined.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Deploy")
                        }
                    }
                    if (env.status == ReleaseDeploymentStatus.InProgress) {
                        OutlinedButton(onClick = { }, enabled = false) {
                            Text("Cancel")
                        }
                    }
                    OutlinedButton(onClick = { }, enabled = false) {
                        Icon(
                            Icons.AutoMirrored.Outlined.List,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Logs")
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

private fun environmentStatusDisplayText(env: ReleaseEnvironmentInfo): String =
    when (env.status) {
        ReleaseDeploymentStatus.NotStarted -> "Not deployed"
        else -> env.statusLabel
    }

@Composable
private fun StageStatusGlyph(status: ReleaseDeploymentStatus) {
    val (icon, tint) =
        when (status) {
            ReleaseDeploymentStatus.Succeeded -> Icons.Outlined.CheckCircle to EditorialColors.primary
            ReleaseDeploymentStatus.Failed -> Icons.Outlined.Close to EditorialColors.error
            ReleaseDeploymentStatus.InProgress -> Icons.Outlined.RadioButtonUnchecked to EditorialColors.primary
            ReleaseDeploymentStatus.NotStarted -> Icons.Outlined.RadioButtonUnchecked to EditorialColors.outlineVariant
            ReleaseDeploymentStatus.Cancelled -> Icons.Outlined.Close to EditorialColors.outline
            ReleaseDeploymentStatus.Unknown -> Icons.Outlined.RadioButtonUnchecked to EditorialColors.outline
        }
    Icon(icon, null, tint = tint)
}

@Composable
private fun ReleaseVariablesTab(detail: ReleaseDetail) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Variables", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Values come from the release resource returned by Azure DevOps (GET release). Secret values are masked in the UI.",
            style = MaterialTheme.typography.bodySmall,
            color = EditorialColors.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        if (detail.variables.isEmpty()) {
            Text("No variables on this release.", color = EditorialColors.onSurfaceVariant)
        } else {
            detail.variables.forEach { row ->
                VariableRow(row = row)
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = EditorialColors.outlineVariant.copy(alpha = 0.35f))
            }
        }
    }
}

@Composable
private fun VariableRow(row: ReleaseVariableRow) {
    Column(Modifier.fillMaxWidth()) {
        Text(row.name, fontWeight = FontWeight.Medium)
        Text(
            row.value,
            style = MaterialTheme.typography.bodySmall,
            color = if (row.isSecret) EditorialColors.onSurfaceVariant else EditorialColors.onSurface,
        )
    }
}

@Composable
private fun ReleaseHistoryTab(detail: ReleaseDetail) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("History", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Timeline entries are built from timestamps in the same GET release payload (created/modified, environment, deploy steps).",
            style = MaterialTheme.typography.bodySmall,
            color = EditorialColors.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        if (detail.timeline.isEmpty()) {
            Text("No timeline events parsed.", color = EditorialColors.onSurfaceVariant)
        } else {
            LazyHistoryList(entries = detail.timeline)
        }
    }
}

@Composable
private fun LazyHistoryList(entries: List<ReleaseTimelineEntry>) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        entries.forEach { e ->
            Column(Modifier.fillMaxWidth()) {
                Text(
                    formatReleaseTime(e.timestampIso),
                    style = MaterialTheme.typography.labelSmall,
                    color = EditorialColors.primary,
                    fontWeight = FontWeight.Medium,
                )
                Text(e.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun formatReleaseTime(iso: String?): String =
    iso?.replace('T', ' ')?.substringBefore('.').orEmpty().ifBlank { "—" }
