package dev.azure.desktop.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.azure.desktop.domain.pr.PullRequestDetail
import dev.azure.desktop.domain.pr.PullRequestMergeStrategy
import dev.azure.desktop.pr.review.CodeReviewStateMachine

@Composable
internal fun PrOverviewScreenMobile(
    organization: String,
    detail: PullRequestDetail,
    codeReviewStateMachine: CodeReviewStateMachine,
    isVoting: Boolean,
    voteErrorMessage: String?,
    isClosing: Boolean,
    closeErrorMessage: String?,
    isAutoCompleting: Boolean,
    autoCompleteErrorMessage: String?,
    onBack: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onClosePr: () -> Unit,
    onEnableAutoComplete: (PullRequestMergeStrategy) -> Unit,
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
        isAutoCompleting = isAutoCompleting,
        autoCompleteErrorMessage = autoCompleteErrorMessage,
        onBack = onBack,
        onApprove = onApprove,
        onReject = onReject,
        onClosePr = onClosePr,
        onEnableAutoComplete = onEnableAutoComplete,
        modifier = modifier,
        compactLayout = true,
    )
}
