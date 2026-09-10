package com.jisuanyusuiji.toolbox.tools.random

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jisuanyusuiji.toolbox.data.JsonStore
import com.jisuanyusuiji.toolbox.ui.Sfx
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.launch
import kotlin.random.Random

private data class WheelItem(val name: String, val color: Long, val weight: Int)

private val PALETTE = listOf(
    0xFFE53935, 0xFFFB8C00, 0xFFFDD835, 0xFF43A047,
    0xFF1E88E5, 0xFF8E24AA, 0xFF00897B, 0xFF6D4C41
)

private fun defaultWheelItems() = listOf(
    WheelItem("一等奖", 0xFFE53935, 1),
    WheelItem("二等奖", 0xFFFB8C00, 2),
    WheelItem("三等奖", 0xFFFDD835, 3),
    WheelItem("四等奖", 0xFF43A047, 4),
    WheelItem("谢谢参与", 0xFF8E24AA, 5),
    WheelItem("再来一次", 0xFF1E88E5, 2)
)

private fun itemsToArray(items: List<WheelItem>): JSONArray {
    val arr = JSONArray()
    items.forEach { item ->
        arr.put(
            JSONObject()
                .put("name", item.name)
                .put("color", item.color)
                .put("weight", item.weight)
        )
    }
    return arr
}

private fun arrayToItems(arr: JSONArray): List<WheelItem> =
    (0 until arr.length()).mapNotNull { i ->
        try {
            val o = arr.getJSONObject(i)
            WheelItem(
                name = o.getString("name"),
                color = o.getLong("color"),
                weight = o.getInt("weight").coerceAtLeast(1)
            )
        } catch (_: Exception) {
            null
        }
    }

