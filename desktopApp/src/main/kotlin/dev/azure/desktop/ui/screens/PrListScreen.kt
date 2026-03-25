package dev.azure.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import dev.azure.desktop.domain.pr.PullRequestSummary
import dev.azure.desktop.pr.list.PrListAction
import dev.azure.desktop.pr.list.PrListState
import dev.azure.desktop.pr.list.PrListStateMachine
import dev.azure.desktop.theme.EditorialColors
import kotlinx.coroutines.launch

@Composable
fun PrListScreen(
    stateMachine: PrListStateMachine,
    onOpenPullRequest: (PullRequestSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<PrListState>(PrListState.Loading) }
    LaunchedEffect(stateMachine) {
        stateMachine.state.collect { state = it }
    }

    Surface(modifier.fillMaxSize(), color = EditorialColors.surfaceContainerLow) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text("Pull requests", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            when (val current = state) {
                PrListState.Loading -> {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 32.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is PrListState.Error -> {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(current.message, color = EditorialColors.error)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                scope.launch { stateMachine.dispatch(PrListAction.Refresh) }
                            },
                        ) {
                            Text("Retry")
                        }
                    }
                }

                is PrListState.Content -> {
                    Surface(
                        color = EditorialColors.surfaceContainerLowest,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        LazyColumn(Modifier.fillMaxWidth()) {
                            items(current.items, key = { it.id }) { item ->
                                PullRequestRow(item = item, onClick = { onOpenPullRequest(item) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PullRequestRow(item: PullRequestSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = EditorialColors.surface),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${item.creatorDisplayName} requests merge into ${item.targetRefName.substringAfterLast("/")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = EditorialColors.onSurfaceVariant,
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = EditorialColors.primaryFixed,
            ) {
                Text(
                    item.status.uppercase(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = EditorialColors.onPrimaryFixed,
                )
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = EditorialColors.primaryContainer),
            ) {
                Text("Open")
            }
        }
    }
}
