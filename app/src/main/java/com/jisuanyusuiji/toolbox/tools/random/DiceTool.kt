package com.jisuanyusuiji.toolbox.tools.random

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jisuanyusuiji.toolbox.data.JsonStore
import com.jisuanyusuiji.toolbox.ui.Sfx
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.ResultText
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

private data class Die(
    val id: Long,
    val faces: Int,
    val value: Int,
    val rolling: Boolean,
    val colorIndex: Int
)

private data class DiceRecord(
    val time: String,
    val label: String,
    val rolls: List<Int>,
    val total: Int
)

private val DIE_COLORS = listOf(
    0xFFE53935, 0xFF1E88E5, 0xFF43A047, 0xFF8E24AA,
    0xFFFB8C00, 0xFF00897B, 0xFF5E35B1, 0xFFD81B60
)

private val PRESET_FACES = listOf(4, 6, 8, 10, 12, 20, 100)

private fun loadDiceHistory(store: JsonStore): List<DiceRecord> {
    val arr = store.getArray("history")
    return (0 until arr.length()).mapNotNull { i ->
        try {
            val o = arr.getJSONObject(i)
            val rollsArr = o.getJSONArray("rolls")
            val rolls = (0 until rollsArr.length()).map { j -> rollsArr.getInt(j) }
            DiceRecord(o.getString("time"), o.getString("label"), rolls, o.getInt("total"))
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
    val scope = rememberCoroutineScope()

    var dice by remember { mutableStateOf(listOf<Die>()) }
    var nextId by remember { mutableStateOf(1L) }
    var history by remember { mutableStateOf(loadDiceHistory(store)) }
    var rolling by remember { mutableStateOf(false) }
    var showCustom by remember { mutableStateOf(false) }
    var customFaces by remember { mutableStateOf("") }
    var customError by remember { mutableStateOf("") }

    fun addDie(faces: Int) {
        dice = dice + Die(
            id = nextId,
            faces = faces,
            value = Random.nextInt(1, faces + 1),
            rolling = false,
            colorIndex = (nextId % DIE_COLORS.size).toInt()
        )
        nextId++
    }

    fun roll() {
        if (dice.isEmpty() || rolling) return
        Sfx.tick()
        rolling = true
        val snapshot = dice
        val finals = snapshot.associate { it.id to Random.nextInt(1, it.faces + 1) }
        scope.launch {
            coroutineScope {
                snapshot.map { die ->
                    launch {
                        val duration = Random.nextLong(500, 1100)
                        val start = System.currentTimeMillis()
                        while (System.currentTimeMillis() - start < duration) {
                            dice = dice.map {
                                if (it.id == die.id) it.copy(
                                    value = Random.nextInt(1, die.faces + 1),
                                    rolling = true
                                ) else it
                            }
                            delay(55)
                        }
                        dice = dice.map {
                            if (it.id == die.id) it.copy(value = finals[die.id] ?: 1, rolling = false) else it
                        }
                    }
                }.joinAll()
            }
            rolling = false
            val rolls = snapshot.map { finals[it.id] ?: 1 }
            val label = snapshot.groupingBy { it.faces }.eachCount()
                .entries.sortedBy { it.key }
                .joinToString(" + ") { "${it.value}个${it.key}面" }.ifBlank { "自定义骰子" }
            val record = DiceRecord(
                time = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                label = label,
                rolls = rolls,
                total = rolls.sum()
            )
            history = listOf(record) + history
            saveDiceHistory(store, history)
        }
    }

    fun addCustom() {
        val n = customFaces.toIntOrNull()
        if (n == null || n < 2 || n > 100000) {
            customError = "请输入 2 ~ 100000 之间的整数面数"
            return
        }
        addDie(n)
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
        SectionCard(title = "🎲 添加骰子") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PRESET_FACES.forEach { faces ->
                    OutlinedButton(onClick = { addDie(faces) }) { Text("${faces}面") }
                }
                OutlinedButton(onClick = { showCustom = true }) { Text("自定义面数…") }
            }
            if (dice.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { dice = emptyList() }) { Text("清空所有骰子") }
            }
        }

        SectionCard(title = "骰子（每个独立滚动）") {
            if (dice.isEmpty()) {
                Text("还没有骰子，点击上方按钮添加", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                dice.chunked(4).forEach { rowDice ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowDice.forEach { die ->
                            DieView(die = die, onRemove = {
                                dice = dice.filterNot { it.id == die.id }
                            })
                        }
                    }
                }
            }
        }

        Button(
            onClick = { roll() },
            enabled = dice.isNotEmpty() && !rolling,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (rolling) "🎲 滚动中…" else "🎲 掷骰", style = MaterialTheme.typography.titleMedium)
        }

        val total = dice.filterNot { it.rolling }.sumOf { it.value }
        if (dice.isNotEmpty() && !rolling) {
            SectionCard(title = "本次结果") {
                Text(
                    "点数：${dice.joinToString(", ") { it.value.toString() }}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    "总和：$total",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        SectionCard(title = "🕘 掷骰历史（本地保存，最多 30 条）") {
            if (history.isEmpty()) {
                Text("暂无历史", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                history.take(8).forEach { record ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(record.label, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${record.rolls.joinToString(", ")} = ${record.total}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(record.time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
                Spacer(Modifier.height(4.dp))
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
            title = { Text("自定义面数骰子") },
            text = {
                Column {
                    LabeledField(
                        value = customFaces,
                        onChange = { customFaces = it; customError = "" },
                        label = "骰子面数（例如 3、30、144）",
                        keyboardType = KeyboardType.Number
                    )
                    ErrorText(customError)
                }
            },
            confirmButton = { TextButton(onClick = { addCustom() }) { Text("添加") } },
            dismissButton = { TextButton(onClick = { showCustom = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun DieView(die: Die, onRemove: () -> Unit) {
    val wobble = remember { Animatable(0f) }
    val color = Color(DIE_COLORS[die.colorIndex % DIE_COLORS.size].toInt())

    LaunchedEffect(die.rolling) {
        if (die.rolling) {
            while (true) {
                wobble.animateTo(if (wobble.value >= 0f) -16f else 16f, tween(70))
            }
        } else {
            wobble.animateTo(0f, tween(180))
        }
    }

    Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(62.dp)
                .graphicsLayer {
                    rotationZ = wobble.value
                    rotationX = wobble.value * 0.7f
                    cameraDistance = 16f * density
                }
                .background(color, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (die.faces <= 6) {
                DicePips(value = die.value, faces = die.faces, modifier = Modifier.size(50.dp))
            } else {
                Text(
                    "${die.value}",
                    color = Color.White,
                    fontSize = when {
                        die.value >= 100 -> 18.sp
                        die.value >= 10 -> 24.sp
                        else -> 28.sp
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            "✕",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clickable { onRemove() }
                .padding(4.dp)
        )
        Text(
            "${die.faces}面",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun DicePips(value: Int, faces: Int, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension * 0.085f
        fun point(x: Float, y: Float) = Offset(size.width * x, size.height * y)
        val layouts: Map<Int, List<Pair<Float, Float>>> = when (faces) {
            4 -> mapOf(
                1 to listOf(0.5f to 0.5f),
                2 to listOf(0.28f to 0.28f, 0.72f to 0.72f),
                3 to listOf(0.25f to 0.25f, 0.5f to 0.5f, 0.75f to 0.75f),
                4 to listOf(0.27f to 0.27f, 0.73f to 0.27f, 0.27f to 0.73f, 0.73f to 0.73f)
            )
            else -> mapOf(
                1 to listOf(0.5f to 0.5f),
                2 to listOf(0.28f to 0.28f, 0.72f to 0.72f),
                3 to listOf(0.26f to 0.26f, 0.5f to 0.5f, 0.74f to 0.74f),
                4 to listOf(0.28f to 0.28f, 0.72f to 0.28f, 0.28f to 0.72f, 0.72f to 0.72f),
                5 to listOf(0.28f to 0.28f, 0.72f to 0.28f, 0.5f to 0.5f, 0.28f to 0.72f, 0.72f to 0.72f),
                6 to listOf(0.28f to 0.24f, 0.72f to 0.24f, 0.28f to 0.5f, 0.72f to 0.5f, 0.28f to 0.76f, 0.72f to 0.76f)
            )
        }
        layouts[value.coerceIn(1, 6)]?.forEach { (x, y) ->
            drawCircle(Color.White, radius = r, center = point(x, y))
        }
    }
}
