package com.jisuanyusuiji.toolbox.tools.random

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jisuanyusuiji.toolbox.data.JsonStore
import com.jisuanyusuiji.toolbox.ui.Sfx
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun levelColor(level: String): Color = when {
    level.startsWith("上上") -> Color(0xFF1B5E20)
    level.startsWith("上") -> Color(0xFF2E7D32)
    level.startsWith("中") -> Color(0xFFF57F17)
    else -> Color(0xFFC62828)
}

@Composable
fun LotteryTool() {
    val context = LocalContext.current
    val store = remember { JsonStore(context, "lottery") }
    val scope = rememberCoroutineScope()
    val shake = remember { Animatable(0f) }
    val allLots = remember { LotteryData.all }

    var result by remember { mutableStateOf<LotteryLot?>(null) }
    var history by remember { mutableStateOf(loadLotteryHistory(store)) }
    var drawing by remember { mutableStateOf(false) }
    var showBrowse by remember { mutableStateOf(false) }

    fun draw() {
        if (drawing) return
        Sfx.tick()
        scope.launch {
            drawing = true
            result = null
            repeat(5) {
                shake.animateTo(-14f, tween(55))
                shake.animateTo(14f, tween(55))
            }
            shake.animateTo(0f, tween(60))
            val lot = allLots.random()
            result = lot
            history = listOf("${nowText()} · ${lot.title} · ${lot.level}") + history
            saveLotteryHistory(store, history.take(100))
            drawing = false
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "🏮 摇签（内置 ${allLots.size} 条签文）") {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .graphicsLayer { translationX = shake.value }
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏮", fontSize = 70.sp)
                        Text(
                            "共 ${allLots.size} 条 · 64 卦 × 6 爻",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { showBrowse = true }, modifier = Modifier.weight(1f)) { Text("📖 浏览签库") }
                Button(
                    onClick = { draw() },
                    enabled = !drawing,
                    modifier = Modifier.weight(1f)
                ) { Text(if (drawing) "摇签中…" else "🪭 摇签") }
            }
        }

        result?.let { lot ->
            SectionCard(title = lot.title) {
                Text(
                    "【${lot.level}】",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = levelColor(lot.level)
                )
                Spacer(Modifier.height(8.dp))
                lot.poem.forEach { line ->
                    Text(
                        line,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(10.dp))
                lot.interpretations.forEach { (category, text) ->
                    Text(
                        "【$category】$text",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "解签内容为本地生成的原创释义，仅供娱乐参考。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        SectionCard(title = "摇签记录") {
            if (history.isEmpty()) {
                Text("暂无记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(history.take(10).joinToString("\n"), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = {
                    history = emptyList()
                    saveLotteryHistory(store, history)
                }) { Text("清空记录") }
            }
        }

        SectionCard(title = "说明") {
            Text(
                "签库参考《易经》64 卦与民间签诗风格，共 384 条，包含综合、事业、财运、感情、健康、出行六类解读。\n" +
                    "所有内容均为程序化生成的原创文字，不代表任何真实预测，请理性看待。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showBrowse) {
        LotteryBrowseDialog(allLots = allLots, onDismiss = { showBrowse = false })
    }
}

@Composable
private fun LotteryBrowseDialog(allLots: List<LotteryLot>, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) allLots
        else allLots.filter {
            it.number.toString().contains(query) ||
                it.title.contains(query, true) ||
                it.level.contains(query, true) ||
                it.poem.any { line -> line.contains(query, true) }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("浏览签库（${filtered.size} 条）") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("搜索编号 / 卦名 / 签文") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 460.dp)) {
                    items(filtered, key = { it.number }) { lot ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Column(Modifier.padding(10.dp)) {
                                Row {
                                    Text(
                                        lot.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        lot.level,
                                        color = levelColor(lot.level),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    lot.poem.joinToString("，"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

private fun nowText(): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date())

private fun loadLotteryHistory(store: JsonStore): List<String> {
    val arr = store.getArray("history")
    return (0 until arr.length()).mapNotNull { i ->
        try { arr.getString(i) } catch (_: Exception) { null }
    }
}

private fun saveLotteryHistory(store: JsonStore, items: List<String>) {
    val arr = JSONArray()
    items.forEach { arr.put(it) }
    store.putArray("history", arr)
}
