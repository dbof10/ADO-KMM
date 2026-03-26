package dev.azure.desktop.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azure.desktop.theme.EditorialColors

@Composable
internal fun LoginScreenDesktop(
    scroll: ScrollState,
    organization: String,
    onOrganizationChange: (String) -> Unit,
    pat: String,
    onPatChange: (String) -> Unit,
    idleError: String?,
    isWorking: Boolean,
    onSubmit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorialColors.surfaceContainerLow),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(5f)
                    .fillMaxHeight()
                    .background(
                        Brush.linearGradient(
                            listOf(EditorialColors.primary, EditorialColors.primaryContainer),
                        ),
                    )
                    .padding(40.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxHeight(),
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(
                                Icons.Outlined.AccountTree,
                                contentDescription = null,
                                tint = EditorialColors.onPrimary,
                                modifier = Modifier.size(36.dp),
                            )
                            Text(
                                text = "THE ENGINEERING EDITORIAL",
                                color = EditorialColors.onPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                        Spacer(Modifier.height(40.dp))
                        Text(
                            "Master your workflow with architectural precision.",
                            color = EditorialColors.onPrimary,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = EditorialColors.onPrimary,
                                lineHeight = 32.sp,
                            ),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "A sophisticated interface for managing Azure DevOps Pull Requests, designed for clarity and speed.",
                            color = EditorialColors.primaryFixed.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(260.dp),
                        )
                    }
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
                            repeat(3) { i ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            listOf(
                                                EditorialColors.primaryFixedDim,
                                                EditorialColors.primaryFixed,
                                                EditorialColors.inversePrimary,
                                            )[i],
                                        ),
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "TRUSTED BY SENIOR ENGINEERS",
                            color = EditorialColors.primaryFixed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            LoginFormPane(
                modifier = Modifier
                    .weight(7f)
                    .fillMaxHeight()
                    .background(EditorialColors.surfaceContainerLowest)
                    .verticalScroll(scroll)
                    .padding(horizontal = 40.dp, vertical = 48.dp),
                organization = organization,
                onOrganizationChange = onOrganizationChange,
                pat = pat,
                onPatChange = onPatChange,
                idleError = idleError,
                isWorking = isWorking,
                onSubmit = onSubmit,
                showHelpCard = true,
            )
        }
    }
}
