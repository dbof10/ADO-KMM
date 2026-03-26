package dev.azure.desktop.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.BoldHighlight
import dev.snipme.highlights.model.CodeHighlight
import dev.snipme.highlights.model.ColorHighlight
import dev.snipme.highlights.model.SyntaxLanguage
import dev.snipme.highlights.model.SyntaxTheme
import dev.snipme.highlights.model.SyntaxThemes
import dev.azure.desktop.domain.pr.PullRequestCheckState
import dev.azure.desktop.domain.pr.PullRequestCheckStatus
import dev.azure.desktop.domain.pr.PullRequestDiffLine
import dev.azure.desktop.pr.review.CodeReviewAction
import dev.azure.desktop.pr.review.CodeReviewState
import dev.azure.desktop.pr.review.CodeReviewStateMachine
import dev.azure.desktop.theme.EditorialColors
import dev.azure.desktop.ui.components.ChangesTreePane
import kotlinx.coroutines.launch

private enum class DiffViewMode { SideBySide, Inline }
private enum class SyntaxLang { Kotlin, TypeScript, JavaScript, Json, Yaml, Xml, Markdown, Plain }

private object HighlightsEngine {
    // Highlights caches analysis internally; keep one instance per language/theme setup.
    private val theme: SyntaxTheme = SyntaxThemes.atom(true)
    private val engines = mutableMapOf<SyntaxLanguage, Highlights>()

    fun engineFor(language: SyntaxLanguage): Highlights =
        engines.getOrPut(language) {
            Highlights.Builder()
                .language(language)
                .theme(theme)
                .build()
        }
}

@Composable
fun CodeReviewScreen(
    stateMachine: CodeReviewStateMachine,
    modifier: Modifier = Modifier,
) {
    var state: CodeReviewState by remember(stateMachine) { mutableStateOf(CodeReviewState.Loading) }
    androidx.compose.runtime.LaunchedEffect(stateMachine) { stateMachine.state.collect { state = it } }
    val scope = rememberCoroutineScope()
    var viewMode by remember { mutableStateOf(DiffViewMode.SideBySide) }
    var treePaneWidth by remember { mutableStateOf(260f) }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val minTreePaneWidth = 180.dp.value
        val maxTreePaneWidth = maxOf(minTreePaneWidth, maxWidth.value - 320.dp.value)
        val clampedTreePaneWidth = treePaneWidth.coerceIn(minTreePaneWidth, maxTreePaneWidth)
        if (treePaneWidth != clampedTreePaneWidth) {
            treePaneWidth = clampedTreePaneWidth
        }

        Row(Modifier.fillMaxSize()) {
            ChangesTreePane(
                state = state,
                onSelectPath = { path ->
                    scope.launch { stateMachine.dispatch(CodeReviewAction.SelectFile(path)) }
                },
                modifier = Modifier.width(clampedTreePaneWidth.dp).fillMaxHeight(),
            )
            VerticalDragHandle(
                onDragDelta = { dragAmount ->
                    treePaneWidth = (treePaneWidth + dragAmount).coerceIn(minTreePaneWidth, maxTreePaneWidth)
                },
            )
            Column(Modifier.weight(1f).fillMaxHeight()) {
                DiffToolbar(
                    state = state,
                    viewMode = viewMode,
                    onSelectViewMode = { viewMode = it },
                )
                DiffBody(state, Modifier.weight(1f), viewMode = viewMode)
                DiffFooter(state)
            }
        }
    }
}

@Composable
private fun VerticalDragHandle(
    onDragDelta: (Float) -> Unit,
    width: Dp = 6.dp,
) {
    val density = LocalDensity.current
    Box(
        Modifier
            .width(width)
            .fillMaxHeight()
            .background(EditorialColors.surfaceContainerHigh)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    val dragAmountDp = with(density) { dragAmount.toDp().value }
                    onDragDelta(dragAmountDp)
                }
            },
    )
}

@Composable
private fun DiffToolbar(
    state: CodeReviewState,
    viewMode: DiffViewMode,
    onSelectViewMode: (DiffViewMode) -> Unit,
) {
    val selected =
        when (state) {
            CodeReviewState.Loading -> null
            is CodeReviewState.Error -> null
            is CodeReviewState.Content -> state.selectedPath
        }
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
        }
        DiffViewModeToggle(
            viewMode = viewMode,
            onSelectViewMode = onSelectViewMode,
        )
    }
}

