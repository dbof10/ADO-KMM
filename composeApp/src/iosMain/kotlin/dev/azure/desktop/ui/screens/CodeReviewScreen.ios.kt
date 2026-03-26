package dev.azure.desktop.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.azure.desktop.pr.review.CodeReviewState
import dev.azure.desktop.pr.review.CodeReviewStateMachine
import dev.azure.desktop.theme.EditorialColors

@Composable
actual fun CodeReviewScreen(
    stateMachine: CodeReviewStateMachine,
    modifier: Modifier,
) {
    var state: CodeReviewState by remember(stateMachine) { mutableStateOf(CodeReviewState.Loading) }
    LaunchedEffect(stateMachine) { stateMachine.state.collect { state = it } }

    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            when (val s = state) {
                is CodeReviewState.Loading -> "Loading diff…"
                is CodeReviewState.Error -> s.message
                is CodeReviewState.Content ->
                    "Code review with syntax highlighting is optimized on desktop. " +
                        "Use the desktop app for the full side-by-side diff."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = EditorialColors.onSurfaceVariant,
        )
    }
}
