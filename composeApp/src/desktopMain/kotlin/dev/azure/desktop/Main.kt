package dev.azure.desktop

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.Dimension
import java.awt.Toolkit
import kotlin.math.roundToInt

fun main() = application {
    val density = LocalDensity.current
    val screenSize = remember(density) {
        val sizePx = Toolkit.getDefaultToolkit().screenSize
        with(density) {
            DpSize(
                width = (sizePx.width * 0.75f).roundToInt().toDp(),
                height = (sizePx.height * 0.75f).roundToInt().toDp(),
            )
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "ADO Desktop",
        state = WindowState(
            size = screenSize,
            position = WindowPosition.Aligned(Alignment.Center),
        ),
    ) {
        DisposableEffect(Unit) {
            val minWidthPx = with(density) { screenSize.width.roundToPx() }
            val minHeightPx = with(density) { screenSize.height.roundToPx() }
            window.minimumSize = Dimension(minWidthPx, minHeightPx)
            onDispose { }
        }
        App()
    }
}
