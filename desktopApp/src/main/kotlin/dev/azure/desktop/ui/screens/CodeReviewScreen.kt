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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.azure.desktop.theme.EditorialColors

@Composable
fun CodeReviewScreen(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxSize()) {
        FileExplorerPane(Modifier.width(260.dp).fillMaxHeight())
        Column(Modifier.weight(1f).fillMaxHeight()) {
            DiffToolbar()
            DiffBody(Modifier.weight(1f))
            DiffFooter()
        }
    }
}

@Composable
private fun FileExplorerPane(modifier: Modifier) {
    Column(
        modifier
            .background(EditorialColors.surfaceContainerLow),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("EXPLORER", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Outlined.MoreHoriz, null, tint = EditorialColors.outline, modifier = Modifier.size(18.dp))
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FileRow(Icons.Outlined.FolderOpen, "src/components", indent = 0, selected = false, dotColor = null)
            FileRow(Icons.Outlined.Description, "DiffViewer.tsx", indent = 1, selected = true, dotColor = EditorialColors.tertiaryContainer)
            FileRow(Icons.Outlined.Description, "StatusBadge.tsx", indent = 1, selected = false, dotColor = EditorialColors.primary)
            FileRow(Icons.Outlined.Description, "LegacyLoader.js", indent = 1, selected = false, dotColor = EditorialColors.error, struck = true)
            FileRow(Icons.Outlined.Folder, "styles", indent = 0, selected = false, dotColor = null)
            FileRow(Icons.Outlined.Description, "globals.css", indent = 1, selected = false, dotColor = null)
        }
        Surface(color = EditorialColors.surfaceContainerHigh) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(EditorialColors.primaryFixed),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("JS", color = EditorialColors.onPrimaryFixed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("PR #1204", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text("Architectural Ledger Refactor", fontSize = 10.sp, color = EditorialColors.outline, fontStyle = FontStyle.Italic)
                }
            }
        }
    }
}

@Composable
private fun FileRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    indent: Int,
    selected: Boolean,
    dotColor: androidx.compose.ui.graphics.Color?,
    struck: Boolean = false,
) {
    val bg = if (selected) EditorialColors.surfaceContainerHighest else EditorialColors.surfaceContainerLow
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = (8 + indent * 12).dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, tint = if (selected) EditorialColors.primary else EditorialColors.outline, modifier = Modifier.size(20.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            modifier = Modifier.weight(1f),
            textDecoration = if (struck) TextDecoration.LineThrough else TextDecoration.None,
        )
        if (dotColor != null) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor))
        }
    }
}

@Composable
private fun DiffToolbar() {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(EditorialColors.surface)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("src/components /", color = EditorialColors.outline, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text("DiffViewer.tsx", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Surface(shape = RoundedCornerShape(999.dp), color = EditorialColors.tertiaryFixed) {
                Text(
                    "MODIFIED",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = EditorialColors.onTertiaryFixed,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(10.dp), color = EditorialColors.surfaceContainerHigh) {
                Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = { },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EditorialColors.surfaceContainerLowest),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                    ) {
                        Text("Side-by-side", fontSize = 11.sp)
                    }
                    TextButton(onClick = { }) { Text("Inline", fontSize = 11.sp, color = EditorialColors.outline) }
                }
            }
            Icon(Icons.Outlined.Settings, null, tint = EditorialColors.outline)
        }
    }
}

@Composable
private fun DiffBody(modifier: Modifier) {
    val scroll = rememberScrollState()
    Column(
        modifier
            .fillMaxSize()
            .background(EditorialColors.surfaceContainerLowest)
            .verticalScroll(scroll)
            .padding(bottom = 8.dp),
    ) {
        DiffLine("142", EditorialColors.onSurfaceVariant, "    const [isOpen, setIsOpen] = useState(false);", null, null)
        DiffLine("143", EditorialColors.onErrorContainer, " -  const toggle = () => setIsOpen(!isOpen);", EditorialColors.errorContainer.copy(alpha = 0.2f), EditorialColors.error)
        DiffLine("143", EditorialColors.onPrimaryFixedVariant, " +  const toggle = useCallback(() => setIsOpen(prev => !prev), []);", EditorialColors.primaryFixed.copy(alpha = 0.2f), EditorialColors.primary)
        DiffLine("144", EditorialColors.onSurfaceVariant, "", null, null)
        CommentThreadCard()
        DiffLine("145", EditorialColors.onSurfaceVariant, "    useEffect(() => {", null, null)
        DiffLine("146", EditorialColors.onSurfaceVariant, "        if (isOpen) {", null, null)
        DiffLine("147", EditorialColors.onSurfaceVariant, "            document.body.style.overflow = 'hidden';", null, null)
        DiffLine("148", EditorialColors.onSurfaceVariant, "        }", null, null)
        DiffLine("149", EditorialColors.onSurfaceVariant, "    }, [isOpen]);", null, null)
    }
}

@Composable
private fun DiffLine(
    num: String,
    textColor: androidx.compose.ui.graphics.Color,
    body: String,
    rowBg: androidx.compose.ui.graphics.Color?,
    accent: androidx.compose.ui.graphics.Color?,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(rowBg ?: EditorialColors.surfaceContainerLowest),
    ) {
        if (accent != null) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(accent))
        }
        Text(
            num,
            modifier = Modifier.width(48.dp).padding(vertical = 4.dp),
            fontSize = 12.sp,
            color = EditorialColors.outline,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            body,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp),
            fontSize = 13.sp,
            color = textColor,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun CommentThreadCard() {
    Column(Modifier.padding(start = 40.dp, end = 24.dp, top = 20.dp, bottom = 20.dp)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 3.dp,
            color = EditorialColors.surfaceContainerLowest,
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(EditorialColors.surfaceContainerHigh))
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sarah Chen", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Surface(shape = RoundedCornerShape(999.dp), color = EditorialColors.primaryFixed) {
                                Text(
                                    "ACTIVE",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EditorialColors.onPrimaryFixed,
                                )
                            }
                        }
                        Text("2h ago", fontSize = 11.sp, color = EditorialColors.outline)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Should we consider adding a dependency array to this `useCallback`? Also, let's ensure the toggle state is synchronized with the parent component's ledger state.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Column(Modifier.padding(start = 40.dp)) {
                    val reply = remember { mutableStateOf("") }
                    BasicTextField(
                        value = reply.value,
                        onValueChange = { reply.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(EditorialColors.surfaceContainerHigh, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                            .height(72.dp),
                        decorationBox = { innerTextField ->
                            Box {
                                if (reply.value.isEmpty()) {
                                    Text("Reply or use @ to mention someone...", fontSize = 12.sp, color = EditorialColors.outline)
                                }
                                innerTextField()
                            }
                        },
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { }) { Text("Resolve", fontSize = 12.sp, color = EditorialColors.outline) }
                        Button(
                            onClick = { },
                            colors = ButtonDefaults.buttonColors(containerColor = EditorialColors.primary),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("Reply", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiffFooter() {
    Row(
        Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(EditorialColors.surfaceContainerLow)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(16.dp), tint = EditorialColors.outline)
                Text("Lint: Passed", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EditorialColors.outline)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Sync, null, modifier = Modifier.size(16.dp), tint = EditorialColors.outline)
                Text("Branch: feature/refactor-ledger", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EditorialColors.outline)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("UTF-8", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EditorialColors.outline)
            Text("TypeScript JSX", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EditorialColors.outline)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(EditorialColors.primary))
                Text("+42 -12", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EditorialColors.outline)
            }
        }
    }
}
