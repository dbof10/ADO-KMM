package dev.azure.desktop.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.azure.desktop.theme.EditorialColors

enum class TopNavSection {
    PullRequests,
    Repositories,
    Pipelines,
}

@Composable
internal fun TopBar(
    showSearch: Boolean,
    activeTopNav: TopNavSection,
    onTopNavSelect: (TopNavSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(1.dp, RoundedCornerShape(0.dp))
            .background(EditorialColors.surface.copy(alpha = 0.92f))
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "The Engineering Editorial",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = EditorialColors.onSurface,
            modifier = Modifier.widthIn(max = 220.dp),
        )
        if (showSearch) {
            OutlinedTextField(
                value = "",
                onValueChange = { },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                placeholder = { Text("Search work items...", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = EditorialColors.outline)
                },
                singleLine = true,
                readOnly = true,
                enabled = false,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = EditorialColors.onSurface,
                    disabledBorderColor = EditorialColors.outlineVariant.copy(alpha = 0.25f),
                    disabledContainerColor = EditorialColors.surfaceContainer,
                ),
            )
        } else {
            Row(modifier = Modifier.weight(1f)) { }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopNavLink(
                label = "Pull Requests",
                selected = activeTopNav == TopNavSection.PullRequests,
                onClick = { onTopNavSelect(TopNavSection.PullRequests) },
            )
            TopNavLink(
                label = "Repositories",
                selected = activeTopNav == TopNavSection.Repositories,
                onClick = { onTopNavSelect(TopNavSection.Repositories) },
            )
            TopNavLink(
                label = "Pipelines",
                selected = activeTopNav == TopNavSection.Pipelines,
                onClick = { onTopNavSelect(TopNavSection.Pipelines) },
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                tint = EditorialColors.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                tint = EditorialColors.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EditorialColors.primaryContainer,
                    contentColor = EditorialColors.onPrimary,
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp),
            ) {
                Text("Create PR", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun TopNavLink(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) EditorialColors.primary else EditorialColors.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        )
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = if (selected) EditorialColors.primary else EditorialColors.surface,
        )
    }
}
