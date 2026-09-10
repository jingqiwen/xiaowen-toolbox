package com.jisuanyusuiji.toolbox.tools.random

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.data.JsonStore
import com.jisuanyusuiji.toolbox.ui.Sfx
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.ResultText
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import com.jisuanyusuiji.toolbox.ui.components.ShareButton
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DEFAULT_NAMES = "张三\n李四\n王五\n赵六\n钱七\n孙八"

@Composable
fun RandomPickerTool() {
    val context = LocalContext.current
    val store = remember { JsonStore(context, "picker_data") }
    val listStore = remember { JsonStore(context, "picker_lists") }

    var namesText by remember { mutableStateOf(store.getString("current") ?: DEFAULT_NAMES) }
    var allowRepeat by remember { mutableStateOf(true) }
    var remaining by remember { mutableStateOf(listOf<String>()) }
    var picked by remember { mutableStateOf("") }
    var history by remember { mutableStateOf(loadHistory(store)) }

    var showSave by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }
    var showLoad by remember { mutableStateOf(false) }

    // 名单变化时重置抽取状态
    LaunchedEffect(namesText) {
        remaining = emptyList()
        picked = ""
    }

    fun parseNames(): List<String> =
        namesText.lines().map { it.trim() }.filter { it.isNotEmpty() }

    fun now(): String = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

    fun saveHistory(items: List<String>) {
        val arr = JSONArray()
        items.forEach { arr.put(it) }
        store.putArray("history", arr)
    }

    fun pick() {
        val names = parseNames()
        if (names.isEmpty()) {
            picked = "⚠️ 名单为空，请先输入名单"
            return
        }
        Sfx.tick()
        val chosen: String
        if (allowRepeat) {
            chosen = names.random()
        } else {
            if (remaining.isEmpty()) remaining = names
            chosen = remaining.random()
            remaining = remaining.filter { it != chosen }
        }
        picked = chosen
        val entry = "${now()} · 抽取：$chosen"
        history = listOf(entry) + history
        saveHistory(history.take(100))
        store.putString("current", namesText)
    }

    fun exportText(): String =
        "随机点名器抽取记录\n导出时间：${now()}\n名单人数：${parseNames().size}\n\n" +
            history.joinToString("\n")

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "📋 名单（每行一个姓名）") {
            LabeledField(
                value = namesText,
                onChange = { namesText = it },
                label = "粘贴或输入名单",
                singleLine = false,
                minLines = 6
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "共 ${parseNames().size} 人",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showSave = true }, modifier = Modifier.weight(1f)) {
                    Text("💾 保存名单")
                }
                OutlinedButton(onClick = { showLoad = true }, modifier = Modifier.weight(1f)) {
                    Text("📂 载入名单")
                }
            }
        }

        SectionCard(title = "抽取模式") {
            ChoiceChips(
                options = listOf(true, false),
                selected = allowRepeat,
                onSelect = { allowRepeat = it },
                label = { if (it) "可重复抽取" else "无重复 · 不重复抽取" }
            )
            if (!allowRepeat) {
                Spacer(Modifier.height(6.dp))
                Text(
                    if (remaining.isEmpty()) "点击抽取后将自动使用完整名单"
                    else "剩余 ${remaining.size} 人（抽完自动停止）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Button(onClick = { pick() }, modifier = Modifier.fillMaxWidth()) {
            Text("🎯 随机抽取", style = MaterialTheme.typography.titleMedium)
        }

        if (picked.isNotBlank()) {
            SectionCard(title = "抽中") {
                Text(
                    picked,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        SectionCard(title = "抽取记录（本地保存，最多 100 条）") {
            if (history.isEmpty()) {
                Text("暂无记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                ResultText(history.take(8).joinToString("\n"))
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShareButton(exportText(), title = "导出抽取记录", modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        history = emptyList()
                        saveHistory(history)
                    }) { Text("清空记录") }
                }
            }
        }
    }

    if (showSave) {
        AlertDialog(
            onDismissRequest = { showSave = false },
            title = { Text("保存当前名单") },
            text = {
                LabeledField(
                    value = saveName,
                    onChange = { saveName = it },
                    label = "名单名称（如：三班学生）"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = saveName.trim()
                    if (name.isNotEmpty()) {
                        listStore.putString(name, namesText)
                        saveName = ""
                        showSave = false
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showSave = false }) { Text("取消") } }
        )
    }

    if (showLoad) {
        val keys = listStore.keys()
        AlertDialog(
            onDismissRequest = { showLoad = false },
            title = { Text("载入已保存名单") },
            text = {
                Column {
                    if (keys.isEmpty()) {
                        Text("还没有保存过名单", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        keys.forEach { key ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(key, modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    listStore.getString(key)?.let { namesText = it }
                                    showLoad = false
                                }) { Text("载入") }
                                TextButton(onClick = {
                                    listStore.remove(key)
                                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
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
