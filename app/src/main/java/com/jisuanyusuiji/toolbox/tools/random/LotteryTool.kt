package com.jisuanyusuiji.toolbox.tools.random

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class Lot(val text: String, val kind: String)

private val LOT_KINDS = listOf("吉", "凶", "空")

private fun kindColor(kind: String): Color = when (kind) {
    "吉" -> Color(0xFF2E7D32)
    "凶" -> Color(0xFFC62828)
    else -> Color(0xFF757575)
}

private fun defaultLots() = listOf(
    Lot("大吉大利，万事顺遂", "吉"),
    Lot("心想事成，马到成功", "吉"),
    Lot("诸事平平，静待时机", "空"),
    Lot("宜守不宜攻，三思而行", "凶"),
    Lot("贵人相助，柳暗花明", "吉"),
    Lot("一切随缘，顺其自然", "空"),
    Lot("近期或有波折，谨慎行事", "凶"),
    Lot("时来运转，渐入佳境", "吉")
)

private fun lotsToArray(lots: List<Lot>): JSONArray {
    val arr = JSONArray()
    lots.forEach { lot ->
        arr.put(JSONObject().put("text", lot.text).put("kind", lot.kind))
    }
    return arr
}

private fun arrayToLots(arr: JSONArray): List<Lot> =
    (0 until arr.length()).mapNotNull { i ->
        try {
            val o = arr.getJSONObject(i)
            val kind = o.getString("kind")
            Lot(o.getString("text"), if (kind in LOT_KINDS) kind else "空")
        } catch (_: Exception) {
            null
        }
    }

@Composable
fun LotteryTool() {
    val context = LocalContext.current
    val store = remember { JsonStore(context, "lottery") }
    val scope = rememberCoroutineScope()
    val shake = remember { Animatable(0f) }

    var lots by remember {
        mutableStateOf(arrayToLots(store.getArray("current")).ifEmpty { defaultLots() })
    }
    var history by remember { mutableStateOf(loadHistory(store)) }
    var result by remember { mutableStateOf<Lot?>(null) }
    var drawing by remember { mutableStateOf(false) }

    var showAdd by remember { mutableStateOf(false) }
    var addText by remember { mutableStateOf("") }
    var addKind by remember { mutableStateOf("吉") }
    var showImport by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var showSave by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }
    var showLoad by remember { mutableStateOf(false) }

    fun persist() {
        store.putArray("current", lotsToArray(lots))
    }

    fun updateLots(newLots: List<Lot>) {
        lots = newLots
        persist()
    }

    fun now(): String = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

    fun saveHistory(items: List<String>) {
        val arr = JSONArray()
        items.forEach { arr.put(it) }
        store.putArray("history", arr)
    }

    fun draw() {
        if (lots.isEmpty() || drawing) return
        Sfx.tick()
        scope.launch {
            drawing = true
            result = null
            repeat(5) {
                shake.animateTo(-14f, tween(60))
                shake.animateTo(14f, tween(60))
            }
            shake.animateTo(0f, tween(60))
            val lot = lots.random()
            result = lot
            history = listOf("${now()} · 抽中【${lot.kind}】${lot.text}") + history
            saveHistory(history.take(100))
            drawing = false
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "🏮 摇签") {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .graphicsLayer { translationX = shake.value }
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏮", fontSize = 72.sp)
                        Text(
                            "共 ${lots.size} 支签",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            result?.let { lot ->
                Spacer(Modifier.height(6.dp))
                Text(
                    "【${lot.kind}】",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = kindColor(lot.kind),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Text(
                    lot.text,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { draw() },
                enabled = lots.isNotEmpty() && !drawing,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (drawing) "摇签中…" else "🪭 摇签", style = MaterialTheme.typography.titleMedium) }
        }

        SectionCard(title = "签库管理") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showAdd = true }, modifier = Modifier.weight(1f)) {
                    Text("＋ 添加签")
                }
                OutlinedButton(onClick = { showImport = true }, modifier = Modifier.weight(1f)) {
                    Text("📥 批量导入")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showSave = true }, modifier = Modifier.weight(1f)) {
                    Text("💾 保存签库")
                }
                OutlinedButton(onClick = { showLoad = true }, modifier = Modifier.weight(1f)) {
                    Text("📂 载入签库")
                }
            }
            Spacer(Modifier.height(8.dp))
            lots.forEachIndexed { index, lot ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "【${lot.kind}】",
                        color = kindColor(lot.kind),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.padding(start = 4.dp))
                    Text(lot.text, modifier = Modifier.weight(1f), maxLines = 1)
                    TextButton(onClick = {
                        updateLots(lots.filterIndexed { i, _ -> i != index })
                    }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                }
            }
        }

        SectionCard(title = "摇签记录（本地保存）") {
            if (history.isEmpty()) {
                Text("暂无记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(history.take(10).joinToString("\n"), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = {
                    history = emptyList()
                    saveHistory(history)
                }) { Text("清空记录") }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("添加签文") },
            text = {
                Column {
                    LabeledField(addText, { addText = it }, "签文内容")
                    Spacer(Modifier.height(10.dp))
                    ChoiceChips(LOT_KINDS, addKind, { addKind = it }, { it })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val text = addText.trim()
                    if (text.isNotEmpty()) {
                        updateLots(lots + Lot(text, addKind))
                        addText = ""
                        showAdd = false
                    }
                }) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("取消") } }
        )
    }

    if (showImport) {
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text("批量导入签文") },
            text = {
                Column {
                    LabeledField(
                        value = importText,
                        onChange = { importText = it },
                        label = "每行一条；格式：签文,类型（吉/凶/空）",
                        singleLine = false,
                        minLines = 5
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newLots = importText.lines().mapNotNull { line ->
                        val parts = line.split(",", "，").map { it.trim() }
                        if (parts.isEmpty() || parts[0].isEmpty()) null
                        else Lot(parts[0], parts.getOrNull(1)?.takeIf { it in LOT_KINDS } ?: "空")
                    }
                    if (newLots.isNotEmpty()) {
                        updateLots(lots + newLots)
                        importText = ""
                        showImport = false
                    }
                }) { Text("导入") }
            },
            dismissButton = { TextButton(onClick = { showImport = false }) { Text("取消") } }
        )
    }

    if (showSave) {
        AlertDialog(
            onDismissRequest = { showSave = false },
            title = { Text("保存签库") },
            text = { LabeledField(saveName, { saveName = it }, "签库名称") },
            confirmButton = {
                TextButton(onClick = {
                    val name = saveName.trim()
                    if (name.isNotEmpty()) {
                        store.putArray(name, lotsToArray(lots))
                        saveName = ""
                        showSave = false
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showSave = false }) { Text("取消") } }
        )
    }

    if (showLoad) {
        val keys = store.keys().filter { it != "current" && it != "history" }
        AlertDialog(
            onDismissRequest = { showLoad = false },
            title = { Text("载入签库") },
            text = {
                Column {
                    if (keys.isEmpty()) {
                        Text("还没有保存过签库", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        keys.forEach { key ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(key, modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    val loaded = arrayToLots(store.getArray(key))
                                    if (loaded.isNotEmpty()) updateLots(loaded)
                                    showLoad = false
                                }) { Text("载入") }
                                TextButton(onClick = { store.remove(key) }) {
                                    Text("删除", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLoad = false }) { Text("关闭") } }
        )
    }
}

private fun loadHistory(store: JsonStore): List<String> {
    val arr = store.getArray("history")
    return (0 until arr.length()).mapNotNull { i ->
        try {
            arr.getString(i)
        } catch (_: Exception) {
            null
        }
    }
}