@Composable
fun LuckyWheelTool() {
    val context = LocalContext.current
    val store = remember { JsonStore(context, "wheel_templates") }
    val scope = rememberCoroutineScope()
    val rotation = remember { Animatable(0f) }

    var items by remember {
        mutableStateOf(arrayToItems(store.getArray("current")).ifEmpty { defaultWheelItems() })
    }
    var removeAfterWin by remember { mutableStateOf(false) }
    var spinning by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }

    var showAdd by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editWeight by remember { mutableStateOf("1") }
    var editColor by remember { mutableStateOf(PALETTE.first()) }

    var showSave by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }
    var showLoad by remember { mutableStateOf(false) }

    fun persistCurrent() {
        store.putArray("current", itemsToArray(items))
    }

    fun updateItems(newItems: List<WheelItem>) {
        items = newItems
        persistCurrent()
    }

    fun spin() {
        if (items.isEmpty() || spinning) return
        Sfx.tick()
        val total = items.sumOf { it.weight }
        val r = Random.nextInt(total)
        var chosenIndex = 0
        var acc = 0
        for (i in items.indices) {
            acc += items[i].weight
            if (r < acc) {
                chosenIndex = i
                break
            }
        }
        var before = 0f
        for (i in 0 until chosenIndex) before += items[i].weight
        val centerFrac = (before + items[chosenIndex].weight / 2f) / total
        val desired = (-centerFrac * 360f) % 360f
        val current = ((rotation.value % 360f) + 360f) % 360f
        val delta = ((desired - current) % 360f + 360f) % 360f
        val target = rotation.value + 360f * 5f + delta

        scope.launch {
            spinning = true
            result = null
            rotation.animateTo(target, tween(durationMillis = 4200, easing = FastOutSlowInEasing))
            if (chosenIndex in items.indices) {
                result = items[chosenIndex].name
                if (removeAfterWin && items.size > 1) {
                    updateItems(items.filterIndexed { index, _ -> index != chosenIndex })
                }
            }
            spinning = false
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "🎡 幸运转盘") {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(280.dp)) {
                    val total = items.sumOf { it.weight }.toFloat()
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val radius = size.minDimension / 2f - 4.dp.toPx()
                    val paint = Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 15.sp.toPx()
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    var cumulative = 0f
                    items.forEach { item ->
                        val sweep = item.weight / total * 360f
                        val start = rotation.value - 90f + cumulative / total * 360f
                        drawArc(
                            color = Color(item.color.toInt()),
                            startAngle = start,
                            sweepAngle = sweep,
                            useCenter = true,
                            topLeft = Offset(cx - radius, cy - radius),
                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                        )
                        var angle = start + sweep / 2f
                        var flip = false
                        if (angle > 90f && angle < 270f) {
                            angle += 180f
                            flip = true
                        }
                        drawIntoCanvas { canvas ->
                            canvas.save()
                            canvas.translate(cx, cy)
                            canvas.rotate(angle)
                            val tx = if (flip) -radius * 0.58f else radius * 0.58f
                            val ty = -(paint.descent() + paint.ascent()) / 2f
                            canvas.nativeCanvas.drawText(item.name, tx, ty, paint)
                            canvas.restore()
                        }
                        cumulative += item.weight
                    }
                    // 外圈与中心装饰
                    drawCircle(Color.White, radius = radius, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
                    drawCircle(Color(0xFFFFFFFF), radius = 26.dp.toPx(), center = Offset(cx, cy))
                    drawCircle(Color(0xFF37474F), radius = 22.dp.toPx(), center = Offset(cx, cy))
                    // 顶部指针
                    val pointer = Path().apply {
                        moveTo(cx - 14.dp.toPx(), 2.dp.toPx())
                        lineTo(cx + 14.dp.toPx(), 2.dp.toPx())
                        lineTo(cx, 34.dp.toPx())
                        close()
                    }
                    drawPath(pointer, Color(0xFFD32F2F))
                }
            }

            result?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    "抽中：$it",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { spin() },
                enabled = items.isNotEmpty() && !spinning,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (spinning) "转动中…" else "🎡 开始转动", style = MaterialTheme.typography.titleMedium) }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("抽中后移除", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "被抽中的选项将从转盘上删除",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = removeAfterWin, onCheckedChange = { removeAfterWin = it })
            }
        }

        SectionCard(title = "条目（点击颜色可修改，权重越大越容易被抽中）") {
            items.forEachIndexed { index, item ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(18.dp)
                            .background(Color(item.color.toInt()), CircleShape)
                    )
                    Spacer(Modifier.size(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.bodyLarge)
                        Text("权重 ${item.weight}", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = {
                        updateItems(items.filterIndexed { i, _ -> i != index })
                    }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                }
            }
            if (items.isEmpty()) {
                Text("暂无条目，请添加", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = {
                editName = ""
                editWeight = "1"
                editColor = PALETTE.first()
                showAdd = true
            }, modifier = Modifier.fillMaxWidth()) { Text("＋ 添加条目") }
        }

        SectionCard(title = "模板") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showSave = true }, modifier = Modifier.weight(1f)) {
                    Text("💾 保存模板")
                }
                OutlinedButton(onClick = { showLoad = true }, modifier = Modifier.weight(1f)) {
                    Text("📂 载入模板")
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("添加转盘条目") },
            text = {
                Column {
                    LabeledField(editName, { editName = it }, "条目文字")
                    Spacer(Modifier.height(8.dp))
                    LabeledField(editWeight, { editWeight = it }, "权重（整数，默认 1）", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PALETTE.forEach { color ->
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .background(Color(color.toInt()), CircleShape)
                                    .clickable { editColor = color }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = editName.trim()
                    val weight = editWeight.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    if (name.isNotEmpty()) {
                        updateItems(items + WheelItem(name, editColor, weight))
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
            title = { Text("保存转盘模板") },
            text = {
                LabeledField(saveName, { saveName = it }, "模板名称")
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = saveName.trim()
                    if (name.isNotEmpty()) {
                        store.putArray(name, itemsToArray(items))
                        saveName = ""
                        showSave = false
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showSave = false }) { Text("取消") } }
        )
    }

    if (showLoad) {
        val keys = store.keys().filter { it != "current" }
        AlertDialog(
            onDismissRequest = { showLoad = false },
            title = { Text("载入转盘模板") },
            text = {
                Column {
                    if (keys.isEmpty()) {
                        Text("还没有保存过模板", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        keys.forEach { key ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(key, modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    val loaded = arrayToItems(store.getArray(key))
                                    if (loaded.isNotEmpty()) updateItems(loaded)
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
