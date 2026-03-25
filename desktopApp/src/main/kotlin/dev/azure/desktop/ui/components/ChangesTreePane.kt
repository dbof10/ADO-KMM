package dev.azure.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azure.desktop.domain.pr.PullRequestChange
import dev.azure.desktop.pr.review.CodeReviewState
import dev.azure.desktop.theme.EditorialColors

@Composable
fun ChangesTreePane(
    state: CodeReviewState,
    onSelectPath: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandedFolders by remember { mutableStateOf(setOf<String>()) }

    Column(
        modifier
            .background(EditorialColors.surfaceContainerLow),
    ) {
        ChangesHeader(state = state)
        ChangesTabs()
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
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier =
                        Modifier
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
        modifier =
            Modifier
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
private fun ChangesHeader(state: CodeReviewState) {
    val repo =
        when (state) {
            CodeReviewState.Loading -> "—"
            is CodeReviewState.Error -> "—"
            is CodeReviewState.Content -> state.pullRequest.repositoryName.ifBlank { "—" }
        }
    val branch =
        when (state) {
            CodeReviewState.Loading -> "—"
            is CodeReviewState.Error -> "—"
            is CodeReviewState.Content -> state.pullRequest.sourceRefName.substringAfterLast("/").ifBlank { "—" }
        }

    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Current Repository", fontSize = 10.sp, color = EditorialColors.outline)
                Text(
                    repo,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Current Branch", fontSize = 10.sp, color = EditorialColors.outline)
                Text(
                    branch,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Outlined.MoreHoriz, null, tint = EditorialColors.outline, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ChangesTabs() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(EditorialColors.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Changes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("History", fontSize = 11.sp, color = EditorialColors.outline)
    }
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

    val textDecoration = if (struck) TextDecoration.LineThrough else TextDecoration.None
    val styledLabel =
        buildAnnotatedString {
            append(base)
            if (ext.isNotBlank()) {
                withStyle(SpanStyle(color = EditorialColors.outline)) {
                    append(ext)
                }
            }
        }

    Text(
        text = styledLabel,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        softWrap = false,
        textDecoration = textDecoration,
    )
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

private fun changeDotAndStrike(change: PullRequestChange): Pair<androidx.compose.ui.graphics.Color?, Boolean> =
    when (change.changeType.lowercase()) {
        "add" -> EditorialColors.primary to false
        "delete" -> EditorialColors.error to true
        "edit", "modify" -> EditorialColors.tertiaryContainer to false
        else -> null to false
    }

