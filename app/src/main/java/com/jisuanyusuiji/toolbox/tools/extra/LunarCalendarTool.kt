package com.jisuanyusuiji.toolbox.tools.extra

import android.icu.util.ChineseCalendar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.InfoRow
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale

private val CN_MONTHS = listOf("正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "冬月", "腊月")
private val CN_DAYS = listOf(
    "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
    "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
    "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
)
private val STEMS = "甲乙丙丁戊己庚辛壬癸"
private val BRANCHES = "子丑寅卯辰巳午未申酉戌亥"
private val ZODIAC = listOf("鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪")

fun yearGanZhi(year: Int): String =
    "${STEMS[((year - 4) % 10 + 10) % 10]}${BRANCHES[((year - 4) % 12 + 12) % 12]}"

fun zodiac(year: Int): String = ZODIAC[((year - 4) % 12 + 12) % 12]

/** 日干支：0 = 甲子。使用儒略日公式（2000-01-01 为戊午，已验证）。 */
fun dayGanZhi(date: LocalDate): String {
    val jdn = date.toEpochDay() + 2440588
    val idx = (((jdn + 49) % 60 + 60) % 60).toInt()
    return "${STEMS[idx % 10]}${BRANCHES[idx % 12]}"
}

// ============================================================
// 45. 农历公历互查万年历
// ============================================================
@Composable
fun LunarCalendarTool() {
    var mode by remember { mutableStateOf("公历 → 农历") }
    var gregorianText by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var lunarYear by remember { mutableStateOf("2026") }
    var lunarMonth by remember { mutableStateOf("1") }
    var lunarDay by remember { mutableStateOf("1") }
    var leap by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf(listOf<String>()) }
    var error by remember { mutableStateOf("") }

    fun toLunar() {
        error = ""
        result = emptyList()
        try {
            val d = LocalDate.parse(gregorianText.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
            val cal = ChineseCalendar()
            cal.timeInMillis = GregorianCalendar(d.year, d.monthValue - 1, d.dayOfMonth).timeInMillis
            val ly = cal.get(ChineseCalendar.EXTENDED_YEAR)
            val lm = cal.get(ChineseCalendar.MONTH) + 1
            val ld = cal.get(ChineseCalendar.DAY_OF_MONTH)
            val isLeap = cal.get(ChineseCalendar.IS_LEAP_MONTH) == 1
            result = listOf(
                "公历：${d}",
                "农历：$ly 年（${zodiac(ly)}年） ${if (isLeap) "闰" else ""}${CN_MONTHS[lm - 1]}${CN_DAYS[ld - 1]}",
                "年干支：${yearGanZhi(ly)}（以农历年计）",
                "日干支：${dayGanZhi(d)}"
            )
        } catch (e: Exception) {
            error = "转换失败：${e.message ?: "请检查日期格式 yyyy-MM-dd"}"
        }
    }

    fun toGregorian() {
        error = ""
        result = emptyList()
        try {
            val y = lunarYear.trim().toInt()
            val m = lunarMonth.trim().toInt()
            val day = lunarDay.trim().toInt()
            if (m !in 1..12 || day !in 1..30 || y !in 1000..3000) { error = "年份支持 1000~3000，月份 1~12，日 1~30"; return }
            val cal = ChineseCalendar()
            cal.clear()
            cal.set(ChineseCalendar.EXTENDED_YEAR, y)
            cal.set(ChineseCalendar.MONTH, m - 1)
            cal.set(ChineseCalendar.IS_LEAP_MONTH, if (leap) 1 else 0)
            cal.set(ChineseCalendar.DAY_OF_MONTH, day)
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d = fmt.format(Date(cal.timeInMillis))
            result = listOf(
                "农历：$y 年 ${if (leap) "闰" else ""}${CN_MONTHS[m - 1]}${CN_DAYS[day - 1]}",
                "公历：$d",
                "生肖：${zodiac(y)}年",
                "年干支：${yearGanZhi(y)}"
            )
        } catch (e: Exception) {
            error = "转换失败：${e.message ?: "日期无效"}"
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "转换方向") {
            ChoiceChips(listOf("公历 → 农历", "农历 → 公历"), mode, { mode = it }, { it })
        }
        SectionCard(title = "输入") {
            if (mode == "公历 → 农历") {
                LabeledField(gregorianText, { gregorianText = it }, "公历日期（yyyy-MM-dd）")
                Spacer(Modifier.height(12.dp))
                Button(onClick = { toLunar() }, modifier = Modifier.fillMaxWidth()) { Text("查询农历") }
            } else {
                LabeledField(lunarYear, { lunarYear = it }, "农历年（1000~3000）", keyboardType = KeyboardType.Number)
                Spacer(Modifier.height(8.dp))
                LabeledField(lunarMonth, { lunarMonth = it }, "农历月（1~12）", keyboardType = KeyboardType.Number)
                Spacer(Modifier.height(8.dp))
                LabeledField(lunarDay, { lunarDay = it }, "农历日（1~30）", keyboardType = KeyboardType.Number)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("是否闰月", modifier = Modifier.weight(1f))
                    Switch(checked = leap, onCheckedChange = { leap = it })
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = { toGregorian() }, modifier = Modifier.fillMaxWidth()) { Text("查询公历") }
            }
        }
        ErrorText(error)
        if (result.isNotEmpty()) {
            SectionCard(title = "查询结果（内置 ICU 万年历，离线可用，支持 1000~3000 年）") {
                result.forEach { line ->
                    Text(line, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

// ============================================================
// 46. 生肖 / 天干地支查询
// ============================================================
@Composable
fun GanzhiTool() {
    var dateText by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var result by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var error by remember { mutableStateOf("") }

    fun query() {
        error = ""
        try {
            val d = LocalDate.parse(dateText.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
            result = listOf(
                "公历日期" to d.toString(),
                "生肖" to "${zodiac(d.year)}（按农历年）",
                "年干支" to yearGanZhi(d.year),
                "日干支" to dayGanZhi(d),
                "所属季节" to when (d.monthValue) { 3, 4, 5 -> "春"; 6, 7, 8 -> "夏"; 9, 10, 11 -> "秋"; else -> "冬" }
            )
        } catch (_: Exception) {
            error = "日期格式应为 yyyy-MM-dd"
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "生肖与干支") {
            LabeledField(dateText, { dateText = it }, "日期（yyyy-MM-dd）")
            Spacer(Modifier.height(12.dp))
            Button(onClick = { query() }, modifier = Modifier.fillMaxWidth()) { Text("查询") }
        }
        ErrorText(error)
        if (result.isNotEmpty()) {
            SectionCard(title = "结果") {
                result.forEach { (label, value) -> InfoRow("$label：", value) }
            }
        }
    }
}
