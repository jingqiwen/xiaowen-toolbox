package com.jisuanyusuiji.toolbox.tools.random

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.data.JsonStore
import com.jisuanyusuiji.toolbox.ui.Sfx
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.ResultText
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

private data class GachaCard(
    val name: String,
    val weight: Int,
    val color: Long,
    val rarity: String
)

private val RARITY_COLORS = listOf(
    0xFF9E9E9E, 0xFF1E88E5, 0xFF8E24AA, 0xFFFDD835, 0xFFE53935
)

private val RARITY_NAMES = listOf("N", "R", "SR", "SSR", "UR")

private fun defaultPool() = listOf(
    GachaCard("普通卡牌A", 50, 0xFF9E9E9E, "N"),
    GachaCard("普通卡牌B", 30, 0xFF9E9E9E, "N"),
    GachaCard("稀有卡牌C", 12, 0xFF1E88E5, "R"),
    GachaCard("稀有卡牌D", 6, 0xFF8E24AA, "SR"),
    GachaCard("传说卡牌E", 2, 0xFFFDD835, "SSR")
)

private fun poolToArray(pool: List<GachaCard>): JSONArray {
    val arr = JSONArray()
    pool.forEach { card ->
        arr.put(
            JSONObject()
                .put("name", card.name)
                .put("weight", card.weight)
                .put("color", card.color)
                .put("rarity", card.rarity)
        )
    }
    return arr
}

private fun arrayToPool(arr: JSONArray): List<GachaCard> =
    (0 until arr.length()).mapNotNull { i ->
        try {
            val o = arr.getJSONObject(i)
            GachaCard(
                name = o.getString("name"),
                weight = o.getInt("weight").coerceAtLeast(1),
                color = o.getLong("color"),
                rarity = o.getString("rarity")
            )
        } catch (_: Exception) {
            null
        }
    }

