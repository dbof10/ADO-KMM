package dev.azure.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azure.desktop.theme.EditorialColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesignSystemScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    Column(
        modifier
            .fillMaxSize()
            .background(EditorialColors.surfaceContainerLow)
            .verticalScroll(scroll)
            .padding(28.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Azure Meridian", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Design tokens from stitch-gitpro / 01-design-system",
                    style = MaterialTheme.typography.bodySmall,
                    color = EditorialColors.onSurfaceVariant,
                )
            }
            TextButton(onClick = onBack) {
                Text("Back to PR", fontWeight = FontWeight.Bold, color = EditorialColors.primary)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("SURFACE STACK (NO 1PX DIVIDERS)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            Surface(Modifier.weight(1f).height(72.dp), color = EditorialColors.surface) {
                Text("surface", fontSize = 11.sp, modifier = Modifier.padding(12.dp), color = EditorialColors.onSurfaceVariant)
            }
            Surface(Modifier.weight(1f).height(72.dp), color = EditorialColors.surfaceContainerLow) {
                Text("container-low", fontSize = 11.sp, modifier = Modifier.padding(12.dp), color = EditorialColors.onSurfaceVariant)
            }
            Surface(Modifier.weight(1f).height(72.dp), color = EditorialColors.surfaceContainerHighest) {
                Text("container-highest", fontSize = 11.sp, modifier = Modifier.padding(12.dp), color = EditorialColors.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(28.dp))
        Text("CORE COLORS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Swatch("primary", EditorialColors.primary)
            Swatch("primary-container", EditorialColors.primaryContainer)
            Swatch("tertiary", EditorialColors.tertiary)
            Swatch("error", EditorialColors.error)
            Swatch("outline-variant", EditorialColors.outlineVariant)
        }
        Spacer(Modifier.height(28.dp))
        Text("TYPOGRAPHY (INTER / SANS-SERIF)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Display · 28sp Bold", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Headline · 18sp Bold", style = MaterialTheme.typography.headlineSmall)
        Text("Title · 14sp SemiBold", style = MaterialTheme.typography.titleSmall)
        Text("Body · 14sp Regular", style = MaterialTheme.typography.bodyMedium)
        Text("LABEL · 11sp BOLD", style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(28.dp))
        Text("BUTTONS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EditorialColors.primary,
                    contentColor = EditorialColors.onPrimary,
                ),
                shape = RoundedCornerShape(10.dp),
            ) { Text("Primary") }
            OutlinedButton(
                onClick = { },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    1.dp,
                    EditorialColors.outlineVariant.copy(alpha = 0.2f),
                ),
            ) { Text("Secondary", color = EditorialColors.primary) }
            TextButton(onClick = { }) { Text("Ghost", color = EditorialColors.primary, fontWeight = FontWeight.SemiBold) }
        }
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(EditorialColors.primary, EditorialColors.primaryContainer)))
                .clickable { }
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text("Gradient CTA", color = EditorialColors.onPrimary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "“The Architectural Ledger” — tonal surfaces, ghost borders, 200ms ease-out on state changes.",
            style = MaterialTheme.typography.bodySmall,
            color = EditorialColors.onSurfaceVariant,
        )
    }
}

@Composable
private fun Swatch(label: String, color: Color) {
    Column {
        Text(label, fontSize = 10.sp, color = EditorialColors.outline, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Surface(
            modifier = Modifier
                .size(width = 88.dp, height = 40.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, EditorialColors.outlineVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
            color = color,
        ) {
            Box(Modifier.fillMaxSize())
        }
    }
}
