package dev.azure.desktop.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.azure.desktop.domain.pr.PullRequestDetail
import dev.azure.desktop.pr.review.CodeReviewStateMachine

@Composable
internal fun PrOverviewScreenDesktop(
    detail: PullRequestDetail,
    codeReviewStateMachine: CodeReviewStateMachine,
    isVoting: Boolean,
    voteErrorMessage: String?,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PrOverviewScreenContent(
        detail = detail,
        codeReviewStateMachine = codeReviewStateMachine,
        isVoting = isVoting,
        voteErrorMessage = voteErrorMessage,
        onApprove = onApprove,
        onReject = onReject,
        modifier = modifier,
        compactLayout = false,
    )
}
