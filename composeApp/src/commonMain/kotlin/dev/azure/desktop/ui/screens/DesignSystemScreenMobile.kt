package dev.azure.desktop.ui.screens

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DesignSystemScreenMobile(
    onBack: () -> Unit,
    onClearCache: () -> Result<Unit>,
    modifier: Modifier = Modifier,
) {
    DesignSystemScreenContent(
        onBack = onBack,
        onClearCache = onClearCache,
        modifier = modifier,
    )
}
