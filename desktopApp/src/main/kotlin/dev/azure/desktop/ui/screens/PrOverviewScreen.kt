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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Commit
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Pending
import androidx.compose.material.icons.outlined.Task
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azure.desktop.domain.pr.PullRequestDetail
import dev.azure.desktop.domain.pr.PullRequestReviewer
import dev.azure.desktop.theme.EditorialColors

@Composable
fun PrOverviewScreen(
    detail: PullRequestDetail,
    modifier: Modifier = Modifier,
) {
    val summary = detail.summary
    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(EditorialColors.surfaceContainerLow)
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = EditorialColors.primaryFixed,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        "ACTIVE",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = EditorialColors.onPrimaryFixed,
                        fontSize = 10.sp,
                    )
                }
                Text("PR #${summary.id}", style = MaterialTheme.typography.bodySmall, color = EditorialColors.outline, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                summary.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold),
                color = EditorialColors.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(EditorialColors.primaryContainer),
                )
                Text(summary.creatorDisplayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "wants to merge into ${summary.targetRefName.substringAfterLast("/")}" +
                        " from ${summary.sourceRefName.substringAfterLast("/")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = EditorialColors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = EditorialColors.surfaceContainerLowest),
                        elevation = CardDefaults.cardElevation(2.dp),
                    ) {
                        Column(Modifier.padding(24.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("DESCRIPTION", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                TextButton(onClick = { }) { Text("Edit", color = EditorialColors.primary, fontWeight = FontWeight.SemiBold) }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                detail.description?.takeIf { it.isNotBlank() } ?: "No description was provided.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = EditorialColors.onSurfaceVariant,
                            )
                        }
                    }
                    ActivityTimeline()
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    ReviewersCard(detail.reviewers)
                    LinkedItemsCard()
                    StatsCard()
                }
            }
            Spacer(Modifier.height(80.dp))
        }
        FloatingActionButton(
            onClick = { },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp),
            containerColor = EditorialColors.primaryContainer,
            contentColor = EditorialColors.onPrimary,
        ) {
            Icon(Icons.Outlined.Check, contentDescription = null)
        }
    }
}

@Composable
private fun ActivityTimeline() {
    Column {
        Text("ACTIVITY TIMELINE", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            TimelineComment()
            TimelineApproval()
            TimelineCommit()
        }
    }
}

@Composable
private fun TimelineComment() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Outlined.ModeComment, null, tint = EditorialColors.outline, modifier = Modifier.size(18.dp))
        Card(colors = CardDefaults.cardColors(containerColor = EditorialColors.surfaceContainerLowest), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Sarah Miller", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("2 hours ago", style = MaterialTheme.typography.bodySmall, color = EditorialColors.outline)
                    }
                    Icon(Icons.Outlined.MoreHoriz, null, tint = EditorialColors.outline)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "The retry logic on line 142 seems a bit aggressive for the database shard. Should we consider an exponential backoff instead?",
                    style = MaterialTheme.typography.bodySmall,
                    color = EditorialColors.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { }) { Text("Reply", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EditorialColors.primary) }
                    TextButton(onClick = { }) { Text("Resolve", fontSize = 11.sp, color = EditorialColors.outline) }
                }
            }
        }
    }
}

@Composable
private fun TimelineApproval() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.CheckCircle, null, tint = EditorialColors.primary, modifier = Modifier.size(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Alex Chen", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text("approved these changes", style = MaterialTheme.typography.bodySmall, color = EditorialColors.onSurfaceVariant)
            Text("1 hour ago", style = MaterialTheme.typography.bodySmall, color = EditorialColors.outline)
        }
    }
}

@Composable
private fun TimelineCommit() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Outlined.Commit, null, tint = EditorialColors.outline, modifier = Modifier.size(18.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = EditorialColors.surfaceContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("8f2a10c", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = EditorialColors.primary)
                    Text("chore: update Envoy proxy configuration schema", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
                Text("45 mins ago", style = MaterialTheme.typography.bodySmall, color = EditorialColors.outline)
            }
        }
    }
}

@Composable
private fun ReviewersCard(reviewers: List<PullRequestReviewer>) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(EditorialColors.surfaceContainerLowest)) {
        Column(Modifier.padding(20.dp)) {
            Text("REVIEWERS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            reviewers.take(6).forEachIndexed { index, reviewer ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(32.dp).clip(CircleShape).background(EditorialColors.surfaceContainerHigh))
                        Column {
                            Text(reviewer.displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(reviewer.uniqueName.orEmpty(), fontSize = 9.sp, color = EditorialColors.outline, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (reviewer.vote >= 10) Icons.Filled.CheckCircle else Icons.Outlined.Schedule,
                            null,
                            tint = if (reviewer.vote >= 10) EditorialColors.primary else EditorialColors.outline,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            if (reviewer.vote >= 10) "Approved" else "Waiting",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (reviewer.vote >= 10) EditorialColors.primary else EditorialColors.outline,
                        )
                    }
                }
                if (index != reviewers.lastIndex) Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun LinkedItemsCard() {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(EditorialColors.surfaceContainerLowest)) {
        Column {
            Column(Modifier.padding(20.dp)) {
                Text("LINKED ITEMS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                LinkedRow(Icons.Outlined.Task, EditorialColors.tertiary, "#2045: Service Mesh Latency", "User Story • In Progress")
                LinkedRow(Icons.Outlined.BugReport, EditorialColors.error, "#1982: Cluster Discovery Race", "Bug • Fixed")
            }
            Column(Modifier.background(EditorialColors.surfaceContainerHighest.copy(alpha = 0.5f)).padding(20.dp)) {
                Text("CHECKS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EditorialColors.outline)
                Spacer(Modifier.height(8.dp))
                CheckRow(Icons.Filled.CheckCircle, "Unit Tests", "PASSING")
                CheckRow(Icons.Filled.CheckCircle, "Static Analysis", "PASSING")
                CheckRow(Icons.Outlined.Pending, "Security Scan", "RUNNING")
            }
        }
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
private fun CheckRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, status: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (status == "RUNNING") EditorialColors.outline else EditorialColors.primary, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
        Text(status, fontSize = 10.sp, color = EditorialColors.outline, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatsCard() {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(EditorialColors.surfaceContainerLowest)) {
        Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCell(Modifier.weight(1f), "742", "ADDED")
            StatCell(Modifier.weight(1f), "128", "REMOVED")
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
