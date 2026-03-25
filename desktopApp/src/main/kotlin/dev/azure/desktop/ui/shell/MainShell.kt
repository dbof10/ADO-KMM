package dev.azure.desktop.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
        AppScreen.CodeReview -> MainTab.Files
        AppScreen.DesignSystem -> null
        AppScreen.Login -> null
    }
    val topNav = when (screen) {
        AppScreen.PrList -> TopNavSection.PullRequests
        AppScreen.PrDetail -> TopNavSection.PullRequests
        AppScreen.CodeReview -> TopNavSection.Repositories
        AppScreen.DesignSystem -> TopNavSection.PullRequests
        AppScreen.Login -> TopNavSection.PullRequests
    }
    val showSearch = screen == AppScreen.PrList

    Row(Modifier.fillMaxSize().background(EditorialColors.surface)) {
        SideRail(
            selected = tab,
            onOverview = { onNavigate(AppScreen.PrList) },
            onFiles = { onNavigate(AppScreen.CodeReview) },
            onSettings = { onNavigate(AppScreen.DesignSystem) },
            onSignOut = onSignOut,
        )
        Column(Modifier.weight(1f).fillMaxSize()) {
            TopBar(
                showSearch = showSearch,
                activeTopNav = topNav,
                onTopNavSelect = { section ->
                    when (section) {
                        TopNavSection.PullRequests -> onNavigate(AppScreen.PrList)
                        TopNavSection.Repositories -> onNavigate(AppScreen.CodeReview)
                        TopNavSection.Pipelines -> { }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                content()
            }
        }
    }
}
