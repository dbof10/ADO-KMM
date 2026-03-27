package dev.azure.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.azure.desktop.domain.pr.CreatePullRequestParams
import dev.azure.desktop.domain.pr.CreatedPullRequest
import dev.azure.desktop.domain.pr.PullRequestRepositoryRef
import dev.azure.desktop.theme.EditorialColors
import dev.azure.desktop.ui.components.MascotLoadingIndicator
import kotlinx.coroutines.launch

@Composable
fun CreatePullRequestDialog(
    visible: Boolean,
    compactLayout: Boolean,
    organization: String,
    projectName: String?,
    listRepositories: suspend (organization: String, projectName: String) -> Result<List<PullRequestRepositoryRef>>,
    createPullRequest: suspend (CreatePullRequestParams) -> Result<CreatedPullRequest>,
    onDismiss: () -> Unit,
    onCreated: (CreatedPullRequest) -> Unit,
) {
    if (!visible) return
    val selectedProject = projectName?.takeIf { it.isNotBlank() } ?: return
    val scope = rememberCoroutineScope()

    var repositories by remember(selectedProject, visible) { mutableStateOf<List<PullRequestRepositoryRef>>(emptyList()) }
    var loadingRepos by remember(selectedProject, visible) { mutableStateOf(false) }
    var loadingError by remember(selectedProject, visible) { mutableStateOf<String?>(null) }
    var submitError by remember(selectedProject, visible) { mutableStateOf<String?>(null) }
    var busy by remember(visible) { mutableStateOf(false) }

    var selectedRepositoryId by remember(selectedProject, visible) { mutableStateOf("") }
    var sourceBranch by remember(selectedProject, visible) { mutableStateOf("") }
    var targetBranch by remember(selectedProject, visible) { mutableStateOf("develop") }
    var title by remember(selectedProject, visible) { mutableStateOf("") }
    var description by remember(selectedProject, visible) { mutableStateOf("") }

    LaunchedEffect(selectedProject, visible) {
        if (!visible) return@LaunchedEffect
        loadingRepos = true
        loadingError = null
        listRepositories(organization, selectedProject).fold(
            onSuccess = {
                repositories = it.sortedBy { repo -> repo.name.lowercase() }
                if (selectedRepositoryId.isBlank()) {
                    selectedRepositoryId = repositories.firstOrNull()?.id.orEmpty()
                }
                loadingRepos = false
            },
            onFailure = {
                loadingRepos = false
                loadingError = it.message ?: "Failed to load repositories."
            },
        )
    }

    CreatePullRequestDialogContainer(
        compactLayout = compactLayout,
        busy = busy,
        onDismiss = onDismiss,
        modifier = Modifier,
    ) {
        Column(Modifier.fillMaxHeight()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Create pull request", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(selectedProject, style = MaterialTheme.typography.bodyMedium, color = EditorialColors.onSurfaceVariant)
                }
            }

            Column(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                loadingError?.let { Text(it, color = EditorialColors.error) }
                if (loadingRepos) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { MascotLoadingIndicator() }
                }

                val selectedRepoName = repositories.firstOrNull { it.id == selectedRepositoryId }?.name ?: "Select repository"
                Text("Repository", style = MaterialTheme.typography.labelLarge)
                SimpleDropdown(
                    label = selectedRepoName,
                    enabled = repositories.isNotEmpty(),
                    options = repositories.map { it.id to it.name },
                    onSelect = { selectedRepositoryId = it },
                )
                OutlinedTextField(
                    value = sourceBranch,
                    onValueChange = { sourceBranch = it },
                    label = { Text("Source branch") },
                    placeholder = { Text("feature/my-branch") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = targetBranch,
                    onValueChange = { targetBranch = it },
                    label = { Text("Target branch") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                )
                submitError?.let { Text(it, color = EditorialColors.error) }
            }
            Row(
                Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = { if (!busy) onDismiss() }, enabled = !busy) { Text("Cancel") }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        scope.launch {
                            busy = true
                            submitError = null
                            createPullRequest(
                                CreatePullRequestParams(
                                    organization = organization,
                                    projectName = selectedProject,
                                    repositoryId = selectedRepositoryId,
                                    sourceBranchName = sourceBranch,
                                    targetBranchName = targetBranch,
                                    title = title,
                                    description = description,
                                ),
                            ).fold(
                                onSuccess = {
                                    busy = false
                                    onCreated(it)
                                    onDismiss()
                                },
                                onFailure = {
                                    busy = false
                                    submitError = it.message ?: "Failed to create pull request."
                                },
                            )
                        }
                    },
                    enabled = !busy && selectedRepositoryId.isNotBlank() && sourceBranch.isNotBlank() && targetBranch.isNotBlank() && title.isNotBlank(),
                ) {
                    Text("Create")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePullRequestDialogContainer(
    compactLayout: Boolean,
    busy: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    if (compactLayout) {
        ModalBottomSheet(
            onDismissRequest = { if (!busy) onDismiss() },
            modifier = modifier.fillMaxSize(),
            containerColor = EditorialColors.surfaceContainerLowest,
        ) {
            content()
        }
    } else {
        BoxWithConstraints(modifier.fillMaxSize()) {
            val panelWidth = maxWidth * 0.35f
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)).clickable(enabled = !busy) { onDismiss() },
            )
            Surface(
                shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                color = EditorialColors.surfaceContainerLowest,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(panelWidth),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SimpleDropdown(
    label: String,
    enabled: Boolean,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { if (enabled) expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().height(40.dp),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(label, maxLines = 1)
        }
        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.92f),
        ) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        expanded = false
                        onSelect(value)
                    },
                )
            }
        }
    }
}
