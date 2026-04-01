package dev.azure.desktop.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Commit
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Pending
import androidx.compose.material.icons.outlined.Task
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azure.desktop.domain.pr.PullRequestDetail
import dev.azure.desktop.domain.pr.PullRequestMergeStrategy
import dev.azure.desktop.domain.pr.PullRequestCheckState
import dev.azure.desktop.domain.pr.PullRequestCheckStatus
import dev.azure.desktop.domain.pr.PullRequestLinkedWorkItem
import dev.azure.desktop.domain.pr.PullRequestReviewer
import dev.azure.desktop.domain.pr.PullRequestReviewerVote
import dev.azure.desktop.domain.pr.reviewerVoteDisplayLabel
import dev.azure.desktop.domain.pr.PullRequestTimelineItem
import dev.azure.desktop.domain.pr.isAutoCompleteEnabled
import dev.azure.desktop.domain.pr.uiDescription
import dev.azure.desktop.domain.pr.uiLabel
import dev.azure.desktop.deeplink.azureDevOpsPullRequestWebUrl
import dev.azure.desktop.platform.pullRequestBridge
import dev.azure.desktop.pr.review.CodeReviewStateMachine
import dev.azure.desktop.theme.EditorialColors
import dev.azure.desktop.ui.adaptive.LayoutClass
import dev.azure.desktop.ui.adaptive.layoutClassForWidth
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Composable
fun PrOverviewScreen(
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
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(EditorialColors.surfaceContainerLow),
    ) {
        val compactLayout = layoutClassForWidth(maxWidth) == LayoutClass.Compact
        if (compactLayout) {
            PrOverviewScreenMobile(
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
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            PrOverviewScreenDesktop(
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
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
internal fun PrOverviewScreenContent(
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
    compactLayout: Boolean,
) {
    val summary = detail.summary
    val reviewActionsEnabled =
        summary.status.equals("active", ignoreCase = true) && !isVoting && !isClosing && !isAutoCompleting
    val showAutoComplete =
        summary.status.equals("active", ignoreCase = true) && !detail.isAutoCompleteEnabled()
    val autoCompleteActionsEnabled = showAutoComplete && reviewActionsEnabled
    var showAutoCompleteDialog by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val prWebUrl =
        azureDevOpsPullRequestWebUrl(
            organization = organization,
            projectName = summary.projectName,
            repositoryName = summary.repositoryName,
            pullRequestId = summary.id,
        )
    var selectedTab by remember { mutableIntStateOf(0) }
    Column(modifier = modifier.fillMaxSize()) {
            Column(
                Modifier.padding(
                    start = if (compactLayout) 16.dp else 32.dp,
                    top = if (compactLayout) 16.dp else 32.dp,
                    end = if (compactLayout) 16.dp else 32.dp,
                    bottom = 8.dp,
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                        Surface(
                            color = EditorialColors.primaryFixed,
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                summary.status.uppercase(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = EditorialColors.onPrimaryFixed,
                                fontSize = 10.sp,
                            )
                        }
                        Text(
                            "PR #${summary.id}",
                            style = MaterialTheme.typography.bodySmall,
                            color = EditorialColors.outline,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = { clipboard.setText(AnnotatedString(prWebUrl)) },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Copy pull request link",
                            tint = EditorialColors.primary,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    summary.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = if (compactLayout) 22.sp else 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = EditorialColors.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                if (compactLayout) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(EditorialColors.primaryContainer),
                            )
                            Text(summary.creatorDisplayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        MergeBranchLine(
                            targetRefName = summary.targetRefName,
                            sourceRefName = summary.sourceRefName,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(EditorialColors.primaryContainer),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(summary.creatorDisplayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            MergeBranchLine(
                                targetRefName = summary.targetRefName,
                                sourceRefName = summary.sourceRefName,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                if (detail.isAutoCompleteEnabled()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = EditorialColors.primary.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = EditorialColors.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                "Auto-complete is enabled for this pull request.",
                                style = MaterialTheme.typography.bodySmall,
                                color = EditorialColors.onSurface,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (compactLayout) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = onReject,
                                enabled = reviewActionsEnabled,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Reject")
                            }
                            Button(
                                onClick = onApprove,
                                enabled = reviewActionsEnabled,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Approve")
                            }
                        }
                        TextButton(
                            onClick = onClosePr,
                            enabled = reviewActionsEnabled,
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                ButtonDefaults.textButtonColors(
                                    contentColor = EditorialColors.error,
                                    disabledContentColor = EditorialColors.outline,
                                ),
                        ) {
                            Text(if (isClosing) "Closing…" else "Close PR")
                        }
                        if (showAutoComplete) {
                            Button(
                                onClick = { showAutoCompleteDialog = true },
                                enabled = autoCompleteActionsEnabled,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Auto-complete")
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = onClosePr,
                                enabled = reviewActionsEnabled,
                                colors =
                                    ButtonDefaults.textButtonColors(
                                        contentColor = EditorialColors.error,
                                        disabledContentColor = EditorialColors.outline,
                                    ),
                            ) {
                                Text(if (isClosing) "Closing…" else "Close PR")
                            }
                            OutlinedButton(
                                onClick = onReject,
                                enabled = reviewActionsEnabled,
                            ) {
                                Text("Reject")
                            }
                            Button(
                                onClick = onApprove,
                                enabled = reviewActionsEnabled,
                            ) {
                                Text("Approve")
                            }
                            if (showAutoComplete) {
                                Button(
                                    onClick = { showAutoCompleteDialog = true },
                                    enabled = autoCompleteActionsEnabled,
                                ) {
                                    Text("Auto-complete")
                                }
                            }
                        }
                    }
                }
                if (!voteErrorMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        voteErrorMessage,
                        color = EditorialColors.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (!closeErrorMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        closeErrorMessage,
                        color = EditorialColors.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (!autoCompleteErrorMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        autoCompleteErrorMessage,
                        color = EditorialColors.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (isAutoCompleting) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = EditorialColors.primary,
                        )
                        Text(
                            "Enabling auto-complete…",
                            style = MaterialTheme.typography.bodySmall,
                            color = EditorialColors.onSurfaceVariant,
                        )
                    }
                }
            }
            SetAutoCompleteDialog(
                visible = showAutoCompleteDialog,
                onDismiss = { showAutoCompleteDialog = false },
                onConfirm = { strategy ->
                    showAutoCompleteDialog = false
                    onEnableAutoComplete(strategy)
                },
            )
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = EditorialColors.surfaceContainerLow,
                modifier = Modifier.padding(horizontal = if (compactLayout) 0.dp else 32.dp),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]).height(2.dp),
                        color = EditorialColors.primary,
                    )
                },
                divider = { },
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Overview", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Files", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium) },
                )
            }
            Spacer(Modifier.height(8.dp))
            when (selectedTab) {
                0 ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = if (compactLayout) 16.dp else 32.dp)
                            .padding(bottom = if (compactLayout) 16.dp else 32.dp),
                    ) {
                        if (compactLayout) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                DescriptionCard(detail.description, compactLayout = true)
                                ReviewersCard(detail.reviewers)
                                ActivityTimeline(detail.timeline, compactLayout = true)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                            ) {
                                Column(Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                    DescriptionCard(detail.description, compactLayout = false)
                                    ActivityTimeline(detail.timeline, compactLayout = false)
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                    ReviewersCard(detail.reviewers)
                                    LinkedItemsCard(
                                        linkedWorkItems = detail.linkedWorkItems,
                                        checks = detail.checks,
                                    )
                                    StatsCard(linesAdded = detail.linesAdded, linesRemoved = detail.linesRemoved)
                                }
                            }
                            Spacer(Modifier.height(80.dp))
                        }
                    }
                1 ->
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        CodeReviewScreen(
                            stateMachine = codeReviewStateMachine,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
            }
        }
}

