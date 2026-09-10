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
// 42. BMI 计算器
// ============================================================
@Composable
fun BmiTool() {
    var height by remember { mutableStateOf("175") }
    var weight by remember { mutableStateOf("70") }
    var result by remember { mutableStateOf("") }
    var advice by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    fun calc() {
        error = ""
        val h = d2(height)?.div(100.0)
        val w = d2(weight)
        if (h == null || h <= 0 || w == null || w <= 0) { error = "请输入有效身高和体重"; return }
        val bmi = w / (h * h)
        result = String.format(Locale.getDefault(), "BMI = %.1f", bmi)
        advice = when {
            bmi < 18.5 -> "偏瘦（<18.5）"
            bmi < 24 -> "正常（18.5 ~ 23.9）"
            bmi < 28 -> "超重（24 ~ 27.9）"
            else -> "肥胖（≥28）"
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "身体质量指数 BMI（中国标准）") {
            LabeledField(height, { height = it }, "身高（cm）", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(8.dp))
            LabeledField(weight, { weight = it }, "体重（kg）", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { calc() }, modifier = Modifier.fillMaxWidth()) { Text("计算 BMI") }
        }
        ErrorText(error)
        if (result.isNotBlank()) {
            SectionCard(title = "结果") {
                Text(result, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(advice, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
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
