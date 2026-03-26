package dev.azure.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.azure.desktop.domain.release.CreateReleaseParams
import dev.azure.desktop.domain.release.CreatedRelease
import dev.azure.desktop.domain.release.ReleaseDefinitionDetail
import dev.azure.desktop.domain.release.ReleaseDefinitionEnvironmentStage
import dev.azure.desktop.theme.EditorialColors
import kotlinx.coroutines.launch

@Composable
fun CreateReleaseDialog(
    visible: Boolean,
    organization: String,
    projectName: String,
    definitionId: Int,
    getReleaseDefinition: suspend (String, Int) -> Result<ReleaseDefinitionDetail>,
    createRelease: suspend (CreateReleaseParams) -> Result<CreatedRelease>,
    onDismiss: () -> Unit,
    onCreated: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    val scope = rememberCoroutineScope()
    var definition by remember(definitionId, projectName) { mutableStateOf<ReleaseDefinitionDetail?>(null) }
    var loadError by remember(definitionId, projectName) { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }
    var manualNames by remember(definitionId, projectName) { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(definitionId, projectName, visible) {
        if (!visible) return@LaunchedEffect
        loadError = null
        definition = null
        manualNames = emptySet()
        description = ""
        getReleaseDefinition(projectName, definitionId).fold(
            onSuccess = { definition = it },
            onFailure = { loadError = it.message ?: "Failed to load definition." },
        )
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val panelWidth = maxWidth * 0.3f
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
                .clickable(enabled = !busy) { onDismiss() },
        )
        Surface(
            shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
            color = EditorialColors.surfaceContainerLowest,
            tonalElevation = 3.dp,
            shadowElevation = 8.dp,
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(panelWidth),
        ) {
            Column(Modifier.fillMaxHeight()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Create a new release",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        definition?.let {
                            Text(
                                it.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = EditorialColors.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(onClick = { if (!busy) onDismiss() }, enabled = !busy) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                }

                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 8.dp),
                ) {
                    loadError?.let { err ->
                        Text(err, color = EditorialColors.error)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = onDismiss) { Text("Close") }
                        return@Column
                    }

                    if (definition == null) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator()
                        }
                        return@Column
                    }

                    val def = definition!!

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.Bolt, contentDescription = null, tint = EditorialColors.primary)
                        Text("Pipeline", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap a stage to mark it for manual deployment (sent as manualEnvironments to Azure DevOps). Others keep the definition default.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EditorialColors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    PipelinePreview(
                        stages = def.stages,
                        manualNames = manualNames,
                        onToggle = { name ->
                            manualNames = manualNames.toMutableSet().apply { if (!add(name)) remove(name) }
                        },
                    )
                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Stages for a trigger change from automated to manual",
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val all = def.stages.map { it.name }.toSet()
                            val allSelected = all.isNotEmpty() && manualNames.containsAll(all)
                            Checkbox(
                                checked = allSelected,
                                onCheckedChange = { checked ->
                                    manualNames = if (checked) all else emptySet()
                                },
                            )
                            Text("Select all")
                        }
                        def.stages.forEach { stage ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = manualNames.contains(stage.name),
                                    onCheckedChange = {
                                        manualNames = manualNames.toMutableSet().apply {
                                            if (!add(stage.name)) remove(stage.name)
                                        }
                                    },
                                )
                                Text(stage.name)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        label = { Text("Release description") },
                        maxLines = 6,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EditorialColors.primary),
                    )

                    submitError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = EditorialColors.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { if (!busy) onDismiss() }, enabled = !busy) {
                            Text("Cancel")
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    busy = true
                                    submitError = null
                                    createRelease(
                                        CreateReleaseParams(
                                            organization = organization,
                                            projectName = projectName,
                                            definitionId = definitionId,
                                            description = description.trim(),
                                            manualEnvironmentNames = manualNames.toList(),
                                        ),
                                    ).fold(
                                        onSuccess = {
                                            busy = false
                                            onCreated()
                                            onDismiss()
                                        },
                                        onFailure = {
                                            busy = false
                                            submitError = it.message ?: "Create failed."
                                        },
                                    )
                                }
                            },
                            enabled = !busy,
                            colors = ButtonDefaults.buttonColors(containerColor = EditorialColors.primaryContainer),
                        ) {
                            if (busy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = EditorialColors.onPrimary,
                                )
                            } else {
                                Text("Create")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PipelinePreview(
    stages: List<ReleaseDefinitionEnvironmentStage>,
    manualNames: Set<String>,
    onToggle: (String) -> Unit,
) {
    val byRank = remember(stages) { stages.groupBy { it.rank }.toSortedMap() }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        byRank.forEach { (_, group) ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                group.sortedBy { it.name }.forEach { stage ->
                    val manual = manualNames.contains(stage.name)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (manual) EditorialColors.tertiaryFixed else EditorialColors.primaryFixed,
                        modifier =
                            Modifier.padding(2.dp).clickable {
                                onToggle(stage.name)
                            },
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Bolt,
                                contentDescription = null,
                                tint = if (manual) EditorialColors.tertiary else EditorialColors.primary,
                            )
                            Text(stage.name, style = MaterialTheme.typography.labelLarge, maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}
