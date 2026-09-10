package com.jisuanyusuiji.toolbox.tools.extra

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.data.JsonStore
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.InfoRow
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun d2(text: String): Double? = text.trim().toDoubleOrNull()
private fun money2(v: Double): String = BigDecimal(v).setScale(2, RoundingMode.HALF_UP).toPlainString()

// ============================================================
// 42. BMI 计算器（性别 + 分析）
// ============================================================
@Composable
fun BmiTool() {
    var gender by remember { mutableStateOf("男") }
    var age by remember { mutableStateOf("25") }
    var height by remember { mutableStateOf("175") }
    var weight by remember { mutableStateOf("70") }
    var result by remember { mutableStateOf("") }
    var details by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var analysis by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    fun calc() {
        error = ""
        result = ""
        details = emptyList()
        analysis = ""
        val hCm = d2(height)
        val w = d2(weight)
        val a = d2(age)
        if (hCm == null || hCm <= 0 || w == null || w <= 0 || a == null || a <= 0) {
            error = "请输入有效身高、体重和年龄"
            return
        }
        if (hCm < 80 || hCm > 250 || w > 400 || a > 120) { error = "请输入合理的数值范围"; return }
        val h = hCm / 100.0
        val bmi = w / (h * h)
        result = String.format(Locale.getDefault(), "BMI = %.1f", bmi)

        val category = when {
            bmi < 18.5 -> "偏瘦"
            bmi < 24 -> "正常"
            bmi < 28 -> "超重"
            else -> "肥胖"
        }
        val bmr = if (gender == "男") 10 * w + 6.25 * hCm - 5 * a + 5 else 10 * w + 6.25 * hCm - 5 * a - 161
        val bodyFat = 1.2 * bmi + 0.23 * a - 10.8 * (if (gender == "男") 1 else 0) - 5.4
        val standardMin = 18.5 * h * h
        val standardMax = 23.9 * h * h
        details = listOf(
            "性别" to gender,
            "年龄" to "${a.toInt()} 岁",
            "BMI 区间" to "$category（中国标准）",
            "标准体重范围" to "${money2(standardMin)} ~ ${money2(standardMax)} kg",
            "基础代谢率 BMR（估算）" to "${money2(bmr)} kcal/天",
            "体脂率（估算）" to "${money2(bodyFat)}%",
            "每日建议摄入（轻度活动）" to "${money2(bmr * 1.375)} kcal/天"
        )

        analysis = buildString {
            append("【${gender}性 · ${a.toInt()}岁 · $category】\n")
            when (category) {
                "偏瘦" -> append("体重偏低，建议适当增加优质蛋白和主食摄入，配合力量训练增加肌肉量；如长期偏瘦建议排查甲状腺、消化吸收等问题。")
                "正常" -> append("体重处于健康范围，继续保持均衡饮食与规律运动即可。建议每周 150 分钟中等强度运动。")
                "超重" -> append("体重超出健康范围，建议控制精制碳水和油脂摄入，每周进行 3~5 次有氧运动，并保证充足睡眠。")
                else -> append("已达到肥胖范围，建议在医生或营养师指导下制定减重计划，避免极端节食，循序渐进、每周减重 0.5~1kg 为宜。")
            }
            append("\n")
            if (gender == "男" && bodyFat > 25) append("男性体脂率偏高（健康参考约 10%~20%），建议增加力量训练。\n")
            if (gender == "女" && bodyFat > 32) append("女性体脂率偏高（健康参考约 18%~28%），建议增加有氧与力量结合训练。\n")
            append("说明：以上均为公式估算，仅供参考，不能替代医生或专业机构的诊断。")
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "身体质量指数 BMI（中国标准）") {
            ChoiceChips(listOf("男", "女"), gender, { gender = it }, { it })
            Spacer(Modifier.height(8.dp))
            LabeledField(age, { age = it }, "年龄（岁）", keyboardType = KeyboardType.Number)
            Spacer(Modifier.height(8.dp))
            LabeledField(height, { height = it }, "身高（cm）", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(8.dp))
            LabeledField(weight, { weight = it }, "体重（kg）", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { calc() }, modifier = Modifier.fillMaxWidth()) { Text("计算并分析") }
        }
        ErrorText(error)
        if (result.isNotBlank()) {
            SectionCard(title = "结果") {
                Text(result, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                details.forEach { (label, value) -> InfoRow("$label：", value) }
            }
            SectionCard(title = "身体分析") {
                Text(analysis, style = MaterialTheme.typography.bodyMedium)
            }
        }
        SectionCard(title = "BMI 参考标准（中国成人）") {
            Text("偏瘦：< 18.5\n正常：18.5 ~ 23.9\n超重：24 ~ 27.9\n肥胖：≥ 28", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ============================================================
// 43. 油耗计算器
// ============================================================
@Composable
fun FuelConsumptionTool() {
    val context = LocalContext.current
    val store = remember { JsonStore(context, "fuel") }
    var records by remember { mutableStateOf(loadFuelRecords(store)) }
    var odometer by remember { mutableStateOf("") }
    var liters by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    fun addRecord() {
        error = ""
        val km = d2(odometer)
        val l = d2(liters)
        if (km == null || km <= 0 || l == null || l <= 0) { error = "请输入有效里程和加油量"; return }
        val prev = records.firstOrNull()?.getDouble("odometer") ?: 0.0
        if (prev > 0 && km <= prev) { error = "本次里程必须大于上次里程（${money2(prev)} km）"; return }
        val obj = JSONObject()
            .put("date", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))
            .put("odometer", km)
            .put("liters", l)
        records = (listOf(obj) + records).toMutableList()
        saveFuelRecords(store, records)
        odometer = ""
        liters = ""
    }

    // 计算每次加油区间的油耗
    val intervals = records.zipWithNext().mapNotNull { pair ->
        val cur = pair.first
        val prev = pair.second
        val dist = cur.getDouble("odometer") - prev.getDouble("odometer")
        if (dist > 0 && prev.getDouble("odometer") > 0) {
            Triple(cur.getString("date"), dist, cur.getDouble("liters"))
        } else null
    }
    val avg = if (intervals.isNotEmpty()) intervals.sumOf { it.second } / intervals.sumOf { it.third } else 0.0

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "记录一次加油") {
            LabeledField(odometer, { odometer = it }, "当前总里程（km）", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(8.dp))
            LabeledField(liters, { liters = it }, "本次加油量（L）", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { addRecord() }, modifier = Modifier.fillMaxWidth()) { Text("添加记录") }
            ErrorText(error)
        }
        SectionCard(title = "统计（本地保存）") {
            Text("平均油耗：${money2(avg)} L/100km", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("说明：平均油耗 = 各次加油区间行驶里程之和 ÷ 加油量之和", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            if (intervals.isEmpty()) Text("至少添加两次记录后才能计算", color = MaterialTheme.colorScheme.onSurfaceVariant)
            intervals.take(20).forEach { (date, dist, fuel) ->
                Text(
                    "$date  行驶 ${money2(dist)} km，加油 ${money2(fuel)} L = ${money2(fuel / dist * 100)} L/100km",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (records.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = {
                    records = mutableListOf()
                    saveFuelRecords(store, records)
                }) { Text("清空记录") }
            }
        }
    }
}

private fun loadFuelRecords(store: JsonStore): MutableList<JSONObject> {
    val arr = store.getArray("records")
    return (0 until arr.length()).mapNotNull { i ->
        try { arr.getJSONObject(i) } catch (_: Exception) { null }
    }.toMutableList()
}

private fun saveFuelRecords(store: JsonStore, records: List<JSONObject>) {
    val arr = JSONArray()
    records.take(200).forEach { arr.put(it) }
    store.putArray("records", arr)
}

// ============================================================
// 44. 多人 AA 分摊计算器
// ============================================================
@Composable
fun AaSplitTool() {
    var amount by remember { mutableStateOf("358.6") }
    var people by remember { mutableStateOf("3") }
    var tip by remember { mutableStateOf("0") }
    var result by remember { mutableStateOf(listOf<String>()) }
    var error by remember { mutableStateOf("") }

    fun calc() {
        error = ""
        result = emptyList()
        val total = amount.trim().toBigDecimalOrNull() ?: run { error = "请输入有效金额"; return }
        val n = people.trim().toIntOrNull() ?: run { error = "请输入有效人数"; return }
        val tipPct = tip.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
        if (total <= BigDecimal.ZERO || n <= 0 || tipPct < BigDecimal.ZERO) { error = "数值必须大于等于 0"; return }
        val totalFen = total.multiply(BigDecimal(100)).add(
            total.multiply(tipPct).divide(BigDecimal(100)).multiply(BigDecimal(100))
        ).setScale(0, RoundingMode.HALF_UP).longValueExact()
        val base = totalFen / n
        val remainder = (totalFen % n).toInt()
        val lines = mutableListOf<String>()
        for (i in 1..n) {
            val fen = base + if (i <= remainder) 1 else 0
            lines.add("第 ${i} 人：${BigDecimal(fen).divide(BigDecimal(100)).toPlainString()} 元")
        }
        result = listOf(
            "总金额：${money2(totalFen / 100.0)} 元（含 ${tip.trim()}% 小费）",
            "平均约：${money2(totalFen / 100.0 / n)} 元/人"
        ) + lines
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "AA 分摊") {
            LabeledField(amount, { amount = it }, "消费总额（元）", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(8.dp))
            LabeledField(people, { people = it }, "人数", keyboardType = KeyboardType.Number)
            Spacer(Modifier.height(8.dp))
            LabeledField(tip, { tip = it }, "小费比例（%，没有填 0）", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { calc() }, modifier = Modifier.fillMaxWidth()) { Text("计算分摊") }
        }
        ErrorText(error)
        if (result.isNotEmpty()) {
            SectionCard(title = "结果（精确到分，差额由前几人多付 1 分）") {
                result.forEach { InfoRow("", it) }
            }
        }
    }
}
