package com.jisuanyusuiji.toolbox.tools.random

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jisuanyusuiji.toolbox.data.JsonStore
import com.jisuanyusuiji.toolbox.ui.Sfx
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.random.Random

private data class CoinStyle(
    val name: String,
    val outer: Long,
    val inner: Long,
    val rim: Long,
    val frontText: String,
    val backText: String,
    val backPattern: String
)

private val COIN_STYLES = listOf(
    CoinStyle("人民币 1 元", 0xFFBFC7CF, 0xFFE9EEF3, 0xFF9AA4AE, "1元", "背面", "菊花"),
    CoinStyle("人民币 5 角", 0xFFD4B24A, 0xFFF3DA8B, 0xFFA98A2C, "5角", "背面", "荷花"),
    CoinStyle("人民币 1 角", 0xFFD7DDE3, 0xFFF6F8FA, 0xFFAEB6BD, "1角", "背面", "兰花"),
    CoinStyle("美元 5 美分", 0xFFB6BEC6, 0xFFDCE2E8, 0xFF8E979F, "5¢", "背面", "Liberty"),
    CoinStyle("美元 1 美分", 0xFFB87333, 0xFFD99A5B, 0xFF8C4F1E, "1¢", "背面", "Lincoln")
)

@Composable
private fun CoinView(
    style: CoinStyle,
    front: Boolean,
    spinScale: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension / 2f - 2f
        drawCircle(Color(style.outer.toInt()), radius = r, center = Offset(cx, cy))
        drawCircle(Color(style.inner.toInt()), radius = r * 0.86f, center = Offset(cx, cy))
        drawCircle(
            Color(style.rim.toInt()),
            radius = r * 0.86f,
            center = Offset(cx, cy),
            style = Stroke(width = r * 0.045f)
        )
        // 边缘装饰点
        val dotColor = Color(style.rim.toInt())
        for (i in 0 until 36) {
            val a = i * 10.0 * PI / 180.0
            drawCircle(
                dotColor,
                radius = r * 0.018f,
                center = Offset(
                    cx + (r * 0.93f * cos(a)).toFloat(),
                    cy + (r * 0.93f * kotlin.math.sin(a)).toFloat()
                )
            )
        }
        val paint = Paint().apply {
            color = android.graphics.Color.argb(255, 60, 60, 60)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
        drawIntoCanvas { canvas ->
            if (front) {
                paint.textSize = size.minDimension * 0.28f
                canvas.nativeCanvas.drawText(
                    style.frontText,
                    cx,
                    cy + paint.textSize * 0.36f,
                    paint
                )
                paint.textSize = size.minDimension * 0.075f
                canvas.nativeCanvas.drawText("正 面", cx, cy + size.minDimension * 0.30f, paint)
            } else {
                // 背面图案：不显示“反”字，避免镜像问题
                paint.textSize = size.minDimension * 0.16f
                canvas.nativeCanvas.drawText(style.backPattern, cx, cy + paint.textSize * 0.35f, paint)
                paint.textSize = size.minDimension * 0.07f
                canvas.nativeCanvas.drawText("背面图案", cx, cy + size.minDimension * 0.30f, paint)
            }
        }
    }
}

@Composable
fun CoinFlipTool() {
    val context = LocalContext.current
    val store = remember { JsonStore(context, "coin") }
    var styleIndex by remember { mutableStateOf(0) }
    val style = COIN_STYLES[styleIndex]

    var heads by remember(styleIndex) { mutableStateOf(store.getString("heads_$styleIndex")?.toIntOrNull() ?: 0) }
    var tails by remember(styleIndex) { mutableStateOf(store.getString("tails_$styleIndex")?.toIntOrNull() ?: 0) }
    var countText by remember { mutableStateOf("1") }
    var angle by remember { mutableStateOf(0f) }
    var lastResults by remember { mutableStateOf<List<Boolean>>(emptyList()) }
    var error by remember { mutableStateOf("") }

    val animatedAngle by animateFloatAsState(
        targetValue = angle,
        animationSpec = tween(durationMillis = 1000),
        label = "coinFlip"
    )
    val front = ((animatedAngle / 180f).toInt() % 2 == 0)
    val spinScale = abs(cos(Math.toRadians(animatedAngle.toDouble()))).toFloat().coerceAtLeast(0.08f)

    fun saveStats() {
        store.putString("heads_$styleIndex", heads.toString())
        store.putString("tails_$styleIndex", tails.toString())
    }

    fun flip() {
        error = ""
        val n = countText.trim().toIntOrNull()
        if (n == null || n < 1 || n > 10000) { error = "请输入 1 ~ 10000 的整数次数"; return }
        Sfx.tick()
        val results = List(n) { Random.nextBoolean() }
        val newHeads = results.count { it }
        heads += newHeads
        tails += results.size - newHeads
        lastResults = results
        saveStats()
        angle += 360f * 2f + if (n % 2 == 1) 180f else 0f
    }

    fun reset() {
        heads = 0; tails = 0; lastResults = emptyList(); saveStats()
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionCard(title = "选择硬币") {
            ChoiceChips(
                options = COIN_STYLES.indices.toList(),
                selected = styleIndex,
                onSelect = { styleIndex = it },
                label = { COIN_STYLES[it].name }
            )
        }

        Box(
            modifier = Modifier
                .size(190.dp)
                .graphicsLayer { scaleX = spinScale },
            contentAlignment = Alignment.Center
        ) {
            CoinView(style = style, front = front, spinScale = spinScale, modifier = Modifier.fillMaxSize())
        }

        Text(
            if (front) "正面（面值面）" else "反面（图案面）",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        lastResults.takeIf { it.isNotEmpty() }?.let { results ->
            val h = results.count { it }
            val t = results.size - h
            SectionCard(title = "本次结果") {
                Text(
                    if (h >= t) "本次判定：正面" else "本次判定：反面",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "正面 $h 次 · 反面 $t 次",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    "说明：正面为面值/文字面，反面为图案面；不显示旋转的“反”字。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SectionCard(title = "抛掷设置（可直接输入次数）") {
            LabeledField(
                value = countText,
                onChange = { countText = it },
                label = "连续抛掷次数（1~10000）",
                keyboardType = KeyboardType.Number
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { countText = "1"; flip() }, modifier = Modifier.weight(1f)) {
                    Text("抛 1 次")
                }
                Button(onClick = { flip() }, modifier = Modifier.weight(1f)) {
                    Text("开始抛掷")
                }
            }
            ErrorText(error)
        }

        SectionCard(title = "统计（${style.name}）") {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("正面", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$heads", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("反面", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$tails", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("总计", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${heads + tails}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
            if (heads + tails > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "正面比例：${"%.1f".format(heads * 100f / (heads + tails))}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { reset() }, modifier = Modifier.fillMaxWidth()) { Text("重置当前硬币统计") }
        }
    }
}
