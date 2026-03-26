package dev.azure.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.azure.desktop.theme.EditorialColors
import dev.azure.desktop.ui.adaptive.LayoutClass
import dev.azure.desktop.ui.adaptive.layoutClassForWidth

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    organization: String,
    patToken: String,
    onSignOut: () -> Unit,
    onClearCache: () -> Result<Unit>,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val compactLayout = layoutClassForWidth(maxWidth) == LayoutClass.Compact
        if (compactLayout) {
            SettingsScreenMobile(
                organization = organization,
                patToken = patToken,
                onSignOut = onSignOut,
                onClearCache = onClearCache,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            SettingsScreenDesktop(
                organization = organization,
                patToken = patToken,
                onSignOut = onSignOut,
                onClearCache = onClearCache,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsScreenContent(
    organization: String,
    patToken: String,
    onSignOut: () -> Unit,
    onClearCache: () -> Result<Unit>,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    var cacheClearMessage by remember { mutableStateOf<String?>(null) }
    var showPatToken by remember { mutableStateOf(false) }
    val renderedPat = if (showPatToken) patToken else maskToken(patToken)

    Column(
        modifier
            .fillMaxSize()
            .background(EditorialColors.surfaceContainerLow)
            .verticalScroll(scroll)
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Manage your current session and local data.",
            style = MaterialTheme.typography.bodyMedium,
            color = EditorialColors.onSurfaceVariant,
        )

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = EditorialColors.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Current organization", style = MaterialTheme.typography.labelMedium, color = EditorialColors.onSurfaceVariant)
                SelectionContainer {
                    Text(
                        text = if (organization.isBlank()) "(not set)" else organization,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = EditorialColors.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("PAT token", style = MaterialTheme.typography.labelMedium, color = EditorialColors.onSurfaceVariant)
                SelectionContainer {
                    Text(
                        text = if (patToken.isBlank()) "(not set)" else renderedPat,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (patToken.isNotBlank()) {
                    TextButton(onClick = { showPatToken = !showPatToken }) {
                        Text(if (showPatToken) "Mask token" else "Unmask token", color = EditorialColors.primary)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    cacheClearMessage = onClearCache().fold(
                        onSuccess = { "Local cache cleared. Login credentials were kept." },
                        onFailure = { "Failed to clear cache: ${it.message ?: "Unknown error"}" },
                    )
                },
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Clear cache")
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = onSignOut,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Log out")
            }
        }

        cacheClearMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = EditorialColors.onSurfaceVariant,
            )
        }
    }
}

private fun maskToken(token: String): String {
    if (token.isBlank()) return token
    if (token.length <= 8) return "*".repeat(token.length)
    val visiblePrefix = token.take(4)
    val visibleSuffix = token.takeLast(4)
    return "$visiblePrefix${"*".repeat(token.length - 8)}$visibleSuffix"
}
