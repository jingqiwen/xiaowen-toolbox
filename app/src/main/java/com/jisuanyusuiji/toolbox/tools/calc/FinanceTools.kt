package com.jisuanyusuiji.toolbox.tools.calc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.InfoRow
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow

private fun money(v: Double): String =
    try { BigDecimal(v).setScale(2, RoundingMode.HALF_UP).toPlainString() } catch (_: Exception) { v.toString() }

private fun d(text: String): Double? = text.trim().toDoubleOrNull()

// ============================================================
// 16. 金融存款利息计算器
// ============================================================
@Composable
fun DepositInterestTool() {
    var mode by remember { mutableStateOf("定期存款") }
    var principal by remember { mutableStateOf("10000") }
    var rateText by remember { mutableStateOf("2.0") }
    var years by remember { mutableStateOf("1") }
    var days by remember { mutableStateOf("90") }
    var compound by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf(listOf<String>()) }
    var error by remember { mutableStateOf("") }

    fun calc() {
        error = ""
        result = emptyList()
        val p = d(principal)
        val r = d(rateText)?.div(100.0)
        if (p == null || p < 0) { error = "请输入有效本金"; return }
        if (r == null || r < 0) { error = "请输入有效年利率"; return }
        if (mode == "定期存款") {
            val y = d(years) ?: run { error = "请输入有效年限"; return }
            if (compound) {
                val total = p * (1.0 + r).pow(y)
                result = listOf(
                    "计息方式：复利（利息也参与计息，按年复利）",
                    "公式：本息合计 = 本金 × (1 + 年利率)^年数",
                    "代入：${money(p)} × (1 + ${money(r * 100)}%)^${money(y)}",
                    "本息合计：${money(total)} 元",
                    "利息：${money(total - p)} 元",
                    "说明：复利比单利多出的部分是“利滚利”产生的。"
                )
            } else {
                val interest = p * r * y
                result = listOf(
                    "计息方式：单利（只按最初本金计息）",
                    "公式：利息 = 本金 × 年利率 × 年数",
                    "代入：${money(p)} × ${money(r * 100)}% × ${money(y)}",
                    "利息：${money(interest)} 元",
                    "本息合计：${money(p + interest)} 元"
                )
            }
        } else {
            val dd = d(days) ?: run { error = "请输入有效天数"; return }
            val interest = p * r * dd / 360.0
            result = listOf(
                "计息方式：活期存款（银行惯例按 360 天/年）",
                "公式：利息 = 本金 × 年利率 × 存款天数 ÷ 360",
                "代入：${money(p)} × ${money(r * 100)}% × ${money(dd)} ÷ 360",
                "利息：${money(interest)} 元",
                "本息合计：${money(p + interest)} 元",
                "注意：活期一般按季度结息，实际利息以银行系统为准。"
            )
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "存款方式") {
            ChoiceChips(listOf("定期存款", "活期存款"), mode, { mode = it }, { it })
        }
        SectionCard(title = "参数") {
            LabeledField(principal, { principal = it }, "本金（元）", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(8.dp))
            LabeledField(rateText, { rateText = it }, "年利率（%，如 2.0）", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(8.dp))
            if (mode == "定期存款") {
                LabeledField(years, { years = it }, "存期（年）", keyboardType = KeyboardType.Decimal)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("按年复利（关闭则为单利）", modifier = Modifier.weight(1f))
                    Switch(checked = compound, onCheckedChange = { compound = it })
                }
            } else {
                LabeledField(days, { days = it }, "存款天数", keyboardType = KeyboardType.Number)
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { calc() }, modifier = Modifier.fillMaxWidth()) { Text("计算利息") }
        }
        ErrorText(error)
        if (result.isNotEmpty()) {
            SectionCard(title = "结果与计算过程") {
                result.forEach { InfoRow("", it) }
            }
        }
        SectionCard(title = "💰 存款利息知识点") {
            Text(
                "1. 单利：只对本金计息。\n" +
                    "   利息 = 本金 × 年利率 × 年数\n\n" +
                    "2. 复利：利息也会产生利息（利滚利）。\n" +
                    "   本息 = 本金 × (1 + 年利率)^年数\n\n" +
                    "3. 年利率 vs 年化收益率：定期存款用「年利率」；理财产品常用「七日年化」等，只是估算，不等于实际收益。\n\n" +
                    "4. 活期利息：通常按 360 天/年、按季结息；金额小、流动性高。\n\n" +
                    "5. 定期提前支取：一般按活期利率计息，会损失大部分利息，存钱前要规划好期限。\n\n" +
                    "6. 利息税：目前中国个人存款利息暂免征收利息税，政策如有调整以最新规定为准。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ============================================================
// 17. 房贷计算器
// ============================================================
@Composable
fun MortgageTool() {
    var amount by remember { mutableStateOf("1000000") }
    var years by remember { mutableStateOf("30") }
    var rateText by remember { mutableStateOf("3.5") }
    var mode by remember { mutableStateOf("等额本息") }
    var result by remember { mutableStateOf(listOf<String>()) }
    var error by remember { mutableStateOf("") }

    fun calc() {
        error = ""
        result = emptyList()
        val p = d(amount) ?: run { error = "请输入有效贷款金额"; return }
        val y = d(years) ?: run { error = "请输入有效年限"; return }
        val annual = d(rateText)?.div(100.0) ?: run { error = "请输入有效年利率"; return }
        if (p <= 0 || y <= 0 || annual <= 0) { error = "数值必须大于 0"; return }
        val n = (y * 12).toInt()
        val i = annual / 12.0

        if (mode == "等额本息") {
            val factor = (1.0 + i).pow(n)
            val monthly = if (factor == 1.0) p / n else p * i * factor / (factor - 1.0)
            val total = monthly * n
            result = listOf(
                "每月月供：${money(monthly)} 元",
                "还款总额：${money(total)} 元",
                "利息总额：${money(total - p)} 元",
                "月利率：${money(i * 100)}%（年利率 ${annual * 100}%）"
            )
        } else {
            val principalPerMonth = p / n
            val first = principalPerMonth + p * i
            val totalInterest = p * i * (n + 1) / 2.0
            result = listOf(
                "首月还款：${money(first)} 元",
                "每月递减：${money(principalPerMonth * i)} 元",
                "末月还款：${money(principalPerMonth + principalPerMonth * i)} 元",
                "利息总额：${money(totalInterest)} 元",
                "还款总额：${money(p + totalInterest)} 元"
            )
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "还款方式") {
            ChoiceChips(listOf("等额本息", "等额本金"), mode, { mode = it }, { it })
        }
        SectionCard(title = "贷款参数") {
            LabeledField(amount, { amount = it }, "贷款总额（元）", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(8.dp))
            LabeledField(years, { years = it }, "贷款年限（年）", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(8.dp))
            LabeledField(rateText, { rateText = it }, "年利率（%，如 3.5）", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { calc() }, modifier = Modifier.fillMaxWidth()) { Text("计算房贷") }
        }
        ErrorText(error)
        if (result.isNotEmpty()) {
            SectionCard(title = "结果") {
                result.forEach { InfoRow("", it) }
            }
        }
    }
}

// ============================================================
// 18. 复利 / 简易年金计算器
// ============================================================
@Composable
fun AnnuityTool() {
    var mode by remember { mutableStateOf("复利终值") }
    var amount by remember { mutableStateOf("10000") }
    var years by remember { mutableStateOf("10") }
    var rateText by remember { mutableStateOf("3.0") }
    var result by remember { mutableStateOf(listOf<String>()) }
    var error by remember { mutableStateOf("") }

    fun calc() {
        error = ""
        result = emptyList()
        val p = d(amount) ?: run { error = "请输入有效金额"; return }
        val y = d(years) ?: run { error = "请输入有效年限"; return }
        val r = d(rateText)?.div(100.0) ?: run { error = "请输入有效利率"; return }
        if (y < 0) { error = "年限不能为负"; return }

        when (mode) {
            "复利终值" -> {
                val fv = p * (1.0 + r).pow(y)
                result = listOf("复利终值：${money(fv)} 元", "利息：${money(fv - p)} 元")
            }
            "年金终值" -> {
                if (r == 0.0) result = listOf("年金终值：${money(p * y)} 元")
                else result = listOf("年金终值：${money(p * ((1.0 + r).pow(y) - 1.0) / r)} 元", "总投入：${money(p * y)} 元")
            }
            "年金现值" -> {
                if (r == 0.0) result = listOf("年金现值：${money(p * y)} 元")
                else result = listOf("年金现值：${money(p * (1.0 - (1.0 + r).pow(-y)) / r)} 元", "总支付：${money(p * y)} 元")
            }
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "计算类型") {
            ChoiceChips(listOf("复利终值", "年金终值", "年金现值"), mode, { mode = it }, { it })
        }
        SectionCard(title = "参数（每年末投入/支付一次）") {
            LabeledField(amount, { amount = it }, "本金 / 每年金额（元）", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(8.dp))
            LabeledField(years, { years = it }, "年限（年）", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(8.dp))
            LabeledField(rateText, { rateText = it }, "年利率（%）", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { calc() }, modifier = Modifier.fillMaxWidth()) { Text("计算") }
        }
        ErrorText(error)
        if (result.isNotEmpty()) {
            SectionCard(title = "结果") {
                result.forEach { InfoRow("", it) }
            }
        }
    }
}
