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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jisuanyusuiji.toolbox.ui.Sfx
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private data class SlotSymbol(val text: String, val color: Long, val weight: Int)

private val SLOT_COLORS = listOf(
    0xFFE53935, 0xFFFB8C00, 0xFFFDD835, 0xFF43A047,
    0xFF1E88E5, 0xFF8E24AA, 0xFF00897B, 0xFF5E35B1
)

private fun defaultSymbols() = listOf(
    SlotSymbol("🍒", SLOT_COLORS[0], 1),
    SlotSymbol("7️⃣", SLOT_COLORS[1], 1),
    SlotSymbol("⭐", SLOT_COLORS[2], 1),
    SlotSymbol("🔔", SLOT_COLORS[3], 1),
    SlotSymbol("🍋", SLOT_COLORS[4], 1),
    SlotSymbol("💎", SLOT_COLORS[5], 1)
)

@Composable
fun SlotMachineTool() {
    val scope = rememberCoroutineScope()
    var symbols by remember { mutableStateOf(defaultSymbols()) }
    var reels by remember { mutableStateOf(listOf(0, 1, 2)) }
    var rolling by remember { mutableStateOf(false) }
    var attempts by remember { mutableStateOf(0) }
    var jackpot by remember { mutableStateOf<SlotSymbol?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("点击“开始转动”，直到三个图案完全相同才会停止") }

    fun weightedPick(): SlotSymbol {
        val total = symbols.sumOf { it.weight }.coerceAtLeast(1)
        var r = Random.nextInt(total)
        for (s in symbols) {
            r -= s.weight
            if (r < 0) return s
        }
        return symbols.last()
    }

    fun start() {
        if (symbols.isEmpty() || rolling) return
        rolling = true
        jackpot = null
        attempts = 0
        status = "转动中…"
        Sfx.tick()
        scope.launch {
            var tries = 0
            while (tries < 200) {
                tries++
                val finals = listOf(weightedPick(), weightedPick(), weightedPick())
                // 三个转轮独立滚动
                for (reel in 0..2) {
                    launch {
                        val duration = Random.nextLong(220, 420)
                        val start = System.currentTimeMillis()
                        while (System.currentTimeMillis() - start < duration) {
                            val idx = symbols.indexOf(weightedPick()).coerceAtLeast(0)
                            reels = reels.toMutableList().also { it[reel] = idx }
                            delay(45)
                        }
                        val finalIndex = symbols.indexOf(finals[reel]).coerceAtLeast(0)
                        reels = reels.toMutableList().also { it[reel] = finalIndex }
                    }
                }
                delay(Random.nextLong(320, 460))
                attempts = tries
                if (finals[0].text == finals[1].text && finals[1].text == finals[2].text) {
                    jackpot = finals[0]
                    status = "🎉 三个相同！本次结果：${finals[0].text}"
                    rolling = false
                    return@launch
                } else {
                    status = "第 $tries 次未中，继续转动…"
                }
            }
            status = "已达到 200 次上限，请调整符号或权重后再试"
            rolling = false
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "🎰 老虎机（三个相同才停）") {
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    reels.forEach { index ->
                        val symbol = symbols.getOrNull(index)
                        Box(
                            Modifier
                                .size(86.dp)
                                .background(
                                    Color((symbol?.color ?: 0xFF9E9E9E).toInt()),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                symbol?.text ?: "?",
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(status, style = MaterialTheme.typography.bodyMedium)
            if (attempts > 0) {
                Text("已转动：$attempts 次", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { showEditor = true }, enabled = !rolling, modifier = Modifier.weight(1f)) {
                    Text("✏️ 编辑符号")
                }
                Button(onClick = { start() }, enabled = !rolling && symbols.isNotEmpty(), modifier = Modifier.weight(1f)) {
                    Text(if (rolling) "转动中…" else "🎰 开始转动")
                }
            }
        }

        jackpot?.let { s ->
            SectionCard(title = "最终结果") {
                Text(
                    s.text,
                    fontSize = 64.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    "共转动 $attempts 次后三个全部相同",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        SectionCard(title = "符号样本空间（可编辑）") {
            symbols.forEach { s ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(s.text, fontSize = 22.sp, modifier = Modifier.width(46.dp))
                    Text("权重 ${s.weight}", modifier = Modifier.weight(1f))
                    Text("占比 ${"%.1f".format(s.weight * 100f / symbols.sumOf { it.weight }.coerceAtLeast(1))}%")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "编辑方式与轮盘相同：可以修改名称、权重、颜色，也可以增删符号。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showEditor) {
        SlotEditorDialog(
            symbols = symbols,
            onChange = { symbols = it },
            onDismiss = { showEditor = false }
        )
    }
}

@Composable
private fun SlotEditorDialog(
    symbols: List<SlotSymbol>,
    onChange: (List<SlotSymbol>) -> Unit,
    onDismiss: () -> Unit
) {
    val totalWeight = symbols.sumOf { it.weight }.coerceAtLeast(1)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑老虎机符号（点击色块换色）") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                symbols.forEachIndexed { index, symbol ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = symbol.text,
                                onValueChange = { newText ->
                                    val list = symbols.toMutableList()
                                    list[index] = symbol.copy(text = newText)
                                    onChange(list)
                                },
                                label = { Text("符号/文字") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = symbol.weight.toString(),
                                onValueChange = { t ->
                                    val w = t.filter { it.isDigit() }.toIntOrNull() ?: 1
                                    val list = symbols.toMutableList()
                                    list[index] = symbol.copy(weight = w.coerceAtLeast(1))
                                    onChange(list)
                                },
                                label = { Text("权重") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(82.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .background(Color(symbol.color.toInt()), CircleShape)
                                    .clickable {
                                        val ci = SLOT_COLORS.indexOf(symbol.color).let { if (it < 0) 0 else it }
                                        val list = symbols.toMutableList()
                                        list[index] = symbol.copy(color = SLOT_COLORS[(ci + 1) % SLOT_COLORS.size])
                                        onChange(list)
                                    }
                            )
                        }
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "占比：${"%.1f".format(symbol.weight * 100f / totalWeight)}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { onChange(symbols.filterIndexed { i, _ -> i != index }) }) {
                                Text("删除", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { onChange(symbols + SlotSymbol("新符号", SLOT_COLORS[symbols.size % SLOT_COLORS.size], 1)) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("＋ 添加符号") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } }
    )
}
