package dev.azure.desktop.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.azure.desktop.theme.EditorialColors

private val RailWidth = 80.dp

@Composable
internal fun SideRail(
    selected: MainTab?,
    onOverview: () -> Unit,
    onFiles: () -> Unit,
    onSettings: () -> Unit,
    onSignOut: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(RailWidth)
            .fillMaxHeight()
            .background(EditorialColors.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Architecture,
                contentDescription = null,
                tint = EditorialColors.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.height(8.dp))
            RailIcon(
                selected = selected == MainTab.Overview,
                icon = Icons.Outlined.Dashboard,
                contentDescription = "Overview",
                onClick = onOverview,
            )
            RailIcon(
                selected = selected == MainTab.Files,
                icon = Icons.Outlined.Description,
                contentDescription = "Files",
                onClick = onFiles,
            )
            RailIcon(
                selected = false,
                icon = Icons.Outlined.History,
                contentDescription = "Commits",
                onClick = { /* UI placeholder — no route */ },
            )
            RailIcon(
                selected = false,
                icon = Icons.Outlined.Sync,
                contentDescription = "Updates",
                onClick = { /* UI placeholder */ },
            )
        }
        Column(
            modifier = Modifier.padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RailIcon(
                selected = false,
                icon = Icons.Outlined.Settings,
                contentDescription = "Design system",
                onClick = onSettings,
            )
            RailIcon(
                selected = false,
                icon = Icons.Outlined.HelpOutline,
                contentDescription = "Help",
                onClick = { },
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSignOut,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Logout,
                    contentDescription = "Sign out",
                    tint = EditorialColors.outline,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun RailIcon(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val bg = if (selected) EditorialColors.primary.copy(alpha = 0.1f) else Color.Transparent
    val tint = if (selected) EditorialColors.primary else EditorialColors.outline
    Box(
        modifier = Modifier
            .width(56.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.align(Alignment.Center).size(24.dp),
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(4.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(EditorialColors.primary),
            )
        }
    }
}
