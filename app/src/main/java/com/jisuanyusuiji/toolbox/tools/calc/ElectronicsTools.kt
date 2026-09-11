package com.jisuanyusuiji.toolbox.tools.calc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.InfoRow
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import com.jisuanyusuiji.toolbox.ui.components.SelectorField
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

private fun f3(v: Double): String = try {
    BigDecimal(v).setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
} catch (_: Exception) { v.toString() }

private fun d(text: String): Double? = text.trim().toDoubleOrNull()

private fun fmtOhm(ohm: Double): String = when {
    ohm >= 1e6 -> "${f3(ohm / 1e6)} MΩ"
    ohm >= 1e3 -> "${f3(ohm / 1e3)} kΩ"
    else -> "${f3(ohm)} Ω"
}

private data class BandColor(val name: String, val digit: Int, val multiplier: Double, val tolerance: Double?)

private val COLOR_BANDS = listOf(
    BandColor("黑", 0, 1.0, null),
    BandColor("棕", 1, 10.0, 1.0),
    BandColor("红", 2, 100.0, 2.0),
    BandColor("橙", 3, 1000.0, null),
    BandColor("黄", 4, 10000.0, null),
    BandColor("绿", 5, 100000.0, 0.5),
    BandColor("蓝", 6, 1000000.0, 0.25),
    BandColor("紫", 7, 10000000.0, 0.1),
    BandColor("灰", 8, 100000000.0, 0.05),
    BandColor("白", 9, 1000000000.0, null),
    BandColor("金", -1, 0.1, 5.0),
    BandColor("银", -1, 0.01, 10.0)
)

private fun band(name: String) = COLOR_BANDS.first { it.name == name }

private val E24 = listOf(10, 11, 12, 13, 15, 16, 18, 20, 22, 24, 27, 30, 33, 36, 39, 43, 47, 51, 56, 62, 68, 75, 82, 91)

/** 输入 "4.7k"、"1M"、"470" 等，返回欧姆。 */
private fun parseOhm(text: String): Double? {
    var s = text.trim().replace("Ω", "").replace("欧", "")
    val mult = when {
        s.endsWith("M", true) -> { s = s.dropLast(1); 1e6 }
        s.endsWith("K", true) -> { s = s.dropLast(1); 1e3 }
        else -> 1.0
    }
    return s.toDoubleOrNull()?.times(mult)
}

/** 反向查色环：找最接近的两位有效数字标准值。 */
private fun reverseBands(ohm: Double): Pair<List<String>, Double>? {
    if (ohm <= 0) return null
    val e = kotlin.math.floor(log10(ohm)).toInt() - 1
    if (e < -2 || e > 9) return null
    val mantissa = ohm / 10.0.pow(e.toDouble())
    val nearest = E24.minByOrNull { abs(it - mantissa) } ?: return null
    val d1 = nearest / 10
    val d2 = nearest % 10
    val digit1 = COLOR_BANDS.first { it.digit == d1 }.name
    val digit2 = COLOR_BANDS.first { it.digit == d2 }.name
    val multiplier = COLOR_BANDS.first { it.multiplier == 10.0.pow(e.toDouble()) }.name
    val actual = nearest * 10.0.pow(e.toDouble())
    return listOf(digit1, digit2, multiplier, "金") to actual
}

/** 六环电阻的第六环：温度系数（ppm/K）。 */
private val TEMPCO_BANDS = listOf("棕" to 100, "红" to 50, "橙" to 15, "黄" to 25, "蓝" to 10, "紫" to 5)

/** 反向查色环（五/六环，三位有效数字）。 */
private fun reverseBands3(ohm: Double): Pair<List<String>, Double>? {
    if (ohm <= 0) return null
    var e = kotlin.math.floor(log10(ohm)).toInt() - 2
    var mantissa = (ohm / 10.0.pow(e)).toLong()
    if ((ohm / 10.0.pow(e)) - mantissa >= 0.5) mantissa += 1
    if (mantissa >= 1000) { mantissa /= 10; e += 1 }
    if (mantissa < 100) return null
    val d1 = (mantissa / 100) % 10
    val d2 = (mantissa / 10) % 10
    val d3 = mantissa % 10
    val multColor = COLOR_BANDS.firstOrNull {
        it.digit >= 0 && abs(it.multiplier - 10.0.pow(e)) < 1e-9
    } ?: return null
    val names = listOf(
        COLOR_BANDS.first { it.digit == d1.toInt() }.name,
        COLOR_BANDS.first { it.digit == d2.toInt() }.name,
        COLOR_BANDS.first { it.digit == d3.toInt() }.name,
        multColor.name
    )
    return names to mantissa * 10.0.pow(e)
}

