package dev.azure.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azure.desktop.login.LoginMachineAction
import dev.azure.desktop.login.LoginMachineState
import dev.azure.desktop.login.LoginStateMachine
import dev.azure.desktop.theme.EditorialColors
import dev.azure.desktop.ui.components.LoginErrorView
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    stateMachine: LoginStateMachine,
    onLoggedIn: () -> Unit,
    initialOrganization: String = "",
) {
    val organization = remember(initialOrganization) { mutableStateOf(initialOrganization) }
    val pat = remember { mutableStateOf("") }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    var machineState by remember { mutableStateOf<LoginMachineState>(LoginMachineState.Idle(null)) }

    LaunchedEffect(stateMachine) {
        var previous: LoginMachineState? = null
        stateMachine.state.collect { state ->
            machineState = state
            if (state is LoginMachineState.Success && previous !is LoginMachineState.Success) {
                onLoggedIn()
            }
            previous = state
        }
    }

    val idleError = (machineState as? LoginMachineState.Idle)?.error
    val isWorking = machineState is LoginMachineState.Working

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorialColors.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
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
            Column(
                modifier = Modifier
                    .weight(7f)
                    .fillMaxHeight()
                    .background(EditorialColors.surfaceContainerLowest)
                    .verticalScroll(scroll)
                    .padding(horizontal = 40.dp, vertical = 48.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Connect to Azure DevOps",
                    style = MaterialTheme.typography.headlineSmall,
                    color = EditorialColors.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Securely authorize your session using a Personal Access Token.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EditorialColors.onSurfaceVariant,
                )
                Spacer(Modifier.height(32.dp))
                Text("Organization", style = MaterialTheme.typography.titleSmall, color = EditorialColors.onSurface)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = organization.value,
                    onValueChange = { organization.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. contoso") },
                    keyboardOptions = KeyboardOptions.Default,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = EditorialColors.surfaceContainerHighest,
                        unfocusedContainerColor = EditorialColors.surfaceContainerHighest,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                )
                Spacer(Modifier.height(16.dp))
                Text("Personal Access Token", style = MaterialTheme.typography.titleSmall, color = EditorialColors.onSurface)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = pat.value,
                    onValueChange = { pat.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Paste your PAT here...") },
                    leadingIcon = { Icon(Icons.Outlined.VpnKey, null, tint = EditorialColors.outline) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions.Default,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = EditorialColors.surfaceContainerHighest,
                        unfocusedContainerColor = EditorialColors.surfaceContainerHighest,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                )
                idleError?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    LoginErrorView(message = message)
                }
                Spacer(Modifier.height(24.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(EditorialColors.surfaceContainer)
                        .padding(20.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Info, null, tint = EditorialColors.primary)
                        Column {
                            Text("How to generate a PAT", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(8.dp))
                            PatStep(1, "Go to User Settings in your ADO portal.")
                            PatStep(2, "Select Personal access tokens.")
                            PatStep(3, "Create a new token with Code (Read & Write) scopes.")
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = {
                        scope.launch {
                            stateMachine.dispatch(
                                LoginMachineAction.SubmitPat(
                                    organization = organization.value,
                                    pat = pat.value,
                                ),
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = !isWorking && organization.value.isNotBlank() && pat.value.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EditorialColors.primary,
                        contentColor = EditorialColors.onPrimary,
                    ),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        if (isWorking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = EditorialColors.onPrimary,
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        Text("Connect to The Ledger", fontWeight = FontWeight.Bold)
                        if (!isWorking) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Outlined.ArrowForward, contentDescription = null)
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Shield,
                            null,
                            tint = EditorialColors.outline.copy(alpha = 0.45f),
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            "ENTERPRISE ENCRYPTION ACTIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = EditorialColors.outline.copy(alpha = 0.45f),
                        )
                    }
                    TextButton(onClick = { }) {
                        Text(
                            "NEED HELP?",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EditorialColors.primary,
                            textDecoration = TextDecoration.Underline,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PatStep(number: Int, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(EditorialColors.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EditorialColors.onSurfaceVariant)
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = EditorialColors.onSurfaceVariant)
    }
}
