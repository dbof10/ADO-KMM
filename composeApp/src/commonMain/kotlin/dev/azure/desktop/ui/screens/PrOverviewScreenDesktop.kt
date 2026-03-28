package dev.azure.desktop.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.azure.desktop.domain.pr.PullRequestDetail
import dev.azure.desktop.pr.review.CodeReviewStateMachine

@Composable
internal fun PrOverviewScreenDesktop(
    organization: String,
    detail: PullRequestDetail,
    codeReviewStateMachine: CodeReviewStateMachine,
    isVoting: Boolean,
    voteErrorMessage: String?,
    isClosing: Boolean,
    closeErrorMessage: String?,
    onBack: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onClosePr: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PrOverviewScreenContent(
        organization = organization,
        detail = detail,
        codeReviewStateMachine = codeReviewStateMachine,
        isVoting = isVoting,
        voteErrorMessage = voteErrorMessage,
        isClosing = isClosing,
        closeErrorMessage = closeErrorMessage,
        onBack = onBack,
        onApprove = onApprove,
        onReject = onReject,
        onClosePr = onClosePr,
        modifier = modifier,
        compactLayout = false,
    )
}