// ============================================================
// 24. 电阻色环计算器
// ============================================================
@Composable
fun ResistorColorTool() {
    var bandCount by remember { mutableStateOf(4) }
    var b1 by remember { mutableStateOf("棕") }
    var b2 by remember { mutableStateOf("黑") }
    var b3 by remember { mutableStateOf("黑") }
    var mult by remember { mutableStateOf("红") }
    var tol by remember { mutableStateOf("金") }
    var tempco by remember { mutableStateOf("棕") }
    var ohmText by remember { mutableStateOf("4700") }
    var forward by remember { mutableStateOf("") }
    var reverse by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    fun calcForward() {
        error = ""
        forward = ""
        val digitColors = listOf(b1, b2, b3).take(if (bandCount == 4) 2 else 3)
        val digits = digitColors.map { band(it).digit }
        if (digits.any { it < 0 }) { error = "数字环必须是黑~白（不能是金/银）"; return }
        val base = digits.fold(0L) { acc, dgt -> acc * 10 + dgt }
        val ohm = base * band(mult).multiplier
        val tolPercent = band(tol).tolerance ?: 20.0
        val lines = mutableListOf(
            "阻值：${fmtOhm(ohm)}",
            "误差：±${f3(tolPercent)}%",
            "阻值范围：${fmtOhm(ohm * (1 - tolPercent / 100))} ~ ${fmtOhm(ohm * (1 + tolPercent / 100))}"
        )
        if (bandCount == 6) {
            val tc = TEMPCO_BANDS.firstOrNull { it.first == tempco }?.second
            lines += "温度系数：${tc ?: "—"} ppm/K"
        }
        lines += "读数方式：" + when (bandCount) {
            4 -> "前两环数字 ${digitColors.joinToString("-")} → 倍率 ${mult} 环 → 误差 ${tol} 环"
            5 -> "前三环数字 ${digitColors.joinToString("-")} → 倍率 ${mult} 环 → 误差 ${tol} 环"
            else -> "前三环数字 ${digitColors.joinToString("-")} → 倍率 ${mult} 环 → 误差 ${tol} 环 → 温度系数 ${tempco} 环"
        }
        forward = lines.joinToString("\n")
    }

    fun calcReverse() {
        error = ""
        reverse = ""
        val ohm = parseOhm(ohmText) ?: run { error = "请输入有效阻值（如 470、4.7k、1M）"; return }
        val r = if (bandCount == 4) reverseBands(ohm) else reverseBands3(ohm)
        if (r == null) {
            error = if (bandCount == 4) "超出四环电阻可表示范围（约 0.1Ω ~ 99GΩ）"
            else "超出五/六环电阻可表示范围（约 1Ω ~ 999GΩ）"
            return
        }
        val bands = r.first + tol
        reverse = "推荐色环（${bandCount} 环）：${bands.joinToString(" - ")}\n" +
            "第 1~${if (bandCount == 4) 2 else 3} 环为有效数字，" +
            "第 ${if (bandCount == 4) 3 else 4} 环为倍率，第 ${if (bandCount == 4) 4 else 5} 环为误差" +
            (if (bandCount == 6) "，第 6 环为温度系数（建议 $tempco）" else "") + "\n" +
            "标称值：${fmtOhm(r.second)}（±${f3(band(tol).tolerance ?: 20.0)}%）\n" +
            "与原值误差：${f3(abs(r.second - ohm) / ohm * 100)}%"
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "选择电阻色环数量") {
            SelectorField(
                "色环数量",
                if (bandCount == 4) "四环（普通碳膜）" else if (bandCount == 5) "五环（精密）" else "六环（精密+温度系数）",
                listOf("四环（普通碳膜）", "五环（精密）", "六环（精密+温度系数）"),
                { bandCount = if (it.startsWith("四")) 4 else if (it.startsWith("五")) 5 else 6 }
            )
        }

        SectionCard(title = "色环 → 阻值（${bandCount} 环）") {
            if (bandCount == 4) {
                SelectorField("第 1 环（数字）", b1, COLOR_BANDS.take(10).map { it.name }, { b1 = it })
                Spacer(Modifier.height(8.dp))
                SelectorField("第 2 环（数字）", b2, COLOR_BANDS.take(10).map { it.name }, { b2 = it })
                Spacer(Modifier.height(8.dp))
                SelectorField("第 3 环（倍率）", mult, COLOR_BANDS.map { it.name }, { mult = it })
                Spacer(Modifier.height(8.dp))
                SelectorField("第 4 环（误差）", tol, COLOR_BANDS.filter { it.tolerance != null }.map { it.name } + "无色", { tol = it })
            } else {
                SelectorField("第 1 环（数字）", b1, COLOR_BANDS.take(10).map { it.name }, { b1 = it })
                Spacer(Modifier.height(8.dp))
                SelectorField("第 2 环（数字）", b2, COLOR_BANDS.take(10).map { it.name }, { b2 = it })
                Spacer(Modifier.height(8.dp))
                SelectorField("第 3 环（数字）", b3, COLOR_BANDS.take(10).map { it.name }, { b3 = it })
                Spacer(Modifier.height(8.dp))
                SelectorField("第 4 环（倍率）", mult, COLOR_BANDS.map { it.name }, { mult = it })
                Spacer(Modifier.height(8.dp))
                SelectorField("第 5 环（误差）", tol, COLOR_BANDS.filter { it.tolerance != null }.map { it.name } + "无色", { tol = it })
                if (bandCount == 6) {
                    Spacer(Modifier.height(8.dp))
                    SelectorField("第 6 环（温度系数）", tempco, TEMPCO_BANDS.map { it.first } + "无色", { tempco = it })
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { calcForward() }, modifier = Modifier.fillMaxWidth()) { Text("读取阻值") }
            if (forward.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(forward, fontWeight = FontWeight.Bold)
            }
        }

        SectionCard(title = "阻值 → 色环（反查，当前 ${bandCount} 环）") {
            LabeledField(ohmText, { ohmText = it }, "输入阻值（如 470、4.7k、1M）")
            Spacer(Modifier.height(12.dp))
            Button(onClick = { calcReverse() }, modifier = Modifier.fillMaxWidth()) { Text("反查色环") }
            if (reverse.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(reverse, fontWeight = FontWeight.Bold)
            }
        }

        SectionCard(title = "电阻色环怎么读") {
            Text(
                "① 颜色对应数字：黑 0、棕 1、红 2、橙 3、黄 4、绿 5、蓝 6、紫 7、灰 8、白 9。\n" +
                    "② 四环：第 1、2 环是有效数字，第 3 环是倍率（×10ⁿ），第 4 环是误差（金 ±5%、银 ±10%、棕 ±1%、无色 ±20%）。\n" +
                    "③ 五环：第 1、2、3 环是有效数字，第 4 环是倍率，第 5 环是误差，精度更高。\n" +
                    "④ 六环：前五环同五环，第 6 环是温度系数（棕 100、红 50、橙 15、黄 25、蓝 10、紫 5 ppm/K）。\n" +
                    "⑤ 判方向：误差环（金/银/棕）一般单独在最后；最后一环与前面环之间的间距通常略大；实在分不清时，用万用表量一下再反查。\n" +
                    "⑥ 例：棕-黑-红-金 = 10×100 Ω = 1 kΩ，误差 ±5%；红-红-黑-棕-棕 = 220×10 Ω = 2.2 kΩ，误差 ±1%。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ErrorText(error)
    }
}

// ============================================================
// 25. LED 限流电阻计算器
// ============================================================
@Composable
fun LedResistorTool() {
    var vs by remember { mutableStateOf("5") }
    var vf by remember { mutableStateOf("2.0") }
    var current by remember { mutableStateOf("20") }
    var count by remember { mutableStateOf("1") }
    var result by remember { mutableStateOf(listOf<String>()) }
    var error by remember { mutableStateOf("") }

    fun nearestStandard(r: Double, series: List<Double>): Double {
        if (r <= 0) return 0.0
        val e = kotlin.math.floor(log10(r)).toInt()
        val mantissa = r / 10.0.pow(e.toDouble())
        val near = series.minByOrNull { abs(it - mantissa) } ?: 1.0
        return near * 10.0.pow(e.toDouble())
    }

    fun calc() {
        error = ""
        result = emptyList()
        val vSupply = d(vs); val vLed = d(vf); val iMa = d(current); val n = count.toIntOrNull()
        if (vSupply == null || vLed == null || iMa == null || n == null || n <= 0) {
            error = "请输入有效参数"
            return
        }
        val iA = iMa / 1000.0
        val vDrop = vSupply - n * vLed
        if (vDrop <= 0) { error = "电源电压必须大于 ${n} 颗 LED 正向电压之和（${f3(n * vLed)}V）"; return }
        val r = vDrop / iA
        val p = iA * iA * r
        val e12 = nearestStandard(r, listOf(1.0, 1.2, 1.5, 1.8, 2.2, 2.7, 3.3, 3.9, 4.7, 5.6, 6.8, 8.2))
        val e24v = nearestStandard(r, E24.map { it / 10.0 })
        result = listOf(
            "所需电阻：${f3(r)} Ω（${fmtOhm(r)}）",
            "电阻功耗：${f3(p * 1000)} mW",
            "建议功率：${f3(p * 2 * 1000)} mW（留 2 倍余量）",
            "E12 最接近：${fmtOhm(e12)}",
            "E24 最接近：${fmtOhm(e24v)}"
        )
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "电路参数") {
            LabeledField(vs, { vs = it }, "电源电压 Vs（V）", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(8.dp))
            LabeledField(vf, { vf = it }, "单颗 LED 正向电压 Vf（V）", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(8.dp))
            LabeledField(current, { current = it }, "工作电流 If（mA）", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(8.dp))
            LabeledField(count, { count = it }, "串联 LED 数量", keyboardType = KeyboardType.Number)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { calc() }, modifier = Modifier.fillMaxWidth()) { Text("计算限流电阻") }
        }
        ErrorText(error)
        if (result.isNotEmpty()) {
            SectionCard(title = "结果") {
                result.forEach { InfoRow("", it) }
            }
        }
    }
}
