package dev.azure.desktop.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.azure.desktop.navigation.AppScreen
import dev.azure.desktop.theme.EditorialColors
import dev.azure.desktop.ui.adaptive.LayoutClass
import dev.azure.desktop.ui.adaptive.layoutClassForWidth

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
        AppScreen.Settings -> MainTab.Settings
        AppScreen.Login -> null
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(EditorialColors.surface)) {
        val compactLayout = layoutClassForWidth(maxWidth) != LayoutClass.Expanded
        if (compactLayout) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxSize()) {
                    content()
                }
                if (tab != null) {
                    NavigationBar(containerColor = EditorialColors.surfaceContainerLowest) {
                        NavigationBarItem(
                            selected = tab == MainTab.Overview,
                            onClick = { onNavigate(AppScreen.PrList) },
                            icon = { Icon(Icons.Outlined.Dashboard, contentDescription = "Overview") },
                            label = { Text("Overview") },
                        )
                        NavigationBarItem(
                            selected = tab == MainTab.Releases,
                            onClick = { onNavigate(AppScreen.ReleaseList) },
                            icon = { Icon(Icons.Outlined.RocketLaunch, contentDescription = "Releases") },
                            label = { Text("Releases") },
                        )
                        NavigationBarItem(
                            selected = tab == MainTab.Settings,
                            onClick = { onNavigate(AppScreen.Settings) },
                            icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") },
                        )
                    }
                }
            }
        } else {
            Row(Modifier.fillMaxSize().background(EditorialColors.surface)) {
                SideRail(
                    selected = tab,
                    onOverview = { onNavigate(AppScreen.PrList) },
                    onReleases = { onNavigate(AppScreen.ReleaseList) },
                    onSettings = { onNavigate(AppScreen.Settings) },
                    onSignOut = onSignOut,
                )
                Box(Modifier.weight(1f).fillMaxSize()) {
                    content()
                }
            }
        }
    }
}