@Composable
private fun DiffViewModeToggle(
    viewMode: DiffViewMode,
    onSelectViewMode: (DiffViewMode) -> Unit,
) {
    val inline = viewMode == DiffViewMode.Inline
    val trackWidth = 84.dp
    val trackHeight = 22.dp
    val thumbSize = 14.dp
    val edgePad = 4.dp
    val travel = trackWidth - edgePad * 2 - thumbSize
    val thumbOffset by animateDpAsState(
        targetValue = if (inline) travel else 0.dp,
        animationSpec = tween(durationMillis = 160),
        label = "diffViewThumb",
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Side-by-side",
            fontSize = 11.sp,
            color = if (!inline) EditorialColors.onSurface else EditorialColors.outline,
        )
        Box(
            Modifier
                .width(trackWidth)
                .height(trackHeight)
                .clip(RoundedCornerShape(999.dp))
                .background(EditorialColors.surfaceContainerHigh)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    onSelectViewMode(if (inline) DiffViewMode.SideBySide else DiffViewMode.Inline)
                },
        ) {
            Text(
                "Inline",
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                fontSize = 10.sp,
                color = EditorialColors.outline,
            )
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = edgePad)
                    .offset(x = thumbOffset, y = 0.dp)
                    .size(thumbSize)
                    .shadow(1.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.12f), spotColor = Color.Black.copy(alpha = 0.12f))
                    .clip(CircleShape)
                    .background(EditorialColors.surfaceContainerLowest),
            )
        }
    }
}

