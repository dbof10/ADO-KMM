package dev.azure.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoginErrorView(
    message: String,
    modifier: Modifier = Modifier,
) {
    val rendered = RenderedLoginError.from(message)
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            rendered.summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        rendered.html?.let { html ->
            Spacer(Modifier.height(8.dp))
            SelectionContainer {
                Text(
                    text = html,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(10.dp),
                            )
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                )
            }
        }
    }
}

private data class RenderedLoginError(
    val summary: String,
    val html: String?,
) {
    companion object {
        fun from(message: String): RenderedLoginError {
            val htmlStart = message.indexOf("<!DOCTYPE", ignoreCase = true).takeIf { it >= 0 }
                ?: message.indexOf("<html", ignoreCase = true).takeIf { it >= 0 }
            if (htmlStart == null) {
                return RenderedLoginError(summary = message, html = null)
            }
            val summary = message.substring(0, htmlStart).trim().ifEmpty { "Azure DevOps returned an HTML error page." }
            val html = message.substring(htmlStart).trim()
            return RenderedLoginError(summary = summary, html = html)
        }
    }
}
