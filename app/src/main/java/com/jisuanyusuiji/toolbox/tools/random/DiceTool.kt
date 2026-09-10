package com.jisuanyusuiji.toolbox.tools.random

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jisuanyusuiji.toolbox.data.JsonStore
import com.jisuanyusuiji.toolbox.ui.Sfx
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.ResultText
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

private data class DiceRecord(
    val time: String,
    val label: String,
    val rolls: List<Int>,
    val total: Int
)

private fun loadDiceHistory(store: JsonStore): List<DiceRecord> {
    val arr = store.getArray("history")
    return (0 until arr.length()).mapNotNull { i ->
        try {
            val o = arr.getJSONObject(i)
            val rollsArr = o.getJSONArray("rolls")
            val rolls = (0 until rollsArr.length()).map { j -> rollsArr.getInt(j) }
            DiceRecord(
                time = o.getString("time"),
                label = o.getString("label"),
                rolls = rolls,
                total = o.getInt("total")
            )
        } catch (_: Exception) {
            null
        }
    }
}

private fun saveDiceHistory(store: JsonStore, history: List<DiceRecord>) {
    val arr = JSONArray()
    history.take(30).forEach { record ->
        val rollsArr = JSONArray()
        record.rolls.forEach { rollsArr.put(it) }
        arr.put(
            JSONObject()
                .put("time", record.time)
                .put("label", record.label)
                .put("rolls", rollsArr)
                .put("total", record.total)
        )
    }
    store.putArray("history", arr)
}

@Composable
fun DiceTool() {
    val context = LocalContext.current
    val store = remember { JsonStore(context, "dice") }

    var pool by remember { mutableStateOf(listOf<Int>()) }
    var last by remember { mutableStateOf<DiceRecord?>(null) }
    var history by remember { mutableStateOf(loadDiceHistory(store)) }
    var showCustom by remember { mutableStateOf(false) }
    var customFaces by remember { mutableStateOf("") }
    var customError by remember { mutableStateOf("") }

    val presetFaces = listOf(4, 6, 8, 10, 12, 20, 100)

    fun now(): String =
        SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

    fun rollNormal() {
        if (pool.isEmpty()) return
        Sfx.tick()
        val rolls = pool.map { Random.nextInt(1, it + 1) }
        val label = pool.groupingBy { it }.eachCount()
            .entries.sortedBy { it.key }
            .joinToString(" + ") { "${it.value}d${it.key}" }
        val record = DiceRecord(now(), label, rolls, rolls.sum())
        last = record
        history = listOf(record) + history
        saveDiceHistory(store, history)
    }

    fun rollAdvantage(advantage: Boolean) {
        Sfx.tick()
        val rolls = listOf(Random.nextInt(1, 21), Random.nextInt(1, 21))
        val record = DiceRecord(
            time = now(),
            label = if (advantage) "优势 2D20 取高" else "劣势 2D20 取低",
            rolls = rolls,
            total = if (advantage) maxOf(rolls[0], rolls[1]) else minOf(rolls[0], rolls[1])
        )
        last = record
        history = listOf(record) + history
        saveDiceHistory(store, history)
    }

    fun addCustom() {
        val n = customFaces.toIntOrNull()
        if (n == null || n < 2 || n > 100000) {
            customError = "请输入 2 ~ 100000 之间的整数面数"
            return
        }
        pool = pool + n
        customFaces = ""
        customError = ""
        showCustom = false
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "🎲 骰子池（点击加入）") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetFaces.forEach { faces ->
                    OutlinedButton(onClick = { pool = pool + faces }) { Text("D$faces") }
                }
                OutlinedButton(onClick = { showCustom = true }) { Text("自定义…") }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                if (pool.isEmpty()) "尚未添加骰子，请点击上方按钮添加"
                else "当前骰子：" + pool.groupingBy { it }.eachCount()
                    .entries.sortedBy { it.key }
                    .joinToString("  ") { "D${it.key}×${it.value}" },
                style = MaterialTheme.typography.bodyLarge
            )
            if (pool.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = { pool = emptyList() }) { Text("清空骰子池") }
            }
        }

        Button(
            onClick = { rollNormal() },
            enabled = pool.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("🎲 掷骰", style = MaterialTheme.typography.titleMedium) }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { rollAdvantage(true) },
                modifier = Modifier.weight(1f)
            ) { Text("🔼 优势 2D20") }
            OutlinedButton(
                onClick = { rollAdvantage(false) },
                modifier = Modifier.weight(1f)
            ) { Text("🔽 劣势 2D20") }
        }

        last?.let { record ->
            SectionCard(title = "本次结果 · ${record.label}") {
                ResultText(record.rolls.joinToString(", ") + "\n")
                Text(
                    "总和：${record.total}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    record.time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SectionCard(title = "🕘 掷骰历史（本地保存，最多 30 条）") {
            if (history.isEmpty()) {
                Text("暂无历史", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                history.take(8).forEach { record ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(record.label, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${record.rolls.joinToString(", ")} = ${record.total}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            record.time,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 10.sp
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = {
                    history = emptyList()
                    saveDiceHistory(store, history)
                }) { Text("清空历史") }
            }
        }
    }

    if (showCustom) {
        AlertDialog(
            onDismissRequest = { showCustom = false },
            title = { Text("自定义骰子") },
            text = {
                Column {
                    LabeledField(
                        value = customFaces,
                        onChange = { customFaces = it; customError = "" },
                        label = "面数（例如 3、30、144）",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                    ErrorText(customError)
                }
            },
            confirmButton = { TextButton(onClick = { addCustom() }) { Text("加入骰子池") } },
            dismissButton = { TextButton(onClick = { showCustom = false }) { Text("取消") } }
        )
    }
}