@Composable
fun GachaTool() {
    val context = LocalContext.current
    val store = remember { JsonStore(context, "gacha") }

    var pool by remember {
        mutableStateOf(arrayToPool(store.getArray("current")).ifEmpty { defaultPool() })
    }
    var results by remember { mutableStateOf(listOf<GachaCard>()) }
    var stats by remember { mutableStateOf(loadStats(store)) }
    var showAdd by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editWeight by remember { mutableStateOf("10") }
    var editRarity by remember { mutableStateOf("R") }
    var showSave by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }
    var showLoad by remember { mutableStateOf(false) }

    fun persist() {
        store.putArray("current", poolToArray(pool))
    }

    fun updatePool(newPool: List<GachaCard>) {
        pool = newPool
        persist()
    }

    fun saveStats() {
        val obj = JSONObject()
        stats.forEach { (name, count) -> obj.put(name, count) }
        store.putObject("stats", obj)
    }

    fun weightedPick(): GachaCard? {
        if (pool.isEmpty()) return null
        val total = pool.sumOf { it.weight }
        val r = Random.nextInt(total)
        var acc = 0
        for (card in pool) {
            acc += card.weight
            if (r < acc) return card
        }
        return pool.last()
    }

    fun draw(times: Int) {
        if (pool.isEmpty()) return
        Sfx.tick()
        val drawn = List(times) { weightedPick()!! }
        results = drawn
        val newStats = stats.toMutableMap()
        drawn.forEach { card ->
            newStats[card.name] = (newStats[card.name] ?: 0) + 1
        }
        stats = newStats
        saveStats()
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "🃏 卡池（权重越大，抽中概率越高）") {
            pool.forEachIndexed { index, card ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(18.dp)
                            .background(Color(card.color.toInt()), CircleShape)
                    )
                    Spacer(Modifier.size(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("【${card.rarity}】${card.name}", style = MaterialTheme.typography.bodyLarge)
                        Text("权重 ${card.weight}", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = {
                        updatePool(pool.filterIndexed { i, _ -> i != index })
                    }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                }
            }
            if (pool.isEmpty()) Text("卡池为空，请添加卡牌")
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = {
                editName = ""
                editWeight = "10"
                editRarity = "R"
                showAdd = true
            }, modifier = Modifier.fillMaxWidth()) { Text("＋ 添加卡牌") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { draw(1) },
                enabled = pool.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) { Text("抽 1 次") }
            OutlinedButton(
                onClick = { draw(10) },
                enabled = pool.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) { Text("十连抽") }
        }

        if (results.isNotEmpty()) {
            SectionCard(title = "本次结果") {
                val grouped = results.groupingBy { it.name }.eachCount()
                ResultText(
                    grouped.entries.joinToString("\n") { (name, count) ->
                        val card = pool.firstOrNull { it.name == name }
                        "【${card?.rarity ?: "?"}】$name × $count"
                    }
                )
            }
        }

        SectionCard(title = "📊 抽卡统计（本地保存）") {
            if (stats.isEmpty()) {
                Text("暂无统计", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val totalDraws = stats.values.sum()
                Text("累计抽取：$totalDraws 次", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                stats.toList().sortedByDescending { it.second }.forEach { (name, count) ->
                    val card = pool.firstOrNull { it.name == name }
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(
                            "【${card?.rarity ?: "?"}】$name",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "$count 次",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = {
                    stats = emptyMap()
                    saveStats()
                }) { Text("清空统计") }
            }
        }

        SectionCard(title = "卡池模板") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showSave = true }, modifier = Modifier.weight(1f)) {
                    Text("💾 保存卡池")
                }
                OutlinedButton(onClick = { showLoad = true }, modifier = Modifier.weight(1f)) {
                    Text("📂 载入卡池")
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("添加卡牌") },
            text = {
                Column {
                    LabeledField(editName, { editName = it }, "卡牌名称")
                    Spacer(Modifier.height(8.dp))
                    LabeledField(editWeight, { editWeight = it }, "概率权重（整数）", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(12.dp))
                    Text("稀有度", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RARITY_NAMES.forEachIndexed { index, rarity ->
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .background(
                                        Color(RARITY_COLORS[index].toInt()),
                                        if (editRarity == rarity) CircleShape
                                        else androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                    )
                                    .clickable { editRarity = rarity },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(rarity, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = editName.trim()
                    val weight = editWeight.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    if (name.isNotEmpty()) {
                        val color = RARITY_COLORS[RARITY_NAMES.indexOf(editRarity).coerceAtLeast(0)]
                        updatePool(pool + GachaCard(name, weight, color, editRarity))
                        showAdd = false
                    }
                }) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("取消") } }
        )
    }

    if (showSave) {
        AlertDialog(
            onDismissRequest = { showSave = false },
            title = { Text("保存卡池配置") },
            text = { LabeledField(saveName, { saveName = it }, "卡池名称") },
            confirmButton = {
                TextButton(onClick = {
                    val name = saveName.trim()
                    if (name.isNotEmpty()) {
                        store.putArray(name, poolToArray(pool))
                        saveName = ""
                        showSave = false
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showSave = false }) { Text("取消") } }
        )
    }

    if (showLoad) {
        val keys = store.keys().filter { it != "current" && it != "stats" }
        AlertDialog(
            onDismissRequest = { showLoad = false },
            title = { Text("载入卡池配置") },
            text = {
                Column {
                    if (keys.isEmpty()) {
                        Text("还没有保存过卡池", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        keys.forEach { key ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(key, modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    val loaded = arrayToPool(store.getArray(key))
                                    if (loaded.isNotEmpty()) updatePool(loaded)
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

private fun loadStats(store: JsonStore): Map<String, Int> {
    val obj = store.getObject("stats")
    val result = mutableMapOf<String, Int>()
    obj.keys().forEach { key ->
        try {
            result[key] = obj.getInt(key)
        } catch (_: Exception) {
        }
    }
    return result
}
