package dev.azure.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.azure.desktop.pr.detail.PrDetailAction
import dev.azure.desktop.pr.detail.PrDetailState
import dev.azure.desktop.pr.detail.PrDetailStateMachine
import dev.azure.desktop.theme.EditorialColors
import kotlinx.coroutines.launch

@Composable
fun PrDetailScreen(
    stateMachine: PrDetailStateMachine,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<PrDetailState>(PrDetailState.Loading) }
    LaunchedEffect(stateMachine) {
        stateMachine.state.collect { state = it }
    }
    Box(
        modifier
            .fillMaxSize()
            .background(EditorialColors.surfaceContainerLow),
    ) {
        when (val current = state) {
            PrDetailState.Loading -> {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }

            is PrDetailState.Error -> {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(current.message, color = EditorialColors.error)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onBack() }) { Text("Back") }
                        Button(onClick = { scope.launch { stateMachine.dispatch(PrDetailAction.Refresh) } }) {
                            Text("Retry")
                        }
                    }
                }
            }

            is PrDetailState.Content -> {
                val detail = current.detail
                val summary = detail.summary
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = onBack) { Text("Back") }
                        Surface(shape = RoundedCornerShape(999.dp), color = EditorialColors.primaryFixed) {
                            Text(
                                summary.status.uppercase(),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = EditorialColors.onPrimaryFixed,
                            )
                        }
                        Text("PR #${summary.id}", color = EditorialColors.outline)
                    }
                    Text(summary.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text(
                        "${summary.creatorDisplayName} wants to merge ${summary.sourceRefName.substringAfterLast("/")} into ${summary.targetRefName.substringAfterLast("/")}",
                        color = EditorialColors.onSurfaceVariant,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Card(
                            modifier = Modifier.weight(2f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(EditorialColors.surfaceContainerLowest),
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("DESCRIPTION", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    detail.description?.takeIf { it.isNotBlank() } ?: "No description was provided.",
                                    color = EditorialColors.onSurfaceVariant,
                                )
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(EditorialColors.surfaceContainerLowest),
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("REVIEWERS", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                detail.reviewers.forEach { reviewer ->
                                    Text(
                                        "• ${reviewer.displayName} (${voteText(reviewer.vote)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = EditorialColors.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun voteText(vote: Int): String =
    when {
        vote >= 10 -> "Approved"
        vote in 1..9 -> "Approved with suggestions"
        vote < 0 -> "Rejected"
        else -> "Waiting"
    }
