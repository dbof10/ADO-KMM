package dev.azure.desktop.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azure.desktop.domain.pr.PullRequestChange
import dev.azure.desktop.domain.pr.PullRequestCheckState
import dev.azure.desktop.domain.pr.PullRequestCheckStatus
import dev.azure.desktop.domain.pr.PullRequestDiffLine
import dev.azure.desktop.pr.review.CodeReviewAction
import dev.azure.desktop.pr.review.CodeReviewState
import dev.azure.desktop.pr.review.CodeReviewStateMachine
import dev.azure.desktop.theme.EditorialColors
import kotlinx.coroutines.launch

@Composable
fun CodeReviewScreen(
    stateMachine: CodeReviewStateMachine,
    modifier: Modifier = Modifier,
) {
    var state: CodeReviewState by remember(stateMachine) { mutableStateOf(CodeReviewState.Loading) }
    LaunchedEffect(stateMachine) { stateMachine.state.collect { state = it } }
    val scope = rememberCoroutineScope()

    Row(modifier.fillMaxSize()) {
        FileExplorerPane(
            state = state,
            onSelectPath = { path ->
                scope.launch { stateMachine.dispatch(CodeReviewAction.SelectFile(path)) }
            },
            modifier = Modifier.width(260.dp).fillMaxHeight(),
        )
        Column(Modifier.weight(1f).fillMaxHeight()) {
            DiffToolbar(state)
            DiffBody(state, Modifier.weight(1f))
            DiffFooter(state)
        }
    }
}