@Composable
private fun DiffBody(
    state: CodeReviewState,
    modifier: Modifier,
    viewMode: DiffViewMode,
) {
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
                DiffLine("", EditorialColors.outline, AnnotatedString("Loading diff…"), null, null)
            }

            is CodeReviewState.Error -> {
                DiffLine(
                    "",
                    EditorialColors.onErrorContainer,
                    AnnotatedString(state.message),
                    EditorialColors.errorContainer.copy(alpha = 0.2f),
                    EditorialColors.error,
                )
            }

            is CodeReviewState.Content -> {
                val lang = remember(state.selectedPath) { syntaxLangForPath(state.selectedPath) }
                if (state.selectedPath == null) {
                    DiffLine("", EditorialColors.outline, AnnotatedString("Select a file to view its diff."), null, null)
                } else if (state.isDiffLoading) {
                    DiffLine("", EditorialColors.outline, AnnotatedString("Loading diff…"), null, null)
                } else if (state.diffLines.isEmpty()) {
                    DiffLine("", EditorialColors.outline, AnnotatedString("No diff to display."), null, null)
                } else {
                    when (viewMode) {
                        DiffViewMode.Inline -> {
                            state.diffLines.forEach { line ->
                                val render = renderDiffLine(line, lang)
                                DiffLine(
                                    num = render.number,
                                    textColor = render.textColor,
                                    body = render.body,
                                    rowBg = render.rowBg,
                                    accent = render.accent,
                                )
                            }
                        }

                        DiffViewMode.SideBySide -> {
                            SideBySideDiff(state.diffLines, lang)
                        }
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
    body: AnnotatedString,
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
private fun SideBySideDiff(lines: List<PullRequestDiffLine>, lang: SyntaxLang) {
    val rows = remember(lines, lang) { toSideBySideRows(lines, lang) }
    Column {
        rows.forEach { row ->
            SideBySideRow(row = row)
        }
    }
}

private data class SideBySideCell(
    val num: String,
    val text: AnnotatedString,
    val textColor: Color,
    val bg: Color?,
    val accent: Color?,
)

private data class SideBySideRowModel(
    val left: SideBySideCell,
    val right: SideBySideCell,
)

private fun toSideBySideRows(lines: List<PullRequestDiffLine>, lang: SyntaxLang): List<SideBySideRowModel> =
    lines.map { line ->
        when (line) {
            is PullRequestDiffLine.Context -> {
                val num = (line.newLineNumber ?: line.oldLineNumber)?.toString().orEmpty()
                val code = highlightCode(line.text, lang, baseColor = EditorialColors.onSurfaceVariant)
                val cell =
                    SideBySideCell(
                        num = num,
                        text = prefixAndCodeAnnotated(prefix = "    ", prefixColor = EditorialColors.outline, code = code),
                        textColor = EditorialColors.onSurfaceVariant,
                        bg = null,
                        accent = null,
                    )
                SideBySideRowModel(left = cell, right = cell)
            }

            is PullRequestDiffLine.Removed -> {
                val code = highlightCode(line.text, lang, baseColor = EditorialColors.onErrorContainer)
                SideBySideRowModel(
                    left =
                        SideBySideCell(
                            num = line.oldLineNumber?.toString().orEmpty(),
                            text =
                                prefixAndCodeAnnotated(
                                    prefix = " -  ",
                                    prefixColor = EditorialColors.error,
                                    code = code,
                                ),
                            textColor = EditorialColors.onErrorContainer,
                            bg = EditorialColors.errorContainer.copy(alpha = 0.2f),
                            accent = EditorialColors.error,
                        ),
                    right =
                        SideBySideCell(
                            num = "",
                            text = AnnotatedString(""),
                            textColor = EditorialColors.onSurfaceVariant,
                            bg = null,
                            accent = null,
                        ),
                )
            }

            is PullRequestDiffLine.Added -> {
                val code = highlightCode(line.text, lang, baseColor = EditorialColors.onPrimaryFixedVariant)
                SideBySideRowModel(
                    left =
                        SideBySideCell(
                            num = "",
                            text = AnnotatedString(""),
                            textColor = EditorialColors.onSurfaceVariant,
                            bg = null,
                            accent = null,
                        ),
                    right =
                        SideBySideCell(
                            num = line.newLineNumber?.toString().orEmpty(),
                            text =
                                prefixAndCodeAnnotated(
                                    prefix = " +  ",
                                    prefixColor = EditorialColors.primary,
                                    code = code,
                                ),
                            textColor = EditorialColors.onPrimaryFixedVariant,
                            bg = EditorialColors.primaryFixed.copy(alpha = 0.2f),
                            accent = EditorialColors.primary,
                        ),
                )
            }
        }
    }

@Composable
private fun SideBySideRow(row: SideBySideRowModel) {
    Row(Modifier.fillMaxWidth()) {
        SideBySideCellView(
            cell = row.left,
            modifier = Modifier.weight(1f),
            divider = true,
        )
        SideBySideCellView(
            cell = row.right,
            modifier = Modifier.weight(1f),
            divider = false,
        )
    }
}

@Composable
private fun SideBySideCellView(
    cell: SideBySideCell,
    modifier: Modifier,
    divider: Boolean,
) {
    Row(
        modifier
            .background(cell.bg ?: EditorialColors.surfaceContainerLowest)
            .let { base -> if (divider) base else base },
    ) {
        if (cell.accent != null) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(cell.accent))
        } else {
            Spacer(Modifier.width(3.dp))
        }
        Text(
            cell.num,
            modifier = Modifier.width(48.dp).padding(vertical = 4.dp),
            fontSize = 12.sp,
            color = EditorialColors.outline,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            cell.text,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp).weight(1f, fill = true),
            fontSize = 13.sp,
            color = cell.textColor,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            softWrap = false,
        )
        if (divider) {
            Box(Modifier.width(1.dp).fillMaxHeight().background(EditorialColors.surfaceContainerHigh))
        }
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
                if (state.isDiffLoading) {
                    0 to 0
                } else {
                    val a = state.diffLines.count { it is PullRequestDiffLine.Added }
                    val r = state.diffLines.count { it is PullRequestDiffLine.Removed }
                    a to r
                }
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
    val body: AnnotatedString,
    val textColor: androidx.compose.ui.graphics.Color,
    val rowBg: androidx.compose.ui.graphics.Color?,
    val accent: androidx.compose.ui.graphics.Color?,
)

private fun renderDiffLine(line: PullRequestDiffLine, lang: SyntaxLang): DiffRender =
    when (line) {
        is PullRequestDiffLine.Context ->
            DiffRender(
                number = (line.newLineNumber ?: line.oldLineNumber)?.toString().orEmpty(),
                body =
                    prefixAndCodeAnnotated(
                        prefix = "    ",
                        prefixColor = EditorialColors.outline,
                        code = highlightCode(line.text, lang, baseColor = EditorialColors.onSurfaceVariant),
                    ),
                textColor = EditorialColors.onSurfaceVariant,
                rowBg = null,
                accent = null,
            )

        is PullRequestDiffLine.Removed ->
            DiffRender(
                number = line.oldLineNumber?.toString().orEmpty(),
                body =
                    prefixAndCodeAnnotated(
                        prefix = " -  ",
                        prefixColor = EditorialColors.error,
                        code = highlightCode(line.text, lang, baseColor = EditorialColors.onErrorContainer),
                    ),
                textColor = EditorialColors.onErrorContainer,
                rowBg = EditorialColors.errorContainer.copy(alpha = 0.2f),
                accent = EditorialColors.error,
            )

        is PullRequestDiffLine.Added ->
            DiffRender(
                number = line.newLineNumber?.toString().orEmpty(),
                body =
                    prefixAndCodeAnnotated(
                        prefix = " +  ",
                        prefixColor = EditorialColors.primary,
                        code = highlightCode(line.text, lang, baseColor = EditorialColors.onPrimaryFixedVariant),
                    ),
                textColor = EditorialColors.onPrimaryFixedVariant,
                rowBg = EditorialColors.primaryFixed.copy(alpha = 0.2f),
                accent = EditorialColors.primary,
            )
    }

private fun syntaxLangForPath(path: String?): SyntaxLang {
    val p = path?.trim().orEmpty()
    if (p.isBlank()) return SyntaxLang.Plain
    return when (p.substringAfterLast('.', missingDelimiterValue = "").lowercase()) {
        "kt", "kts" -> SyntaxLang.Kotlin
        "ts", "tsx" -> SyntaxLang.TypeScript
        "js", "jsx" -> SyntaxLang.JavaScript
        "json" -> SyntaxLang.Json
        "yml", "yaml" -> SyntaxLang.Yaml
        "xml", "html" -> SyntaxLang.Xml
        "md" -> SyntaxLang.Markdown
        else -> SyntaxLang.Plain
    }
}

private fun prefixAndCodeAnnotated(
    prefix: String,
    prefixColor: Color,
    code: AnnotatedString,
): AnnotatedString =
    buildAnnotatedString {
        withStyle(SpanStyle(color = prefixColor)) { append(prefix) }
        append(code)
    }

/**
 * Syntax highlighting using SnipMeDev Highlights.
 *
 * We feed only the *code* portion (without diff prefixes) and then apply spans to an [AnnotatedString].
 * Diff backgrounds/accents remain handled by row UI.
 */
private fun highlightCode(
    code: String,
    lang: SyntaxLang,
    baseColor: Color,
): AnnotatedString {
    if (code.isBlank()) return AnnotatedString("")

    val syntaxLanguage =
        when (lang) {
            SyntaxLang.Kotlin -> SyntaxLanguage.KOTLIN
            SyntaxLang.TypeScript -> SyntaxLanguage.TYPESCRIPT
            SyntaxLang.JavaScript -> SyntaxLanguage.JAVASCRIPT
            SyntaxLang.Json -> null
            SyntaxLang.Yaml -> null
            SyntaxLang.Xml -> null
            SyntaxLang.Markdown -> null
            SyntaxLang.Plain -> null
        }

    if (syntaxLanguage == null) {
        return AnnotatedString(code, spanStyle = SpanStyle(color = baseColor))
    }

    return runCatching {
        val engine = HighlightsEngine.engineFor(syntaxLanguage)
        engine.setCode(code)
        val highlights: List<CodeHighlight> = engine.getHighlights()

        // `getHighlights()` returns a list of highlight ranges with colors (based on theme).
        // We'll paint baseColor first, then overlay theme colors.
        buildAnnotatedString {
            append(code)
            addStyle(SpanStyle(color = baseColor), 0, code.length)

            highlights.forEach { h ->
                val loc = h.location
                val start = loc.start.coerceIn(0, code.length)
                val end = loc.end.coerceIn(0, code.length)
                if (start >= end) return@forEach

                when (h) {
                    is ColorHighlight -> {
                        // rgb is 0xRRGGBB; Compose Color expects 0xAARRGGBB
                        val argb = 0xFF000000.toInt() or (h.rgb and 0x00FFFFFF)
                        addStyle(SpanStyle(color = Color(argb)), start, end)
                    }

                    is BoldHighlight -> {
                        addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), start, end)
                    }

                    else -> Unit
                }
            }
        }
    }.getOrElse {
        AnnotatedString(code, spanStyle = SpanStyle(color = baseColor))
    }
}