@Composable
private fun SetAutoCompleteDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (PullRequestMergeStrategy) -> Unit,
) {
    if (!visible) return
    var selectedStrategy by remember { mutableStateOf(PullRequestMergeStrategy.Squash) }
    val optionScroll = rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Set auto-complete",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(Modifier.verticalScroll(optionScroll)) {
                Text(
                    "When this pull request can merge, Azure DevOps will complete it automatically using the merge type below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = EditorialColors.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Merge type",
                    style = MaterialTheme.typography.labelLarge,
                    color = EditorialColors.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                PullRequestMergeStrategy.entries.forEach { strategy ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = strategy == selectedStrategy,
                                onClick = { selectedStrategy = strategy },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = strategy == selectedStrategy,
                            onClick = null,
                        )
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(
                                strategy.uiLabel(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                strategy.uiDescription(),
                                style = MaterialTheme.typography.bodySmall,
                                color = EditorialColors.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedStrategy) }) {
                Text("Enable auto-complete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun MergeBranchLine(
    targetRefName: String,
    sourceRefName: String,
    modifier: Modifier = Modifier,
) {
    val targetBranch = targetRefName.substringAfterLast("/")
    val sourceBranch = sourceRefName.substringAfterLast("/")
    val baseStyle = SpanStyle(color = EditorialColors.onSurfaceVariant)
    val branchStyle =
        SpanStyle(
            color = EditorialColors.primary,
            fontWeight = FontWeight.SemiBold,
        )
    val annotated =
        buildAnnotatedString {
            withStyle(baseStyle) { append("wants to merge into ") }
            withStyle(branchStyle) { append(targetBranch) }
            withStyle(baseStyle) { append(" from ") }
            withStyle(branchStyle) { append(sourceBranch) }
        }
    Text(
        annotated,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun DescriptionCard(description: String?, compactLayout: Boolean) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EditorialColors.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.padding(if (compactLayout) 16.dp else 24.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("DESCRIPTION", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (!compactLayout) {
                    TextButton(onClick = { }) { Text("Edit", color = EditorialColors.primary, fontWeight = FontWeight.SemiBold) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                description?.takeIf { it.isNotBlank() } ?: "No description was provided.",
                style = MaterialTheme.typography.bodyMedium,
                color = EditorialColors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActivityTimeline(items: List<PullRequestTimelineItem>, compactLayout: Boolean) {
    Column {
        Text("ACTIVITY TIMELINE", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (items.isEmpty()) {
                Text(
                    "No activity yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = EditorialColors.outline,
                    modifier = Modifier.padding(start = 4.dp),
                )
            } else {
                items.take(25).forEach { item ->
                    when (item) {
                        is PullRequestTimelineItem.Comment -> TimelineComment(item, compactLayout)
                        is PullRequestTimelineItem.Approval -> TimelineApproval(item, compactLayout)
                        is PullRequestTimelineItem.Commit -> TimelineCommit(item, compactLayout)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineComment(item: PullRequestTimelineItem.Comment, compactLayout: Boolean) {
    val isSystemActor = isMicrosoftSystemTimelineActor(item.actorDisplayName)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Outlined.ModeComment, null, tint = EditorialColors.outline, modifier = Modifier.size(18.dp))
        Card(colors = CardDefaults.cardColors(containerColor = EditorialColors.surfaceContainerLowest), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isSystemActor) Arrangement.Start else Arrangement.SpaceBetween,
                ) {
                    if (compactLayout) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.actorDisplayName.ifBlank { "Unknown" }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            relativeTimeLabel(item.createdDateIso)?.let { label ->
                                Text(label, style = MaterialTheme.typography.bodySmall, color = EditorialColors.outline)
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(item.actorDisplayName.ifBlank { "Unknown" }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            relativeTimeLabel(item.createdDateIso)?.let { label ->
                                Text(label, style = MaterialTheme.typography.bodySmall, color = EditorialColors.outline)
                            }
                        }
                    }
                    if (!isSystemActor) {
                        Icon(Icons.Outlined.MoreHoriz, null, tint = EditorialColors.outline)
                    }
                }
                Spacer(Modifier.height(8.dp))
                TimelineCommentBody(item.content)
                if (!isSystemActor) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { }) { Text("Reply", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EditorialColors.primary) }
                        TextButton(onClick = { }) { Text("Resolve", fontSize = 11.sp, color = EditorialColors.outline) }
                    }
                }
            }
        }
    }
}

private sealed class TimelineCommentPart {
    data class Text(val value: String) : TimelineCommentPart()

    data class Image(val alt: String, val url: String) : TimelineCommentPart()
}

@Composable
private fun TimelineCommentBody(content: String) {
    val parts = remember(content) { parseTimelineCommentParts(content) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parts.forEach { part ->
            when (part) {
                is TimelineCommentPart.Text ->
                    if (part.value.isNotBlank()) {
                        Text(
                            part.value,
                            style = MaterialTheme.typography.bodySmall,
                            color = EditorialColors.onSurfaceVariant,
                        )
                    }

                is TimelineCommentPart.Image ->
                    TimelineCommentMarkdownImage(url = part.url, alt = part.alt)
            }
        }
    }
}

@Composable
private fun TimelineCommentMarkdownImage(url: String, alt: String) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    var loading by remember(url) { mutableStateOf(true) }
    LaunchedEffect(url) {
        loading = true
        bitmap = null
        val bytes = pullRequestBridge.fetchAuthenticatedDevOpsResource(url).getOrNull()
        bitmap = bytes?.decodeTimelineImageOrNull()
        loading = false
    }
    val description = alt.ifBlank { "Comment image" }
    val decoded = bitmap
    when {
        loading ->
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = EditorialColors.primary,
            )

        decoded != null ->
            Image(
                bitmap = decoded,
                contentDescription = description,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit,
            )

        else ->
            Text(
                "![$alt]($url)",
                style = MaterialTheme.typography.bodySmall,
                color = EditorialColors.outline,
            )
    }
}

private val markdownImageRegex = Regex("""!\[([^\]]*)\]\(([^)]+)\)""")

private fun parseTimelineCommentParts(content: String): List<TimelineCommentPart> {
    if (!markdownImageRegex.containsMatchIn(content)) {
        return listOf(TimelineCommentPart.Text(formatTimelineContent(content)))
    }
    val parts = mutableListOf<TimelineCommentPart>()
    var i = 0
    for (match in markdownImageRegex.findAll(content)) {
        if (match.range.first > i) {
            val slice = content.substring(i, match.range.first)
            if (slice.isNotEmpty()) {
                parts += TimelineCommentPart.Text(formatTimelineContent(slice))
            }
        }
        parts += TimelineCommentPart.Image(alt = match.groupValues[1], url = match.groupValues[2].trim())
        i = match.range.last + 1
    }
    if (i < content.length) {
        val tail = content.substring(i)
        if (tail.isNotEmpty()) {
            parts += TimelineCommentPart.Text(formatTimelineContent(tail))
        }
    }
    return parts
}

/** Azure DevOps posts reviewer join/vote system lines under identities like [Microsoft.VisualStudio.Services.TFS]. */
private fun isMicrosoftSystemTimelineActor(displayName: String): Boolean {
    val name = displayName.trim()
    if (name.isEmpty()) return false
    return name.startsWith("Microsoft.", ignoreCase = true)
}

private val voteLineRegex = Regex("""\bvoted\s+(-?\d+)\b""", RegexOption.IGNORE_CASE)

private fun formatTimelineContent(content: String): String {
    val match = voteLineRegex.find(content) ?: return content
    val vote = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return content
    val voteLabel = voteLabel(vote)
    return content.replace(voteLineRegex) { mr ->
        val prefix = mr.value.substringBefore("voted", missingDelimiterValue = "")
        "${prefix}voted $voteLabel"
    }
}

private fun voteLabel(vote: Int): String = reviewerVoteDisplayLabel(vote)

@Composable
private fun TimelineApproval(item: PullRequestTimelineItem.Approval, compactLayout: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.CheckCircle, null, tint = EditorialColors.primary, modifier = Modifier.size(20.dp))
        if (compactLayout) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.actorDisplayName.ifBlank { "Unknown" }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text("approved these changes", style = MaterialTheme.typography.bodySmall, color = EditorialColors.onSurfaceVariant)
                relativeTimeLabel(item.createdDateIso)?.let { label ->
                    Text(label, style = MaterialTheme.typography.bodySmall, color = EditorialColors.outline)
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.actorDisplayName.ifBlank { "Unknown" }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text("approved these changes", style = MaterialTheme.typography.bodySmall, color = EditorialColors.onSurfaceVariant)
                relativeTimeLabel(item.createdDateIso)?.let { label ->
                    Text(label, style = MaterialTheme.typography.bodySmall, color = EditorialColors.outline)
                }
            }
        }
    }
}

@Composable
private fun TimelineCommit(item: PullRequestTimelineItem.Commit, compactLayout: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Outlined.Commit, null, tint = EditorialColors.outline, modifier = Modifier.size(18.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = EditorialColors.surfaceContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (compactLayout) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.commitId, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = EditorialColors.primary)
                    Text(item.message, style = MaterialTheme.typography.bodySmall)
                    relativeTimeLabel(item.createdDateIso)?.let { label ->
                        Text(label, style = MaterialTheme.typography.bodySmall, color = EditorialColors.outline)
                    }
                }
            } else {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.commitId, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = EditorialColors.primary)
                        Text(item.message, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                    relativeTimeLabel(item.createdDateIso)?.let { label ->
                        Text(label, style = MaterialTheme.typography.bodySmall, color = EditorialColors.outline)
                    }
                }
            }
        }
    }
}

