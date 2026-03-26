package dev.azure.desktop.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.azure.desktop.navigation.AppScreen
import dev.azure.desktop.theme.EditorialColors

@Composable
internal fun MainShell(
    screen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    onSignOut: () -> Unit,
    content: @Composable () -> Unit,
) {
    val tab: MainTab? = when (screen) {
        AppScreen.PrList -> MainTab.Overview
        AppScreen.PrDetail -> MainTab.Overview
        AppScreen.CodeReview -> null
        AppScreen.ReleaseList -> MainTab.Releases
        AppScreen.ReleaseDetail -> MainTab.Releases
        AppScreen.DesignSystem -> null
        AppScreen.Login -> null
    }
    Row(Modifier.fillMaxSize().background(EditorialColors.surface)) {
        SideRail(
            selected = tab,
            onOverview = { onNavigate(AppScreen.PrList) },
            onReleases = { onNavigate(AppScreen.ReleaseList) },
            onSettings = { onNavigate(AppScreen.DesignSystem) },
            onSignOut = onSignOut,
        )
        Box(Modifier.weight(1f).fillMaxSize()) {
            content()
        }
    }
}
