package dev.azure.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.azure.desktop.data.auth.JvmAuthServices
import dev.azure.desktop.login.LoginStateMachine
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
        val hasStoredPat = remember { JvmAuthServices.patStorage.loadPat() != null }
        val screen = remember {
            mutableStateOf(if (hasStoredPat) AppScreen.PrOverview else AppScreen.Login)
        }
        var loginMachineEpoch by remember { mutableStateOf(0) }
        val loginStateMachine = remember(loginMachineEpoch) {
            LoginStateMachine(JvmAuthServices.verifyAndStorePat)
        }
        val signOut: () -> Unit = {
            JvmAuthServices.patStorage.clearPat()
            loginMachineEpoch++
            screen.value = AppScreen.Login
        }

        when (screen.value) {
            AppScreen.Login ->
                LoginScreen(
                    stateMachine = loginStateMachine,
                    onLoggedIn = { screen.value = AppScreen.PrOverview },
                )

            AppScreen.PrOverview ->
                MainShell(
                    screen = AppScreen.PrOverview,
                    onNavigate = { screen.value = it },
                    onSignOut = signOut,
                    content = {
                        PrOverviewScreen(Modifier.fillMaxSize())
                    },
                )

            AppScreen.CodeReview ->
                MainShell(
                    screen = AppScreen.CodeReview,
                    onNavigate = { screen.value = it },
                    onSignOut = signOut,
                    content = {
                        CodeReviewScreen(Modifier.fillMaxSize())
                    },
                )

            AppScreen.DesignSystem ->
                MainShell(
                    screen = AppScreen.DesignSystem,
                    onNavigate = { screen.value = it },
                    onSignOut = signOut,
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
