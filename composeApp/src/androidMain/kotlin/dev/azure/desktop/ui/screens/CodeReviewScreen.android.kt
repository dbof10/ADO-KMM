package dev.azure.desktop.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.azure.desktop.pr.review.CodeReviewStateMachine

@Composable
actual fun CodeReviewScreen(
    stateMachine: CodeReviewStateMachine,
    modifier: Modifier,
) {
    CodeReviewScreenMobile(stateMachine = stateMachine, modifier = modifier)
}
