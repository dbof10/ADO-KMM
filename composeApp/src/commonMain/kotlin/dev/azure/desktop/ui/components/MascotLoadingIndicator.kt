package dev.azure.desktop.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.azure.desktop.theme.EditorialColors

private val MascotBody = EditorialColors.primary
private val MascotEye = Color(0xFFFFFFFF)

/**
 * A pixel-art mascot loading indicator that bobs up and down and walks in place.
 *
 * @param size The size of the bounding box. Defaults to 80.dp for full-screen use.
 */
@Composable
fun MascotLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mascot")

    // Bobbing up/down
    val bobFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bob",
    )

    // Leg swing phase
    val legPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "legs",
    )

    Canvas(modifier = modifier.size(size)) {
        val unit = minOf(this.size.width, this.size.height) / 9f
        val bobOffset = -bobFraction * unit * 1.2f

        // Character grid: 7 units wide, 7 units tall (5 body + 2 legs)
        val left = (this.size.width - 7f * unit) / 2f
        val top = (this.size.height - 7f * unit) / 2f + bobOffset

        // Left arm
        drawRect(MascotBody, topLeft = Offset(left, top + unit), size = Size(unit, 2f * unit))
        // Body
        drawRect(MascotBody, topLeft = Offset(left + unit, top), size = Size(5f * unit, 5f * unit))
        // Right arm
        drawRect(MascotBody, topLeft = Offset(left + 6f * unit, top + unit), size = Size(unit, 2f * unit))
        // Left eye
        drawRect(MascotEye, topLeft = Offset(left + 2f * unit, top + unit), size = Size(unit, unit))
        // Right eye
        drawRect(MascotEye, topLeft = Offset(left + 4f * unit, top + unit), size = Size(unit, unit))
        // Left leg (extends further when legPhase is high)
        val leftExtra = legPhase * unit * 0.4f
        drawRect(
            MascotBody,
            topLeft = Offset(left + 2f * unit, top + 5f * unit),
            size = Size(unit, 2f * unit + leftExtra),
        )
        // Right leg (opposite phase)
        val rightExtra = (1f - legPhase) * unit * 0.4f
        drawRect(
            MascotBody,
            topLeft = Offset(left + 4f * unit, top + 5f * unit),
            size = Size(unit, 2f * unit + rightExtra),
        )
    }
}

/** Full-screen centered mascot loading indicator. */
@Composable
fun MascotLoadingScreen(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        MascotLoadingIndicator()
    }
}
