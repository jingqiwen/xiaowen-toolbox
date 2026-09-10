package com.jisuanyusuiji.toolbox.tools.calc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jisuanyusuiji.toolbox.data.JsonStore
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import org.json.JSONArray
import java.math.BigDecimal
import java.math.RoundingMode

private fun formatNumber(v: Double): String {
    if (v.isNaN() || v.isInfinite()) return "错误"
    return try {
        BigDecimal(v).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    } catch (_: Exception) {
        v.toString()
    }
}

// ============================================================
// 10. 基础计算器
// ============================================================
@Composable
fun BasicCalculatorTool() {
    val context = LocalContext.current
    val store = remember { JsonStore(context, "basic_calc") }
    var expression by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }
    var history by remember { mutableStateOf(loadCalcHistory(store)) }

    fun press(key: String) {
        when (key) {
            "C" -> { expression = ""; resultText = "" }
            "⌫" -> expression = expression.dropLast(1)
            "=" -> {
                if (expression.isBlank()) return
                val r = CalcExpr.evaluate(expression)
                if (r.ok && r.value != null) {
                    resultText = formatNumber(r.value)
                    history = listOf("$expression = $resultText") + history
                    saveCalcHistory(store, history.take(50))
                } else {
                    resultText = r.error ?: "错误"
                }
            }
            else -> {
                expression += key
                resultText = ""
            }
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(110.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                Modifier.fillMaxWidth().padding(14.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    expression.ifBlank { "0" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 3
                )
                Text(
                    resultText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    maxLines = 2
                )
            }
        }

        val rows = listOf(
            listOf("C", "⌫", "(", ")"),
            listOf("7", "8", "9", "÷"),
            listOf("4", "5", "6", "×"),
            listOf("1", "2", "3", "-"),
            listOf("0", ".", "%", "+"),
            listOf("=")
        )
        rows.forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    val accent = key == "=" || key == "C" || key in listOf("÷", "×", "-", "+")
                    if (accent) {
                        Button(onClick = { press(key) }, modifier = Modifier.weight(1f)) {
                            Text(key, fontSize = 20.sp)
                        }
                    } else {
                        Button(
                            onClick = { press(key) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) { Text(key, fontSize = 20.sp) }
                    }
                }
            }
        }

        SectionCard(title = "计算历史（本地保存，最多 50 条）") {
            if (history.isEmpty()) {
                Text("暂无历史", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(history.take(10).joinToString("\n"), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = {
                    history = emptyList()
                    saveCalcHistory(store, history)
                }) { Text("清空历史") }
            }
        }
    }
}

// ============================================================
// 11. 科学计算器
// ============================================================
@Composable
fun ScientificCalculatorTool() {
    var expression by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }
    var complexMode by remember { mutableStateOf(false) }

    fun press(key: String) {
        when (key) {
            "C" -> { expression = ""; resultText = "" }
            "⌫" -> expression = expression.dropLast(1)
            "=" -> {
                if (expression.isBlank()) return
                if (complexMode) {
                    val r = ComplexCalc.evaluate(expression)
                    resultText = r.error ?: r.value.toString()
                } else {
                    val r = CalcExpr.evaluate(expression)
                    resultText = if (r.ok && r.value != null) formatNumber(r.value) else (r.error ?: "错误")
                }
            }
            "sin", "cos", "tan", "log", "ln" -> expression += "$key("
            "√" -> expression += "sqrt("
            "x²" -> expression += "^2"
            "xⁿ" -> expression += "^"
            "π" -> expression += "pi"
            "e" -> expression += "e"
            else -> expression += key
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                Modifier.fillMaxWidth().padding(14.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    expression.ifBlank { "0" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 4
                )
                Text(
                    resultText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    maxLines = 2
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("复数模式", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "支持 (1+2i)*(3-4i) 形式的 + - × ÷ 运算",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = complexMode, onCheckedChange = { complexMode = it })
        }

        val rows = listOf(
            listOf("C", "⌫", "(", ")", "÷"),
            listOf("sin", "cos", "tan", "log", "ln"),
            listOf("7", "8", "9", "×", "√"),
            listOf("4", "5", "6", "-", "x²"),
            listOf("1", "2", "3", "+", "xⁿ"),
            listOf("0", ".", "π", "e", "%"),
            listOf("=")
        )
        rows.forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    val function = key in setOf("sin", "cos", "tan", "log", "ln", "√", "x²", "xⁿ")
                    val accent = key == "=" || key == "C"
                    Button(
                        onClick = { press(key) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                accent -> MaterialTheme.colorScheme.primary
                                function -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = when {
                                accent -> MaterialTheme.colorScheme.onPrimary
                                function -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    ) { Text(key, fontSize = if (key.length > 1) 14.sp else 20.sp) }
                }
            }
        }
    }
}