private fun relativeTimeLabel(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    val instant = try {
        Instant.parse(iso)
    } catch (_: Throwable) {
        return null
    }
    val now = Clock.System.now()
    val seconds = (now.toEpochMilliseconds() - instant.toEpochMilliseconds()) / 1000
    if (seconds < 0) return null
    return when {
        seconds < 60 -> "just now"
        seconds < 60 * 60 -> "${seconds / 60} mins ago"
        seconds < 60 * 60 * 24 -> "${seconds / (60 * 60)} hours ago"
        else -> "${seconds / (60 * 60 * 24)} days ago"
    }
}

@Composable
private fun ReviewersCard(reviewers: List<PullRequestReviewer>) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(EditorialColors.surfaceContainerLowest)) {
        Column(Modifier.padding(20.dp)) {
            Text("REVIEWERS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            reviewers.take(6).forEachIndexed { index, reviewer ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(32.dp).clip(CircleShape).background(EditorialColors.surfaceContainerHigh))
                        Column(Modifier.fillMaxWidth()) {
                            val readableName = reviewer.readableName()
                            Text(
                                readableName,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                reviewer.uniqueName.orEmpty(),
                                fontSize = 9.sp,
                                color = EditorialColors.outline,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    ReviewerStatusBadge(vote = reviewer.vote)
                }
                if (index != reviewers.lastIndex) Spacer(Modifier.height(12.dp))
            }
        }
    }
}

