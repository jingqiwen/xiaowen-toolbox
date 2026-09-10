package com.jisuanyusuiji.toolbox.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jisuanyusuiji.toolbox.data.Prefs
import com.jisuanyusuiji.toolbox.data.ToolDef

/** 每个工具页面的通用外壳：返回按钮、标题、收藏星标。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolHost(tool: ToolDef, onBack: () -> Unit) {
    var favorite by remember(tool.id) { mutableStateOf(Prefs.isFavorite(tool.id)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(tool.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            tool.desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("‹ 返回", style = MaterialTheme.typography.titleMedium)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        favorite = Prefs.toggleFavorite(tool.id)
                    }) {
                        Text(
                            if (favorite) "★" else "☆",
                            fontSize = 22.sp,
                            color = if (favorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            tool.screen()
        }
    }
}

@Composable
fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            if (title != null) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(10.dp))
            }
            content()
        }
    }
}

@Composable
fun LabeledField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}

@Composable
fun Stepper(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        FilledTonalButton(
            onClick = { if (value > range.first) onChange(value - 1) },
            modifier = Modifier.size(42.dp),
            contentPadding = PaddingValues(0.dp),
            enabled = value > range.first
        ) { Text("−", fontSize = 18.sp) }
        Text(
            "$value",
            modifier = Modifier.widthIn(min = 44.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        FilledTonalButton(
            onClick = { if (value < range.last) onChange(value + 1) },
            modifier = Modifier.size(42.dp),
            contentPadding = PaddingValues(0.dp),
            enabled = value < range.last
        ) { Text("+", fontSize = 18.sp) }
    }
}

/** 单选按钮组（可横向滚动）。 */
@Composable
fun <T> ChoiceChips(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            if (option == selected) {
                Button(
                    onClick = { onSelect(option) },
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) { Text(label(option)) }
            } else {
                OutlinedButton(
                    onClick = { onSelect(option) },
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) { Text(label(option)) }
            }
        }
    }
}

@Composable
fun CopyButton(text: String, enabled: Boolean = text.isNotBlank(), modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(text) { copied = false }
    OutlinedButton(
        onClick = {
            clipboard.setText(AnnotatedString(text))
            copied = true
        },
        enabled = enabled,
        modifier = modifier
    ) { Text(if (copied) "✅ 已复制" else "📋 复制") }
}

@Composable
fun ShareButton(text: String, title: String = "分享文本", modifier: Modifier = Modifier) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = { shareText(context, text, title) },
        enabled = text.isNotBlank(),
        modifier = modifier
    ) { Text("📤 导出/分享") }
}

fun shareText(context: Context, text: String, title: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, title))
}

@Composable
fun ResultText(text: String, modifier: Modifier = Modifier) {
    androidx.compose.foundation.text.selection.SelectionContainer {
        Text(
            text,
            modifier = modifier,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ErrorText(text: String, modifier: Modifier = Modifier) {
    if (text.isNotBlank()) {
        Text(
            text,
            modifier = modifier,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/** 点击后弹出选项列表的选择框。 */
@Composable
fun SelectorField(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var show by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Surface(
            onClick = { show = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Text(
                value,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    if (show) {
        AlertDialog(
            onDismissRequest = { show = false },
            title = { Text("选择$label") },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(options) { option ->
                        TextButton(
                            onClick = {
                                onSelect(option)
                                show = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(option, modifier = Modifier.fillMaxWidth()) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { show = false }) { Text("关闭") } }
        )
    }
}

/** 一行 “标签：值” 的结果展示。 */
@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(8.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}
