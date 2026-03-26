package dev.azure.desktop.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.azure.desktop.pr.review.CodeReviewStateMachine

@Composable
expect fun CodeReviewScreen(
    stateMachine: CodeReviewStateMachine,
    modifier: Modifier = Modifier,
)