private fun PullRequestReviewer.readableName(): String {
    val raw = displayName.ifBlank { uniqueName.orEmpty() }.trim()
    if (raw.isBlank()) return "Unknown reviewer"

    val slashSegment = raw.substringAfterLast("\\", raw)
    val normalized = slashSegment
        .replace('_', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()

    return normalized.ifBlank { raw }
}

@Composable
private fun ReviewerStatusBadge(vote: Int) {
    val label = reviewerVoteDisplayLabel(vote)
    val (tint, icon) =
        when (vote) {
            in PullRequestReviewerVote.APPROVED..Int.MAX_VALUE ->
                EditorialColors.primary to Icons.Filled.CheckCircle
            PullRequestReviewerVote.APPROVED_WITH_SUGGESTIONS ->
                EditorialColors.tertiary to Icons.Outlined.Check
            PullRequestReviewerVote.REJECTED ->
                EditorialColors.error to Icons.Outlined.ErrorOutline
            PullRequestReviewerVote.WAITING_FOR_AUTHOR ->
                EditorialColors.tertiary to Icons.Outlined.PersonOutline
            PullRequestReviewerVote.NO_VOTE ->
                EditorialColors.outline to Icons.Outlined.Schedule
            else -> EditorialColors.outline to Icons.Outlined.Schedule
        }

    val badgeSurfaceBase =
        when (vote) {
            in PullRequestReviewerVote.APPROVED..Int.MAX_VALUE -> EditorialColors.primary
            PullRequestReviewerVote.APPROVED_WITH_SUGGESTIONS -> EditorialColors.tertiary
            else -> EditorialColors.surfaceContainerHighest
        }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = badgeSurfaceBase.copy(alpha = 0.12f),
        contentColor = tint,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp),
            )
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = tint,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

