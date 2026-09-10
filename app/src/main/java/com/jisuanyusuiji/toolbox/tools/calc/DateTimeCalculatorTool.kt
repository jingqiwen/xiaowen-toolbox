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
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.InfoRow
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

private fun parseDate(text: String): LocalDate? = try {
    LocalDate.parse(text.trim(), DATE_FMT)
} catch (_: Exception) {
    null
}

private fun today(): String = LocalDate.now().format(DATE_FMT)

@Composable
fun DateTimeCalculatorTool() {
    var date1 by remember { mutableStateOf(today()) }
    var date2 by remember { mutableStateOf(today()) }
    var daysInput by remember { mutableStateOf("30") }
    var saturdayWeekend by remember { mutableStateOf(true) }
    var sundayWeekend by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf(listOf<String>()) }
    var error by remember { mutableStateOf("") }

    fun interval() {
        error = ""
        val d1 = parseDate(date1)
        val d2 = parseDate(date2)
        if (d1 == null || d2 == null) { error = "日期格式应为 yyyy-MM-dd，例如 2026-01-01"; return }
        val days = ChronoUnit.DAYS.between(d1, d2)
        result = listOf(
            "两日期间隔：${kotlin.math.abs(days)} 天",
            "相差周数：${kotlin.math.abs(days) / 7.0} 周",
            "相差月数（约）：${kotlin.math.abs(days) / 30.44} 个月",
            "相差年数（约）：${kotlin.math.abs(days) / 365.25} 年",
            if (d2 > d1) "距 $date2 还有 $days 天" else if (d2 < d1) "$date2 已过去 ${-days} 天" else "两个日期相同"
        )
    }

    fun addDays() {
        error = ""
        val base = parseDate(date1) ?: run { error = "日期格式应为 yyyy-MM-dd"; return }
        val n = daysInput.toLongOrNull() ?: run { error = "天数必须是整数"; return }
        val target = base.plusDays(n)
        result = listOf("$date1 ${if (n >= 0) "+" else ""}$n 天 = ${target.format(DATE_FMT)}", "星期：${weekName(target.dayOfWeek)}")
    }

    fun workdays() {
        error = ""
        val d1 = parseDate(date1)
        val d2 = parseDate(date2)
        if (d1 == null || d2 == null) { error = "日期格式应为 yyyy-MM-dd"; return }
        var count = 0L
        var cur = if (d1 <= d2) d1 else d2
        val end = if (d1 <= d2) d2 else d1
        while (!cur.isAfter(end)) {
            val weekend = (cur.dayOfWeek == DayOfWeek.SATURDAY && saturdayWeekend) ||
                (cur.dayOfWeek == DayOfWeek.SUNDAY && sundayWeekend)
            if (!weekend) count++
            cur = cur.plusDays(1)
        }
        result = listOf("工作日（不含自定义周末）：$count 天", "含首尾两天", "周末设置：${if (saturdayWeekend) "周六" else ""} ${if (sundayWeekend) "周日" else ""}".trim())
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "📅 两日期间隔") {
            LabeledField(date1, { date1 = it }, "日期 1（yyyy-MM-dd）")
            Spacer(Modifier.height(8.dp))
            LabeledField(date2, { date2 = it }, "日期 2（yyyy-MM-dd）")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { date1 = today() }, modifier = Modifier.weight(1f)) { Text("今天") }
                Button(onClick = { date2 = today() }, modifier = Modifier.weight(1f)) { Text("今天") }
                Button(onClick = { interval() }, modifier = Modifier.weight(1f)) { Text("计算间隔") }
            }
        }

        SectionCard(title = "➕ N 天前/后的日期") {
            LabeledField(daysInput, { daysInput = it }, "天数（负数表示之前）", keyboardType = KeyboardType.Number)
            Spacer(Modifier.height(10.dp))
            Button(onClick = { addDays() }, modifier = Modifier.fillMaxWidth()) { Text("计算日期") }
        }

        SectionCard(title = "💼 工作日计算（可自定义周末）") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("周六算周末", modifier = Modifier.weight(1f))
                Switch(checked = saturdayWeekend, onCheckedChange = { saturdayWeekend = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("周日算周末", modifier = Modifier.weight(1f))
                Switch(checked = sundayWeekend, onCheckedChange = { sundayWeekend = it })
            }
            Spacer(Modifier.height(8.dp))
            Text("使用上方“日期 1 / 日期 2”作为起止日期", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { workdays() }, modifier = Modifier.fillMaxWidth()) { Text("计算工作日") }
        }

        ErrorText(error)

        if (result.isNotEmpty()) {
            SectionCard(title = "结果") {
                result.forEach { InfoRow("", it) }
                Spacer(Modifier.height(6.dp))
                Text(
                    "倒计时提示：把“日期 2”设为目标日期，点“计算间隔”即可看到剩余天数。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun weekName(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "一"
    DayOfWeek.TUESDAY -> "二"
    DayOfWeek.WEDNESDAY -> "三"
    DayOfWeek.THURSDAY -> "四"
    DayOfWeek.FRIDAY -> "五"
    DayOfWeek.SATURDAY -> "六"
    DayOfWeek.SUNDAY -> "日"
}
