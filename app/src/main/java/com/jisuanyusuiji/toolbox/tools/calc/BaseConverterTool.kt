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
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.ui.components.CopyButton
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import com.jisuanyusuiji.toolbox.ui.components.SelectorField
import java.math.BigDecimal
import java.math.RoundingMode

private val BASE_OPTIONS = listOf("2 二进制", "8 八进制", "10 十进制", "16 十六进制")

private fun baseOf(option: String): Int = option.substringBefore(" ").toInt()

/** 支持小数的任意进制互转（2/8/10/16）。整数部分精确，小数部分最多 20 位。 */
fun convertBase(input: String, fromBase: Int, toBase: Int, precision: Int = 20): String {
    var text = input.trim().replace(" ", "")
    if (text.isEmpty()) throw IllegalArgumentException("请输入要转换的数字")
    var negative = false
    if (text.startsWith("-")) { negative = true; text = text.drop(1) }
    if (text.startsWith("+")) text = text.drop(1)

    val intPart = text.substringBefore(".").ifBlank { "0" }
    val fracPart = if (text.contains(".")) text.substringAfter(".") else ""

    // 解析整数部分（精确）
    var whole = BigDecimal.ZERO
    for (c in intPart) {
        val d = Character.digit(c, fromBase)
        if (d < 0) throw IllegalArgumentException("“$c”不是${fromBase}进制的有效数字")
        whole = whole.multiply(BigDecimal(fromBase)).add(BigDecimal(d))
    }

    // 解析小数部分（2/8/10/16 的分数在十进制下均可精确表示）
    var frac = BigDecimal.ZERO
    if (fracPart.isNotEmpty()) {
        var den = BigDecimal(fromBase)
        for (c in fracPart) {
            val d = Character.digit(c, fromBase)
            if (d < 0) throw IllegalArgumentException("“$c”不是${fromBase}进制的有效数字")
            frac = frac.add(BigDecimal(d).divide(den, 30, RoundingMode.HALF_UP))
            den = den.multiply(BigDecimal(fromBase))
        }
    }

    // 转换为目标进制
    val intDigits = whole.toBigInteger().toString(toBase)
    val sb = StringBuilder()
    if (negative) sb.append("-")
    sb.append(if (intDigits.isEmpty()) "0" else intDigits)

    var remain = frac
    if (remain.compareTo(BigDecimal.ZERO) != 0) {
        sb.append(".")
        var i = 0
        while (remain.compareTo(BigDecimal.ZERO) != 0 && i < precision) {
            val x = remain.multiply(BigDecimal(toBase))
            val d = x.toInt()
            sb.append(Character.forDigit(d, toBase).uppercaseChar())
            remain = x.subtract(BigDecimal(d))
            i++
        }
    }
    return sb.toString()
}

@Composable
fun BaseConverterTool() {
    var fromOption by remember { mutableStateOf("10 十进制") }
    var toOption by remember { mutableStateOf("2 二进制") }
    var input by remember { mutableStateOf("255.5") }
    var result by remember { mutableStateOf("") }
    var allBases by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var error by remember { mutableStateOf("") }

    fun convert() {
        error = ""
        result = ""
        allBases = emptyList()
        try {
            val from = baseOf(fromOption)
            val to = baseOf(toOption)
            result = convertBase(input, from, to)
            allBases = BASE_OPTIONS.map { opt ->
                val base = baseOf(opt)
                opt to convertBase(input, from, base)
            }
        } catch (e: Exception) {
            error = e.message ?: "转换失败"
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "进制转换（支持小数）") {
            SelectorField("源进制", fromOption, BASE_OPTIONS, { fromOption = it })
            Spacer(Modifier.height(10.dp))
            SelectorField("目标进制", toOption, BASE_OPTIONS, { toOption = it })
            Spacer(Modifier.height(10.dp))
            LabeledField(input, { input = it }, "要转换的数字（如 255.5、1A.8）")
            Spacer(Modifier.height(12.dp))
            Button(onClick = { convert() }, modifier = Modifier.fillMaxWidth()) { Text("转换") }
        }

        ErrorText(error)

        if (result.isNotBlank()) {
            SectionCard(title = "转换结果（${toOption}）") {
                Text(result, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                CopyButton(result)
            }
            SectionCard(title = "同时查看四种进制") {
                allBases.forEach { (name, value) ->
                    Text(
                        "$name：$value",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}