@Composable
private fun LinkedItemsCard(
    linkedWorkItems: List<PullRequestLinkedWorkItem>,
    checks: List<PullRequestCheckStatus>,
) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(EditorialColors.surfaceContainerLowest)) {
        Column {
            Column(Modifier.padding(20.dp)) {
                Text("LINKED ITEMS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                if (linkedWorkItems.isEmpty()) {
                    Text(
                        "No linked items.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EditorialColors.outline,
                    )
                } else {
                    linkedWorkItems.take(6).forEach { item ->
                        val type = item.type.orEmpty()
                        val isBug = type.equals("bug", ignoreCase = true)
                        val icon = if (isBug) Icons.Outlined.BugReport else Icons.Outlined.Task
                        val tint = if (isBug) EditorialColors.error else EditorialColors.tertiary
                        val title = buildString {
                            append("#${item.id}")
                            item.title?.takeIf { it.isNotBlank() }?.let { append(": ").append(it) }
                        }
                        val subtitle = listOfNotNull(item.type?.takeIf { it.isNotBlank() }, item.state?.takeIf { it.isNotBlank() })
                            .joinToString(" • ")
                            .ifBlank { "Work Item" }
                        LinkedRow(icon, tint, title, subtitle)
                    }
                }
            }
            Column(Modifier.padding(20.dp)) {
                Text("CHECKS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EditorialColors.outline)
                Spacer(Modifier.height(8.dp))
                if (checks.isEmpty()) {
                    Text(
                        "No checks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EditorialColors.outline,
                    )
                } else {
                    checks.take(6).forEach { check ->
                        val (icon, statusLabel, tint) = check.toUi()
                        CheckRow(icon, check.name, statusLabel, tint)
                    }
                }
            }
        }
    }
}