// ============================================================
// 12. 卡西欧风格模拟器计算器
// ============================================================
@Composable
fun CasioCalculatorTool() {
    var expression by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }
    var degrees by remember { mutableStateOf(true) }
    var shift by remember { mutableStateOf(false) }
    var memory by remember { mutableStateOf(0.0) }
    var ans by remember { mutableStateOf(0.0) }
    var error by remember { mutableStateOf("") }

    fun press(key: String) {
        error = ""
        when (key) {
            "AC" -> { expression = ""; resultText = "" }
            "DEL", "⌫" -> expression = expression.dropLast(1)
            "=" -> {
                if (expression.isBlank()) return
                val r = CalcExpr.evaluate(expression, degrees)
                if (r.ok && r.value != null) {
                    ans = r.value
                    resultText = formatNumber(r.value)
                } else {
                    resultText = r.error ?: "Error"
                }
            }
            "SHIFT" -> shift = !shift
            "DEG/RAD" -> degrees = !degrees
            "M+" -> memory += resultText.toDoubleOrNull() ?: ans
            "M-" -> memory -= resultText.toDoubleOrNull() ?: ans
            "MR" -> expression += formatNumber(memory)
            "MC" -> memory = 0.0
            "Ans" -> expression += formatNumber(ans)
            "nCr" -> expression += "ncr("
            "nPr" -> expression += "npr("
            "10^" -> expression += "10^("
            "e^" -> expression += "e^("
            else -> expression += key
        }
    }

    val keyRows = listOf(
        listOf("SHIFT", "ALPHA", "MODE", if (degrees) "DEG" else "RAD", "DEL", "AC"),
        listOf(if (shift) "sin⁻¹" else "sin", if (shift) "cos⁻¹" else "cos", if (shift) "tan⁻¹" else "tan", if (shift) "10^" else "log", if (shift) "e^" else "ln"),
        listOf("x²", "x³", "xⁿ", "√", "∛"),
        listOf("nCr", "nPr", "π", "e", "!"),
        listOf("7", "8", "9", "(", ")"),
        listOf("4", "5", "6", "×", "÷"),
        listOf("1", "2", "3", "+", "-"),
        listOf("0", ".", "Ans", "%", "="),
        listOf("M+", "M-", "MR", "MC", "1/x")
    )

    fun keyAction(key: String) {
        when (key) {
            "SHIFT" -> press("SHIFT")
            "DEG", "RAD" -> press("DEG/RAD")
            "sin", "cos", "tan" -> expression += "$key("
            "sin⁻¹" -> expression += "asin("
            "cos⁻¹" -> expression += "acos("
            "tan⁻¹" -> expression += "atan("
            "log" -> expression += "log("
            "ln" -> expression += "ln("
            "10^" -> expression += "10^("
            "e^" -> expression += "e^("
            "√" -> expression += "sqrt("
            "∛" -> expression += "cbrt("
            "x²" -> expression += "^2"
            "x³" -> expression += "^3"
            "xⁿ" -> expression += "^"
            "1/x" -> expression += "1/("
            "!" -> expression += "!"
            "π" -> expression += "pi"
            else -> press(key)
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 显示屏
        Box(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(Color(0xFFC8D6C0), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "fx-991 STYLE" + if (shift) "  ·  SHIFT" else "",
                        fontSize = 11.sp,
                        color = Color(0xFF33523A)
                    )
                    Text(
                        (if (degrees) "DEG" else "RAD") + if (memory != 0.0) "  M" else "",
                        fontSize = 11.sp,
                        color = Color(0xFF33523A)
                    )
                }
                Text(
                    expression.ifBlank { "0" },
                    fontSize = 21.sp,
                    color = Color(0xFF1B2B1D),
                    textAlign = TextAlign.End,
                    maxLines = 3
                )
                Text(
                    resultText.ifBlank { "" },
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0E1A10),
                    textAlign = TextAlign.End,
                    maxLines = 2
                )
            }
        }
        if (error.isNotBlank()) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        keyRows.forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { key ->
                    val decoration = key in setOf("SHIFT", "ALPHA", "MODE")
                    val container = when {
                        key == "SHIFT" && shift -> Color(0xFFEF6C00)
                        key == "AC" || key == "=" -> Color(0xFF2E7D32)
                        decoration -> Color(0xFF5D4037)
                        key in setOf("DEL", "⌫", "√", "∛", "x²", "x³", "xⁿ", "log", "ln", "10^", "e^", "sin", "cos", "tan", "sin⁻¹", "cos⁻¹", "tan⁻¹") -> Color(0xFF546E7A)
                        else -> Color(0xFF37474F)
                    }
                    Button(
                        onClick = { if (!decoration || key == "SHIFT") keyAction(key) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = Color.White),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text(
                            key,
                            fontSize = if (key.length > 3) 9.sp else if (key.length > 2) 11.sp else 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Text(
            "功能：四则运算、括号、幂、阶乘、百分号、平方/立方、开平方/开立方、sin/cos/tan 与反三角、log/ln、10^x/e^x、排列组合 nCr/nPr、π/e、Ans、记忆 M+/M-/MR/MC、DEG/RAD 切换。\n" +
                "仅外观风格参考 CASIO，与 CASIO 公司无关。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}


// ---------- 历史存储 ----------

private fun loadCalcHistory(store: JsonStore): List<String> {
    val arr = store.getArray("history")
    return (0 until arr.length()).mapNotNull { i ->
        try {
            arr.getString(i)
        } catch (_: Exception) {
            null
        }
    }
}

private fun saveCalcHistory(store: JsonStore, items: List<String>) {
    val arr = JSONArray()
    items.forEach { arr.put(it) }
    store.putArray("history", arr)
}
