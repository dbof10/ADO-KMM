package dev.azure.desktop

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.azure.desktop.deeplink.AppDeepLinkBus
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.desktop.OpenURIHandler
import javax.swing.SwingUtilities
import kotlin.math.roundToInt

fun main(args: Array<String>) = application {
    LaunchedEffect(Unit) {
        args.forEach { arg ->
            if (arg.contains("dev.azure.com", ignoreCase = true)) {
                AppDeepLinkBus.emit(arg)
            }
        }
    }
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
            if (Desktop.isDesktopSupported()) {
                val desktop = Desktop.getDesktop()
                if (desktop.isSupported(Desktop.Action.APP_OPEN_URI)) {
                    val handler =
                        OpenURIHandler { event ->
                            SwingUtilities.invokeLater {
                                AppDeepLinkBus.emit(event.uri.toString())
                            }
                        }
                    desktop.setOpenURIHandler(handler)
                    onDispose { desktop.setOpenURIHandler(null) }
                } else {
                    onDispose { }
                }
            } else {
                onDispose { }
            }
        }
        DisposableEffect(Unit) {
            val minWidthPx = with(density) { screenSize.width.roundToPx() }
            val minHeightPx = with(density) { screenSize.height.roundToPx() }
            window.minimumSize = Dimension(minWidthPx, minHeightPx)
            onDispose { }
        }
        App()
    }
}
