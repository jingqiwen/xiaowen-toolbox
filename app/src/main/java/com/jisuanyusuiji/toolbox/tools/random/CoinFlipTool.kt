package com.jisuanyusuiji.toolbox.tools.random

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jisuanyusuiji.toolbox.data.JsonStore
import com.jisuanyusuiji.toolbox.ui.Sfx
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import com.jisuanyusuiji.toolbox.ui.components.Stepper
import kotlin.math.cos
import kotlin.random.Random

@Composable
fun CoinFlipTool() {
    val context = LocalContext.current
    val store = remember { JsonStore(context, "coin") }

    var heads by remember { mutableStateOf(store.getString("heads")?.toIntOrNull() ?: 0) }
    var tails by remember { mutableStateOf(store.getString("tails")?.toIntOrNull() ?: 0) }
    var flipCount by remember { mutableStateOf(1) }
    var rotation by remember { mutableStateOf(0f) }

    val animatedRotation by animateFloatAsState(
        targetValue = rotation,
        animationSpec = tween(durationMillis = 900),
        label = "coinRotation"
    )

    // 根据当前旋转角度判断显示哪一面
    val showHeads = cos(Math.toRadians(animatedRotation.toDouble())) >= 0.0

    fun saveStats() {
        store.putString("heads", heads.toString())
        store.putString("tails", tails.toString())
    }

    fun flip() {
        Sfx.tick()
        val results = List(flipCount) { Random.nextBoolean() }
        val newHeads = results.count { it }
        heads += newHeads
        tails += results.size - newHeads
        saveStats()
        rotation += 360f * 3f + if (flipCount % 2 == 1) 180f else 0f
    }

    fun reset() {
        heads = 0
        tails = 0
        saveStats()
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .size(190.dp)
                .graphicsLayer {
                    rotationY = animatedRotation
                    cameraDistance = 18f * density
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .clip(CircleShape)
                    .background(if (showHeads) Color(0xFFF2C14E) else Color(0xFFB0BEC5)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (showHeads) "正" else "反",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4E342E)
                )
            }
        }
        Text(
            if (showHeads) "正面" else "反面",
            style = MaterialTheme.typography.titleLarge
        )

        SectionCard {
            Stepper("连续抛掷数量", flipCount, { flipCount = it }, 1..100)
            Spacer(Modifier.height(10.dp))
            Button(onClick = { flip() }, modifier = Modifier.fillMaxWidth()) {
                Text("🪙 抛硬币", style = MaterialTheme.typography.titleMedium)
            }
        }

        SectionCard(title = "统计") {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("正面", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "$heads",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("反面", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "$tails",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("总计", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${heads + tails}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (heads + tails > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "正面比例：${(heads * 100f / (heads + tails)).let { String.format("%.1f", it) }}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = { reset() }, modifier = Modifier.fillMaxWidth()) {
                Text("重置统计")
            }
        }
    }
}