private fun PullRequestCheckStatus.toUi(): Triple<androidx.compose.ui.graphics.vector.ImageVector, String, androidx.compose.ui.graphics.Color> {
    return when (state) {
        PullRequestCheckState.Succeeded -> Triple(Icons.Filled.CheckCircle, "PASSING", EditorialColors.primary)
        PullRequestCheckState.Failed -> Triple(Icons.Outlined.ErrorOutline, "FAILING", EditorialColors.error)
        PullRequestCheckState.Running -> Triple(Icons.Outlined.Pending, "RUNNING", EditorialColors.outline)
        PullRequestCheckState.Pending -> Triple(Icons.Outlined.Pending, "PENDING", EditorialColors.outline)
        PullRequestCheckState.NotApplicable -> Triple(Icons.Outlined.Pending, "N/A", EditorialColors.outline)
        PullRequestCheckState.Unknown -> Triple(Icons.Outlined.Pending, "UNKNOWN", EditorialColors.outline)
    }
}

@Composable
private fun LinkedRow(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color, title: String, subtitle: String) {
    Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Text(subtitle, fontSize = 10.sp, color = EditorialColors.outline)
        }
    }
}

@Composable
private fun CheckRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    status: String,
    tint: androidx.compose.ui.graphics.Color,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
        Text(status, fontSize = 10.sp, color = EditorialColors.outline, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatsCard(linesAdded: Int?, linesRemoved: Int?) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(EditorialColors.surfaceContainerLowest)) {
        Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCell(Modifier.weight(1f), linesAdded?.toString() ?: "—", "ADDED")
            StatCell(Modifier.weight(1f), linesRemoved?.toString() ?: "—", "REMOVED")
        }
    }
}

@Composable
private fun StatCell(modifier: Modifier, value: String, label: String) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = EditorialColors.surfaceContainerLow,
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = EditorialColors.outline)
        }
    }
}
