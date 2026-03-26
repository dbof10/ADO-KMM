package dev.azure.desktop.ui.screens

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsScreenDesktop(
    organization: String,
    patToken: String,
    onSignOut: () -> Unit,
    onClearCache: () -> Result<Unit>,
    modifier: Modifier = Modifier,
) {
    SettingsScreenContent(
        organization = organization,
        patToken = patToken,
        onSignOut = onSignOut,
        onClearCache = onClearCache,
        modifier = modifier,
    )
}
