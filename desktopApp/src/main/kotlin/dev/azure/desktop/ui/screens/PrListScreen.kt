package dev.azure.desktop.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.azure.desktop.domain.pr.DevOpsProject
import dev.azure.desktop.domain.pr.PullRequestSummary
import dev.azure.desktop.pr.list.PrListAction
import dev.azure.desktop.pr.list.PrListState
import dev.azure.desktop.pr.list.PrListStateMachine
import dev.azure.desktop.pr.list.PrListTab
import dev.azure.desktop.theme.EditorialColors
import kotlinx.coroutines.launch

@Composable
fun PrListScreen(
    stateMachine: PrListStateMachine,
    onOpenPullRequest: (PullRequestSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<PrListState>(PrListState.LoadingProjects) }
    var prNumberInput by remember { mutableStateOf("") }
    LaunchedEffect(stateMachine) {
        stateMachine.state.collect { state = it }
    }

    Surface(modifier.fillMaxSize(), color = EditorialColors.surfaceContainerLow) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text("Pull requests", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            when (val current = state) {
                PrListState.LoadingProjects -> {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator()
                        Text("Loading projects…", color = EditorialColors.onSurfaceVariant)
                    }
                }

                is PrListState.ProjectsError -> {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(current.message, color = EditorialColors.error)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { scope.launch { stateMachine.dispatch(PrListAction.RetryProjects) } }) {
                            Text("Retry")
                        }
                    }
                }

                is PrListState.LoadingPullRequests -> {
                    ProjectAndTabsRow(
                        projects = current.projects,
                        selectedProjectName = current.selectedProjectName,
                        selectedTab = current.tab,
                        projectMenuEnabled = false,
                        rightOfProjectSelector = {
                            OpenPrByNumber(
                                prNumberInput = prNumberInput,
                                onPrNumberInputChange = { prNumberInput = it },
                                onOpen = { id ->
                                    scope.launch { stateMachine.dispatch(PrListAction.OpenPullRequestById(id)) }
                                },
                                enabled = true,
                            )
                        },
                        onSelectProject = { scope.launch { stateMachine.dispatch(PrListAction.SelectProject(it)) } },
                        onSelectTab = { scope.launch { stateMachine.dispatch(PrListAction.SelectTab(it)) } },
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        Modifier.fillMaxWidth().padding(top = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is PrListState.Ready -> {
                    ProjectAndTabsRow(
                        projects = current.projects,
                        selectedProjectName = current.selectedProjectName,
                        selectedTab = current.tab,
                        projectMenuEnabled = true,
                        rightOfProjectSelector = {
                            OpenPrByNumber(
                                prNumberInput = prNumberInput,
                                onPrNumberInputChange = { prNumberInput = it },
                                onOpen = { id ->
                                    scope.launch { stateMachine.dispatch(PrListAction.OpenPullRequestById(id)) }
                                },
                                enabled = true,
                            )
                        },
                        onSelectProject = { scope.launch { stateMachine.dispatch(PrListAction.SelectProject(it)) } },
                        onSelectTab = { scope.launch { stateMachine.dispatch(PrListAction.SelectTab(it)) } },
                    )
                    Spacer(Modifier.height(12.dp))
                    current.openPullRequestError?.let { msg ->
                        Text(msg, color = EditorialColors.error)
                        Spacer(Modifier.height(8.dp))
                    }
                    current.pendingOpenPullRequest?.let { summary ->
                        LaunchedEffect(summary.id) {
                            onOpenPullRequest(summary)
                            stateMachine.dispatch(PrListAction.ConsumePendingOpenPullRequest)
                        }
                    }
                    Surface(
                        color = EditorialColors.surfaceContainerLowest,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        LazyColumn(Modifier.fillMaxWidth()) {
                            items(current.items, key = { it.id }) { item ->
                                PullRequestRow(item = item, onClick = { onOpenPullRequest(item) })
                            }
                        }
                    }
                }

                is PrListState.PullRequestsError -> {
                    ProjectAndTabsRow(
                        projects = current.projects,
                        selectedProjectName = current.selectedProjectName,
                        selectedTab = current.tab,
                        projectMenuEnabled = true,
                        rightOfProjectSelector = {
                            OpenPrByNumber(
                                prNumberInput = prNumberInput,
                                onPrNumberInputChange = { prNumberInput = it },
                                onOpen = { id ->
                                    scope.launch { stateMachine.dispatch(PrListAction.OpenPullRequestById(id)) }
                                },
                                enabled = true,
                            )
                        },
                        onSelectProject = { scope.launch { stateMachine.dispatch(PrListAction.SelectProject(it)) } },
                        onSelectTab = { scope.launch { stateMachine.dispatch(PrListAction.SelectTab(it)) } },
                    )
                    Spacer(Modifier.height(16.dp))
                    Column(
                        Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(current.message, color = EditorialColors.error)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { scope.launch { stateMachine.dispatch(PrListAction.RefreshPullRequests) } }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectAndTabsRow(
    projects: List<DevOpsProject>,
    selectedProjectName: String?,
    selectedTab: PrListTab,
    projectMenuEnabled: Boolean,
    rightOfProjectSelector: @Composable (() -> Unit)? = null,
    onSelectProject: (String?) -> Unit,
    onSelectTab: (PrListTab) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Project", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProjectDropdown(
                projects = projects,
                selectedProjectName = selectedProjectName,
                enabled = projectMenuEnabled,
                modifier = Modifier.fillMaxWidth(0.25f),
                onSelect = onSelectProject,
            )
            Spacer(Modifier.weight(1f))
            rightOfProjectSelector?.invoke()
        }
        TabRow(
            selectedTabIndex = if (selectedTab == PrListTab.Mine) 0 else 1,
            containerColor = EditorialColors.surfaceContainerLow,
            indicator = { tabPositions ->
                val index = if (selectedTab == PrListTab.Mine) 0 else 1
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[index])
                        .height(2.dp),
                    color = EditorialColors.primary,
                )
            },
            divider = { },
        ) {
            Tab(
                selected = selectedTab == PrListTab.Mine,
                onClick = { onSelectTab(PrListTab.Mine) },
                selectedContentColor = EditorialColors.onSurface,
                unselectedContentColor = EditorialColors.onSurfaceVariant,
                text = { Text("Mine", fontWeight = if (selectedTab == PrListTab.Mine) FontWeight.Bold else FontWeight.Medium) },
            )
            Tab(
                selected = selectedTab == PrListTab.Active,
                onClick = { onSelectTab(PrListTab.Active) },
                selectedContentColor = EditorialColors.onSurface,
                unselectedContentColor = EditorialColors.onSurfaceVariant,
                text = { Text("Active", fontWeight = if (selectedTab == PrListTab.Active) FontWeight.Bold else FontWeight.Medium) },
            )
        }
    }
}

