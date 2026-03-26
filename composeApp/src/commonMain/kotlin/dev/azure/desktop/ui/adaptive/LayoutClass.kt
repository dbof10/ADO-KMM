package dev.azure.desktop.ui.adaptive

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class LayoutClass {
    Compact,
    Medium,
    Expanded,
}

fun layoutClassForWidth(width: Dp): LayoutClass =
    when {
        width < 700.dp -> LayoutClass.Compact
        width < 1100.dp -> LayoutClass.Medium
        else -> LayoutClass.Expanded
    }
