package com.jisuanyusuiji.toolbox.tools.calc

import android.graphics.Color as AndroidColor
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.ui.components.CopyButton
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Base64

// ============================================================
// 30. Base64 编码 / 解码
// ============================================================
@Composable
fun Base64Tool() {
    var input by remember { mutableStateOf("hello，我是温景淇") }
    var output by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var lastAction by remember { mutableStateOf("") }

    fun encode() {
        error = ""
        try {
            output = Base64.getEncoder().encodeToString(input.toByteArray(Charsets.UTF_8))
            lastAction = "编码"
        } catch (e: Exception) {
            error = "编码失败：${e.message}"
        }
    }

    fun decode() {
        error = ""
        try {
            output = String(Base64.getDecoder().decode(input.trim()), Charsets.UTF_8)
            lastAction = "解码"
        } catch (_: Exception) {
            error = "解码失败：输入不是有效的 Base64 文本"
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "本地 Base64 编码 / 解码") {
            LabeledField(input, { input = it }, "输入文本或 Base64", singleLine = false, minLines = 4)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { encode() }, modifier = Modifier.weight(1f)) { Text("编码") }
                OutlinedButton(onClick = { decode() }, modifier = Modifier.weight(1f)) { Text("解码") }
            }
        }
        ErrorText(error)
        if (output.isNotBlank()) {
            SectionCard(title = "结果（${lastAction}）") {
                Text(output, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                CopyButton(output)
            }
        }
    }
}

// ============================================================
// 31. URL 编码 / 解码
// ============================================================
@Composable
fun UrlCodecTool() {
    var input by remember { mutableStateOf("https://example.com/搜索?q=随机&page=1") }
    var output by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var lastAction by remember { mutableStateOf("") }

    fun encode() {
        error = ""
        try {
            output = URLEncoder.encode(input, "UTF-8")
            lastAction = "编码"
        } catch (e: Exception) {
            error = "编码失败：${e.message}"
        }
    }

    fun decode() {
        error = ""
        try {
            output = URLDecoder.decode(input, "UTF-8")
            lastAction = "解码"
        } catch (_: Exception) {
            error = "解码失败：输入格式不正确"
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "本地 URL 编码 / 解码") {
            LabeledField(input, { input = it }, "输入文本或 URL", singleLine = false, minLines = 3)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { encode() }, modifier = Modifier.weight(1f)) { Text("编码") }
                OutlinedButton(onClick = { decode() }, modifier = Modifier.weight(1f)) { Text("解码") }
            }
        }
        ErrorText(error)
        if (output.isNotBlank()) {
            SectionCard(title = "结果（${lastAction}）") {
                Text(output, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                CopyButton(output)
            }
        }
    }
}

// ============================================================
// 47. 颜色拾取器（RGB / HEX 转换）
// ============================================================
@Composable
fun ColorPickerTool() {
    var red by remember { mutableStateOf(63f) }
    var green by remember { mutableStateOf(81f) }
    var blue by remember { mutableStateOf(181f) }
    var hexInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    fun toHex(v: Float): String = v.toInt().coerceIn(0, 255).toString(16).padStart(2, '0').uppercase()
    val hex = "#${toHex(red)}${toHex(green)}${toHex(blue)}"
    val color = Color(AndroidColor.rgb(red.toInt(), green.toInt(), blue.toInt()))

    fun parseHex() {
        error = ""
        val text = hexInput.trim().removePrefix("#")
        if (text.length != 6) { error = "请输入 6 位十六进制颜色，如 3F51B5"; return }
        val r = text.substring(0, 2).toIntOrNull(16)
        val g = text.substring(2, 4).toIntOrNull(16)
        val b = text.substring(4, 6).toIntOrNull(16)
        if (r == null || g == null || b == null) { error = "HEX 格式错误"; return }
        red = r.toFloat(); green = g.toFloat(); blue = b.toFloat()
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "颜色预览") {
            Box(
                Modifier.fillMaxWidth().height(110.dp).background(color, RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.height(10.dp))
            Text(hex, style = MaterialTheme.typography.titleLarge)
            Text(
                "RGB(${red.toInt()}, ${green.toInt()}, ${blue.toInt()})",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(10.dp))
            CopyButton("$hex  RGB(${red.toInt()}, ${green.toInt()}, ${blue.toInt()})")
        }
        SectionCard(title = "RGB 滑块") {
            ColorSlider("R 红", red, { red = it })
            ColorSlider("G 绿", green, { green = it })
            ColorSlider("B 蓝", blue, { blue = it })
        }
        SectionCard(title = "输入 HEX 反向设置 RGB") {
            LabeledField(hexInput, { hexInput = it }, "HEX（如 3F51B5）")
            Spacer(Modifier.height(10.dp))
            Button(onClick = { parseHex() }, modifier = Modifier.fillMaxWidth()) { Text("应用 HEX") }
            ErrorText(error)
        }
    }
}

@Composable
private fun ColorSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column {
        Text("$label：${value.toInt()}")
        Slider(value = value, onValueChange = onChange, valueRange = 0f..255f)
    }
}
