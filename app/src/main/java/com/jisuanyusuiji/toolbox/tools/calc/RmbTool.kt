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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.ui.components.CopyButton
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import java.math.BigDecimal
import java.math.RoundingMode

private val CN_DIGITS = charArrayOf('零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖')
private val CN_SMALL = arrayOf("", "拾", "佰", "仟")
private val CN_BIG = arrayOf("", "万", "亿", "万亿")

/** 把 0~9999 的四位数字转为中文大写（不带大单位）。 */
private fun section(v: Int): String {
    if (v == 0) return ""
    var n = v
    val values = IntArray(4)
    for (i in 3 downTo 0) {
        values[i] = n % 10
        n /= 10
    }
    val sb = StringBuilder()
    var zeroPending = false
    for (i in 0..3) {
        val d = values[i]
        if (d == 0) {
            if (sb.isNotEmpty()) zeroPending = true
        } else {
            if (zeroPending) {
                sb.append('零')
                zeroPending = false
            }
            sb.append(CN_DIGITS[d]).append(CN_SMALL[3 - i])
        }
    }
    return sb.toString()
}

/** 阿拉伯数字转人民币大写金额。 */
fun amountToRmbUpper(input: String): String {
    var text = input.trim().replace(",", "").replace("￥", "").replace("¥", "")
    if (text.isEmpty()) throw IllegalArgumentException("请输入金额")
    var negative = false
    if (text.startsWith("-")) { negative = true; text = text.drop(1) }
    val amount = text.toBigDecimalOrNull() ?: throw IllegalArgumentException("金额格式错误")
    if (amount < BigDecimal.ZERO) throw IllegalArgumentException("请输入非负金额")
    val cents = amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
    val integerPart = cents / 100
    val jiao = ((cents % 100) / 10).toInt()
    val fen = (cents % 10).toInt()

    if (integerPart.toString().length > 16) throw IllegalArgumentException("金额过大，最多支持 16 位整数（万亿级）")

    val sb = StringBuilder()
    if (negative) sb.append("负")

    // 整数部分按 4 位分组处理
    val groups = mutableListOf<Int>()
    var n = integerPart
    do {
        groups.add((n % 10000).toInt())
        n /= 10000
    } while (n > 0)

    var zeroFlag = false
    var started = false
    for (i in groups.indices.reversed()) {
        val g = groups[i]
        if (g == 0) {
            if (started) zeroFlag = true
            continue
        }
        val sec = section(g)
        if (started && (zeroFlag || g < 1000)) sb.append('零')
        sb.append(sec).append(CN_BIG[i])
        started = true
        zeroFlag = false
    }
    if (!started) sb.append("零")
    sb.append("元")

    when {
        jiao == 0 && fen == 0 -> sb.append("整")
        else -> {
            if (jiao > 0) sb.append(CN_DIGITS[jiao]).append("角")
            if (fen > 0) {
                if (jiao == 0) sb.append("零")
                sb.append(CN_DIGITS[fen]).append("分")
            }
        }
    }
    return sb.toString()
}

@Composable
fun RmbTool() {
    var input by remember { mutableStateOf("1234567.89") }
    var result by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    fun convert() {
        error = ""
        result = ""
        try {
            result = amountToRmbUpper(input)
        } catch (e: Exception) {
            error = e.message ?: "转换失败"
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "数字转人民币大写") {
            LabeledField(input, { input = it }, "金额（元，如 1234567.89）", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { convert() }, modifier = Modifier.fillMaxWidth()) { Text("转换") }
        }
        ErrorText(error)
        if (result.isNotBlank()) {
            SectionCard(title = "财务大写") {
                Text(result, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                CopyButton(result)
            }
        }
        SectionCard(title = "示例") {
            Text("1234.56 → 壹仟贰佰叁拾肆元伍角陆分\n100000001.00 → 壹亿零壹元整")
        }
    }
}
