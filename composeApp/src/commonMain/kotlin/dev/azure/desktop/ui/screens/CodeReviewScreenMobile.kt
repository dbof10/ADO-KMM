package dev.azure.desktop.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azure.desktop.domain.pr.PullRequestDiffLine
import dev.azure.desktop.pr.review.CodeReviewState
import dev.azure.desktop.pr.review.CodeReviewStateMachine
import dev.azure.desktop.theme.EditorialColors

@Composable
internal fun CodeReviewScreenMobile(
    stateMachine: CodeReviewStateMachine,
    modifier: Modifier = Modifier,
) {
    var state: CodeReviewState by remember(stateMachine) { mutableStateOf(CodeReviewState.Loading) }
    LaunchedEffect(stateMachine) { stateMachine.state.collect { state = it } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorialColors.surfaceContainerLow)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (val current = state) {
            CodeReviewState.Loading -> Text("Loading diff...", color = EditorialColors.outline)
            is CodeReviewState.Error -> Text(current.message, color = EditorialColors.error)
            is CodeReviewState.Content -> {
                val selectedPath = current.selectedPath
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(EditorialColors.surfaceContainerLowest),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("FILE", style = MaterialTheme.typography.labelSmall, color = EditorialColors.outline)
                        Text(
                            selectedPath ?: "Select a file on desktop for full navigation.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EditorialColors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "+${current.diffLines.count { it is PullRequestDiffLine.Added }} " +
                                "-${current.diffLines.count { it is PullRequestDiffLine.Removed }}",
                            style = MaterialTheme.typography.labelMedium,
                            color = EditorialColors.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (selectedPath == null) {
                    Text(
                        "Mobile shows a focused inline diff view. Select a file from desktop/tablet to inspect.",
                        color = EditorialColors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else if (current.isDiffLoading) {
                    Text("Loading file diff...", color = EditorialColors.outline)
                } else if (current.diffLines.isEmpty()) {
                    Text("No diff to display.", color = EditorialColors.outline)
                } else {
                    current.diffLines.take(220).forEach { line ->
                        MobileDiffRow(line = line)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun MobileDiffRow(line: PullRequestDiffLine) {
    val style =
        when (line) {
            is PullRequestDiffLine.Context ->
                MobileDiffStyle(
                    prefix = " ",
                    bg = EditorialColors.surfaceContainerLowest,
                    textColor = EditorialColors.onSurfaceVariant,
                    lineNo = (line.newLineNumber ?: line.oldLineNumber)?.toString().orEmpty(),
                    accentColor = EditorialColors.surfaceContainerLowest,
                )
            is PullRequestDiffLine.Added ->
                MobileDiffStyle(
                    prefix = "+",
                    bg = EditorialColors.primaryFixed.copy(alpha = 0.24f),
                    textColor = EditorialColors.onPrimaryFixedVariant,
                    lineNo = line.newLineNumber?.toString().orEmpty(),
                    accentColor = EditorialColors.primary,
                )
            is PullRequestDiffLine.Removed ->
                MobileDiffStyle(
                    prefix = "-",
                    bg = EditorialColors.errorContainer.copy(alpha = 0.28f),
                    textColor = EditorialColors.onErrorContainer,
                    lineNo = line.oldLineNumber?.toString().orEmpty(),
                    accentColor = EditorialColors.error,
                )
        }
    val lineText =
        when (line) {
            is PullRequestDiffLine.Context -> line.text
            is PullRequestDiffLine.Added -> line.text
            is PullRequestDiffLine.Removed -> line.text
        }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(style.bg)
            .padding(vertical = 3.dp),
    ) {
        Box(Modifier.width(2.dp).height(22.dp).background(style.accentColor))
        Text(
            style.lineNo,
            modifier = Modifier.width(38.dp).padding(start = 8.dp, end = 6.dp),
            color = EditorialColors.outline,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        )
        Text(
            "${style.prefix} $lineText",
            color = style.textColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class MobileDiffStyle(
    val prefix: String,
    val bg: androidx.compose.ui.graphics.Color,
    val textColor: androidx.compose.ui.graphics.Color,
    val lineNo: String,
    val accentColor: androidx.compose.ui.graphics.Color,
)
