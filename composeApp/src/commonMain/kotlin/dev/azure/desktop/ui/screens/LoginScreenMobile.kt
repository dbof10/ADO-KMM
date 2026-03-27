package dev.azure.desktop.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.azure.desktop.theme.EditorialColors

@Composable
internal fun LoginScreenMobile(
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
        LoginFormPane(
            modifier = Modifier
                .fillMaxSize()
                .background(EditorialColors.surfaceContainerLowest)
                .verticalScroll(scroll)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            organization = organization,
            onOrganizationChange = onOrganizationChange,
            pat = pat,
            onPatChange = onPatChange,
            idleError = idleError,
            isWorking = isWorking,
            onSubmit = onSubmit,
            showHelpCard = false,
            includeBrandHeader = true,
            verticalArrangement = Arrangement.Top,
        )
    }
}