@Composable
private fun FileExplorerPane(
    state: CodeReviewState,
    onSelectPath: (String) -> Unit,
    modifier: Modifier,
) {
    var expandedFolders by remember { mutableStateOf(setOf<String>()) }

    Column(
        modifier
            .background(EditorialColors.surfaceContainerLow),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("EXPLORER", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Outlined.MoreHoriz, null, tint = EditorialColors.outline, modifier = Modifier.size(18.dp))
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            when (state) {
                CodeReviewState.Loading -> {
                    FileRow(
                        disclosure = null,
                        icon = Icons.Outlined.FolderOpen,
                        label = "Loading…",
                        indent = 0,
                        selected = false,
                        onClick = null,
                    )
                }

                is CodeReviewState.Error -> {
                    FileRow(
                        disclosure = null,
                        icon = Icons.Outlined.FolderOpen,
                        label = "Failed to load",
                        indent = 0,
                        selected = false,
                        onClick = null,
                    )
                    FileRow(
                        disclosure = null,
                        icon = Icons.Outlined.Description,
                        label = state.message,
                        indent = 1,
                        selected = false,
                        onClick = null,
                    )
                }

                is CodeReviewState.Content -> {
                    if (state.changes.isEmpty()) {
                        FileRow(
                            disclosure = null,
                            icon = Icons.Outlined.FolderOpen,
                            label = "No changes",
                            indent = 0,
                            selected = false,
                            onClick = null,
                        )
                    } else {
                        val paths = state.changes.map { it.path }
                        val tree = buildPathTree(paths)
                        val fileStrike = state.changes.associateBy({ it.path }, { changeDotAndStrike(it).second })

                        // Auto-expand the first root folder for a nicer first view.
                        LaunchedEffect(tree) {
                            if (expandedFolders.isEmpty()) {
                                val firstRootFolder = tree.filterIsInstance<PathNode.Folder>().firstOrNull()
                                if (firstRootFolder != null) {
                                    expandedFolders = expandedFolders + firstRootFolder.fullPath
                                }
                            }
                        }

                        val visible = flattenVisible(tree, expandedFolders)
                        visible.forEach { item ->
                            when (val node = item.node) {
                                is PathNode.Folder -> {
                                    val isExpanded = expandedFolders.contains(node.fullPath)
                                    FileRow(
                                        disclosure = {
                                            Icon(
                                                if (isExpanded) Icons.Outlined.ExpandMore else Icons.Outlined.ChevronRight,
                                                null,
                                                tint = EditorialColors.outline,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        },
                                        icon = if (isExpanded) Icons.Outlined.FolderOpen else Icons.Outlined.Folder,
                                        label = node.name,
                                        indent = item.depth.coerceAtMost(8),
                                        selected = false,
                                        struck = false,
                                        onClick = {
                                            expandedFolders =
                                                if (isExpanded) expandedFolders - node.fullPath else expandedFolders + node.fullPath
                                        },
                                    )
                                }

                                is PathNode.File -> {
                                    val selected = node.fullPath == state.selectedPath
                                    val struck = fileStrike[node.fullPath] ?: false
                                    FileRow(
                                        disclosure = null,
                                        icon = Icons.Outlined.Description,
                                        label = node.name,
                                        indent = item.depth.coerceAtMost(8),
                                        selected = selected,
                                        struck = struck,
                                        onClick = { onSelectPath(node.fullPath) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Surface(color = EditorialColors.surfaceContainerHigh) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(EditorialColors.primaryFixed),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("JS", color = EditorialColors.onPrimaryFixed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    val (idLabel, titleLabel) =
                        when (state) {
                            CodeReviewState.Loading -> "PR" to "Loading…"
                            is CodeReviewState.Error -> "PR" to "—"
                            is CodeReviewState.Content -> "PR #${state.pullRequest.id}" to state.pullRequest.title
                        }
                    Text(idLabel, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text(titleLabel, fontSize = 10.sp, color = EditorialColors.outline, fontStyle = FontStyle.Italic, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun FileRow(
    disclosure: (@Composable () -> Unit)?,
    icon: ImageVector,
    label: String,
    indent: Int,
    selected: Boolean,
    struck: Boolean = false,
    onClick: (() -> Unit)?,
) {
    val bg = if (selected) EditorialColors.surfaceContainerHighest else EditorialColors.surfaceContainerLow
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .let { base -> if (onClick != null) base.clickable { onClick() } else base }
            .padding(horizontal = (8 + indent * 12).dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (disclosure != null) {
            disclosure()
        } else {
            Spacer(Modifier.width(18.dp))
        }
        Icon(icon, null, tint = if (selected) EditorialColors.primary else EditorialColors.outline, modifier = Modifier.size(20.dp))
        FileNameLabel(
            label = label,
            modifier = Modifier.weight(1f),
            struck = struck,
        )
    }
}

@Composable
private fun DiffToolbar(state: CodeReviewState) {
    val selected =
        when (state) {
            CodeReviewState.Loading -> null
            is CodeReviewState.Error -> null
            is CodeReviewState.Content -> state.selectedPath
        }
    val selectedChangeType =
        (state as? CodeReviewState.Content)
            ?.changes
            ?.firstOrNull { it.path == selected }
            ?.changeType
            .orEmpty()

    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(EditorialColors.surface)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val label = selected?.trimStart('/') ?: "Select a file"
            val folder = label.substringBeforeLast("/", missingDelimiterValue = "")
            if (folder.isNotBlank()) {
                Text("$folder /", color = EditorialColors.outline, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
            Text(label.substringAfterLast("/"), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            val badgeText = selectedChangeType.ifBlank { "—" }.uppercase()
            val badgeColor =
                when (selectedChangeType.lowercase()) {
                    "add" -> EditorialColors.primaryFixed
                    "delete" -> EditorialColors.errorContainer
                    "edit", "modify" -> EditorialColors.tertiaryFixed
                    else -> EditorialColors.surfaceContainerHigh
                }
            val badgeTextColor =
                when (selectedChangeType.lowercase()) {
                    "add" -> EditorialColors.onPrimaryFixed
                    "delete" -> EditorialColors.onErrorContainer
                    "edit", "modify" -> EditorialColors.onTertiaryFixed
                    else -> EditorialColors.outline
                }
            Surface(shape = RoundedCornerShape(999.dp), color = badgeColor) {
                Text(
                    badgeText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(10.dp), color = EditorialColors.surfaceContainerHigh) {
                Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = { },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EditorialColors.surfaceContainerLowest),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                    ) {
                        Text("Side-by-side", fontSize = 11.sp)
                    }
                    TextButton(onClick = { }) { Text("Inline", fontSize = 11.sp, color = EditorialColors.outline) }
                }
            }
            Icon(Icons.Outlined.Settings, null, tint = EditorialColors.outline)
        }
    }
}

@Composable
private fun DiffBody(state: CodeReviewState, modifier: Modifier) {
    val scroll = rememberScrollState()
    Column(
        modifier
            .fillMaxSize()
            .background(EditorialColors.surfaceContainerLowest)
            .verticalScroll(scroll)
            .padding(bottom = 8.dp),
    ) {
        when (state) {
            CodeReviewState.Loading -> {
                DiffLine("", EditorialColors.outline, "Loading diff…", null, null)
            }

            is CodeReviewState.Error -> {
                DiffLine("", EditorialColors.onErrorContainer, state.message, EditorialColors.errorContainer.copy(alpha = 0.2f), EditorialColors.error)
            }

            is CodeReviewState.Content -> {
                if (state.selectedPath == null) {
                    DiffLine("", EditorialColors.outline, "Select a file to view its diff.", null, null)
                } else if (state.diffLines.isEmpty()) {
                    DiffLine("", EditorialColors.outline, "No diff to display.", null, null)
                } else {
                    state.diffLines.forEach { line ->
                        val render = renderDiffLine(line)
                        DiffLine(
                            num = render.number,
                            textColor = render.textColor,
                            body = render.body,
                            rowBg = render.rowBg,
                            accent = render.accent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiffLine(
    num: String,
    textColor: androidx.compose.ui.graphics.Color,
    body: String,
    rowBg: androidx.compose.ui.graphics.Color?,
    accent: androidx.compose.ui.graphics.Color?,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(rowBg ?: EditorialColors.surfaceContainerLowest),
    ) {
        if (accent != null) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(accent))
        }
        Text(
            num,
            modifier = Modifier.width(48.dp).padding(vertical = 4.dp),
            fontSize = 12.sp,
            color = EditorialColors.outline,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            body,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp),
            fontSize = 13.sp,
            color = textColor,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun CommentThreadCard() {
    Column(Modifier.padding(start = 40.dp, end = 24.dp, top = 20.dp, bottom = 20.dp)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 3.dp,
            color = EditorialColors.surfaceContainerLowest,
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(EditorialColors.surfaceContainerHigh))
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sarah Chen", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Surface(shape = RoundedCornerShape(999.dp), color = EditorialColors.primaryFixed) {
                                Text(
                                    "ACTIVE",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EditorialColors.onPrimaryFixed,
                                )
                            }
                        }
                        Text("2h ago", fontSize = 11.sp, color = EditorialColors.outline)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Should we consider adding a dependency array to this `useCallback`? Also, let's ensure the toggle state is synchronized with the parent component's ledger state.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Column(Modifier.padding(start = 40.dp)) {
                    val reply = remember { mutableStateOf("") }
                    BasicTextField(
                        value = reply.value,
                        onValueChange = { reply.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(EditorialColors.surfaceContainerHigh, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                            .height(72.dp),
                        decorationBox = { innerTextField ->
                            Box {
                                if (reply.value.isEmpty()) {
                                    Text("Reply or use @ to mention someone...", fontSize = 12.sp, color = EditorialColors.outline)
                                }
                                innerTextField()
                            }
                        },
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { }) { Text("Resolve", fontSize = 12.sp, color = EditorialColors.outline) }
                        Button(
                            onClick = { },
                            colors = ButtonDefaults.buttonColors(containerColor = EditorialColors.primary),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("Reply", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiffFooter(state: CodeReviewState) {
    val (added, removed) =
        when (state) {
            CodeReviewState.Loading -> 0 to 0
            is CodeReviewState.Error -> 0 to 0
            is CodeReviewState.Content -> {
                val a = state.diffLines.count { it is PullRequestDiffLine.Added }
                val r = state.diffLines.count { it is PullRequestDiffLine.Removed }
                a to r
            }
        }
    val branchLabel =
        when (state) {
            CodeReviewState.Loading -> "Branch: —"
            is CodeReviewState.Error -> "Branch: —"
            is CodeReviewState.Content -> {
                val source = state.pullRequest.sourceRefName.substringAfterLast("/")
                "Branch: ${if (source.isBlank()) "—" else source}"
            }
        }
    val encodingLabel = "UTF-8"
    val languageLabel =
        when (state) {
            CodeReviewState.Loading -> "—"
            is CodeReviewState.Error -> "—"
            is CodeReviewState.Content -> fileLanguageLabel(state.selectedPath)
        }
    val lintLabel =
        when (state) {
            CodeReviewState.Loading -> "Lint: —"
            is CodeReviewState.Error -> "Lint: —"
            is CodeReviewState.Content -> lintStatusLabel(state.checks)
        }
    Row(
        Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(EditorialColors.surfaceContainerLow)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(16.dp), tint = EditorialColors.outline)
                Text(lintLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EditorialColors.outline)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Sync, null, modifier = Modifier.size(16.dp), tint = EditorialColors.outline)
                Text(branchLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EditorialColors.outline)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(encodingLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EditorialColors.outline)
            Text(languageLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EditorialColors.outline)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(EditorialColors.primary))
                Text("+$added -$removed", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EditorialColors.outline)
            }
        }
    }
}

private fun fileLanguageLabel(path: String?): String {
    val p = path?.trim().orEmpty()
    if (p.isBlank()) return "—"
    val ext = p.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return when (ext) {
        "kt", "kts" -> "Kotlin"
        "java" -> "Java"
        "js" -> "JavaScript"
        "jsx" -> "JavaScript JSX"
        "ts" -> "TypeScript"
        "tsx" -> "TypeScript JSX"
        "json" -> "JSON"
        "md" -> "Markdown"
        "yml", "yaml" -> "YAML"
        "xml" -> "XML"
        "html" -> "HTML"
        "css" -> "CSS"
        "gradle" -> "Gradle"
        "properties" -> "Properties"
        "txt" -> "Text"
        "" -> "—"
        else -> ext.uppercase()
    }
}

private fun lintStatusLabel(checks: List<PullRequestCheckStatus>): String {
    if (checks.isEmpty()) return "Lint: —"
    val lint =
        checks.firstOrNull { it.name.contains("lint", ignoreCase = true) }
            ?: checks.firstOrNull { it.name.contains("ktlint", ignoreCase = true) }
            ?: checks.firstOrNull { it.name.contains("detekt", ignoreCase = true) }

    val state = lint?.state ?: aggregateCheckState(checks)
    return "Lint: ${checkStateLabel(state)}"
}

private fun aggregateCheckState(checks: List<PullRequestCheckStatus>): PullRequestCheckState {
    if (checks.any { it.state == PullRequestCheckState.Failed }) return PullRequestCheckState.Failed
    if (checks.any { it.state == PullRequestCheckState.Running }) return PullRequestCheckState.Running
    if (checks.any { it.state == PullRequestCheckState.Pending }) return PullRequestCheckState.Pending
    if (checks.any { it.state == PullRequestCheckState.Succeeded }) return PullRequestCheckState.Succeeded
    if (checks.any { it.state == PullRequestCheckState.NotApplicable }) return PullRequestCheckState.NotApplicable
    return PullRequestCheckState.Unknown
}

private fun checkStateLabel(state: PullRequestCheckState): String =
    when (state) {
        PullRequestCheckState.Succeeded -> "Passed"
        PullRequestCheckState.Failed -> "Failed"
        PullRequestCheckState.Pending -> "Pending"
        PullRequestCheckState.Running -> "Running"
        PullRequestCheckState.NotApplicable -> "N/A"
        PullRequestCheckState.Unknown -> "—"
    }

private data class DiffRender(
    val number: String,
    val body: String,
    val textColor: androidx.compose.ui.graphics.Color,
    val rowBg: androidx.compose.ui.graphics.Color?,
    val accent: androidx.compose.ui.graphics.Color?,
)

private fun renderDiffLine(line: PullRequestDiffLine): DiffRender =
    when (line) {
        is PullRequestDiffLine.Context ->
            DiffRender(
                number = (line.newLineNumber ?: line.oldLineNumber)?.toString().orEmpty(),
                body = "    ${line.text}",
                textColor = EditorialColors.onSurfaceVariant,
                rowBg = null,
                accent = null,
            )

        is PullRequestDiffLine.Removed ->
            DiffRender(
                number = line.oldLineNumber?.toString().orEmpty(),
                body = " -  ${line.text}",
                textColor = EditorialColors.onErrorContainer,
                rowBg = EditorialColors.errorContainer.copy(alpha = 0.2f),
                accent = EditorialColors.error,
            )

        is PullRequestDiffLine.Added ->
            DiffRender(
                number = line.newLineNumber?.toString().orEmpty(),
                body = " +  ${line.text}",
                textColor = EditorialColors.onPrimaryFixedVariant,
                rowBg = EditorialColors.primaryFixed.copy(alpha = 0.2f),
                accent = EditorialColors.primary,
            )
    }

private fun changeDotAndStrike(change: PullRequestChange): Pair<androidx.compose.ui.graphics.Color?, Boolean> =
    when (change.changeType.lowercase()) {
        "add" -> EditorialColors.primary to false
        "delete" -> EditorialColors.error to true
        "edit", "modify" -> EditorialColors.tertiaryContainer to false
        else -> null to false
    }

@Composable
private fun FileNameLabel(
    label: String,
    modifier: Modifier = Modifier,
    struck: Boolean,
) {
    val dot = label.lastIndexOf('.')
    val slash = label.lastIndexOf('/')
    val hasExtension = dot > slash && dot < label.length - 1
    val base = if (hasExtension) label.substring(0, dot) else label
    val ext = if (hasExtension) label.substring(dot) else ""

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Text(
            base,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            modifier = Modifier.weight(1f, fill = true),
            textDecoration = if (struck) TextDecoration.LineThrough else TextDecoration.None,
        )
        if (ext.isNotBlank()) {
            Text(
                ext,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                softWrap = false,
                color = EditorialColors.outline,
                textDecoration = if (struck) TextDecoration.LineThrough else TextDecoration.None,
            )
        }
    }
}

private sealed interface PathNode {
    val name: String
    val fullPath: String

    data class Folder(
        override val name: String,
        override val fullPath: String,
        val children: MutableList<PathNode> = mutableListOf(),
    ) : PathNode

    data class File(
        override val name: String,
        override val fullPath: String,
    ) : PathNode
}

private data class VisibleNode(
    val node: PathNode,
    val depth: Int,
)

private fun buildPathTree(paths: List<String>): List<PathNode> {
    val root = PathNode.Folder(name = "", fullPath = "")

    fun upsertFolder(parent: PathNode.Folder, segment: String, fullPath: String): PathNode.Folder {
        val existing =
            parent.children
                .filterIsInstance<PathNode.Folder>()
                .firstOrNull { it.name == segment }
        if (existing != null) return existing
        return PathNode.Folder(name = segment, fullPath = fullPath).also { parent.children.add(it) }
    }

    paths
        .map { it.trim().trimStart('/') }
        .filter { it.isNotBlank() }
        .forEach { raw ->
            val segments = raw.split('/').filter { it.isNotBlank() }
            var parent = root
            var acc = ""
            segments.forEachIndexed { idx, segment ->
                acc = if (acc.isBlank()) segment else "$acc/$segment"
                val isLast = idx == segments.lastIndex
                if (isLast) {
                    // ADO paths should be files, but be defensive in case a path ends with "/".
                    val existingFile = parent.children.filterIsInstance<PathNode.File>().any { it.name == segment }
                    if (!existingFile) parent.children.add(PathNode.File(name = segment, fullPath = "/$acc"))
                } else {
                    parent = upsertFolder(parent, segment, "/$acc")
                }
            }
        }

    fun sortFolder(folder: PathNode.Folder) {
        folder.children.sortWith(
            compareBy<PathNode> {
                when (it) {
                    is PathNode.Folder -> 0
                    is PathNode.File -> 1
                }
            }.thenBy { it.name.lowercase() },
        )
        folder.children.filterIsInstance<PathNode.Folder>().forEach { sortFolder(it) }
    }
    sortFolder(root)

    return root.children.toList()
}

private fun flattenVisible(nodes: List<PathNode>, expandedFolders: Set<String>): List<VisibleNode> {
    val out = ArrayList<VisibleNode>(nodes.size)

    fun visit(node: PathNode, depth: Int) {
        out.add(VisibleNode(node, depth))
        if (node is PathNode.Folder && expandedFolders.contains(node.fullPath)) {
            node.children.forEach { visit(it, depth + 1) }
        }
    }

    nodes.forEach { visit(it, depth = 0) }
    return out
}
