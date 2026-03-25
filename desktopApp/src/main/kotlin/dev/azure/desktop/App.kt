package dev.azure.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.azure.desktop.navigation.AppScreen
import dev.azure.desktop.theme.EditorialTheme
import dev.azure.desktop.ui.screens.CodeReviewScreen
import dev.azure.desktop.ui.screens.DesignSystemScreen
import dev.azure.desktop.ui.screens.LoginScreen
import dev.azure.desktop.ui.screens.PrOverviewScreen
import dev.azure.desktop.ui.shell.MainShell

@Composable
fun App() {
    EditorialTheme {
        val screen = remember { mutableStateOf(AppScreen.Login) }

        when (screen.value) {
            AppScreen.Login ->
                LoginScreen(
                    onConnect = { screen.value = AppScreen.PrOverview },
                )

            AppScreen.PrOverview ->
                MainShell(
                    screen = AppScreen.PrOverview,
                    onNavigate = { screen.value = it },
                    onSignOut = { screen.value = AppScreen.Login },
                    content = {
                        PrOverviewScreen(Modifier.fillMaxSize())
                    },
                )

            AppScreen.CodeReview ->
                MainShell(
                    screen = AppScreen.CodeReview,
                    onNavigate = { screen.value = it },
                    onSignOut = { screen.value = AppScreen.Login },
                    content = {
                        CodeReviewScreen(Modifier.fillMaxSize())
                    },
                )

            AppScreen.DesignSystem ->
                MainShell(
                    screen = AppScreen.DesignSystem,
                    onNavigate = { screen.value = it },
                    onSignOut = { screen.value = AppScreen.Login },
                    content = {
                        DesignSystemScreen(
                            onBack = { screen.value = AppScreen.PrOverview },
                            modifier = Modifier.fillMaxSize(),
                        )
                    },
                )
        }
    }
}