@Composable
private fun OpenPrByNumber(
    prNumberInput: String,
    onPrNumberInputChange: (String) -> Unit,
    onOpen: (Int) -> Unit,
    enabled: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = prNumberInput,
            onValueChange = { raw -> onPrNumberInputChange(raw.filter { ch -> ch.isDigit() }.take(12)) },
            modifier = Modifier.width(220.dp),
            singleLine = true,
            label = { Text("Open PR by #") },
            placeholder = { Text("130041") },
        )
        Button(
            onClick = {
                val id = prNumberInput.trim().toIntOrNull() ?: return@Button
                onOpen(id)
            },
            enabled = enabled && prNumberInput.trim().isNotBlank(),
        ) {
            Text("Open")
        }
    }
}

@Composable
private fun ProjectDropdown(
    projects: List<DevOpsProject>,
    selectedProjectName: String?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val label =
        when {
            selectedProjectName == null -> "All projects"
            else -> selectedProjectName
        }

    Box(modifier) {
        OutlinedButton(
            onClick = { if (enabled) expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().height(40.dp),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f),
        ) {
            DropdownMenuItem(
                text = { Text("All projects") },
                onClick = {
                    expanded = false
                    onSelect(null)
                },
            )
            projects.forEach { p ->
                DropdownMenuItem(
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
private fun PullRequestRow(item: PullRequestSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = EditorialColors.surface),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${item.creatorDisplayName} • ${item.projectName} → ${item.targetRefName.substringAfterLast("/")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = EditorialColors.onSurfaceVariant,
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = EditorialColors.primaryFixed,
            ) {
                Text(
                    item.status.uppercase(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = EditorialColors.onPrimaryFixed,
                )
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = EditorialColors.primaryContainer),
            ) {
                Text("Open")
            }
        }
    }
}
