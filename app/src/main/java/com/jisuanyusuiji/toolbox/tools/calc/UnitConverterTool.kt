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
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.CopyButton
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import com.jisuanyusuiji.toolbox.ui.components.SelectorField
import java.math.BigDecimal
import java.math.RoundingMode

private data class UnitDef(val name: String, val factor: Double)

private data class UnitCategory(val name: String, val units: List<UnitDef>, val temp: Boolean = false)

private val UNIT_CATEGORIES = listOf(
    UnitCategory("长度", listOf(
        UnitDef("毫米 mm", 0.001), UnitDef("厘米 cm", 0.01), UnitDef("分米 dm", 0.1),
        UnitDef("米 m", 1.0), UnitDef("千米 km", 1000.0), UnitDef("英寸 in", 0.0254),
        UnitDef("英尺 ft", 0.3048), UnitDef("码 yd", 0.9144), UnitDef("英里 mi", 1609.344),
        UnitDef("海里 nmi", 1852.0), UnitDef("里", 500.0), UnitDef("丈", 10.0 / 3.0),
        UnitDef("尺", 1.0 / 3.0), UnitDef("寸", 1.0 / 30.0)
    )),
    UnitCategory("重量", listOf(
        UnitDef("毫克 mg", 0.000001), UnitDef("克 g", 0.001), UnitDef("千克 kg", 1.0),
        UnitDef("吨 t", 1000.0), UnitDef("斤", 0.5), UnitDef("两", 0.05),
        UnitDef("盎司 oz", 0.0283495), UnitDef("磅 lb", 0.453592)
    )),
    UnitCategory("面积", listOf(
        UnitDef("平方厘米 cm²", 0.0001), UnitDef("平方米 m²", 1.0), UnitDef("公顷 ha", 10000.0),
        UnitDef("平方千米 km²", 1000000.0), UnitDef("亩", 2000.0 / 3.0),
        UnitDef("平方英尺 ft²", 0.092903), UnitDef("平方英寸 in²", 0.00064516), UnitDef("英亩 acre", 4046.8564)
    )),
    UnitCategory("体积", listOf(
        UnitDef("毫升 mL", 0.001), UnitDef("立方厘米 cm³", 0.001), UnitDef("升 L", 1.0),
        UnitDef("立方米 m³", 1000.0), UnitDef("美制加仑 gal", 3.78541), UnitDef("英制加仑 gal", 4.54609)
    )),
    UnitCategory("速度", listOf(
        UnitDef("米/秒 m/s", 1.0), UnitDef("千米/时 km/h", 1.0 / 3.6), UnitDef("英里/时 mph", 0.44704),
        UnitDef("节 kn", 0.514444), UnitDef("英尺/秒 ft/s", 0.3048)
    )),
    UnitCategory("压力", listOf(
        UnitDef("帕 Pa", 1.0), UnitDef("千帕 kPa", 1000.0), UnitDef("兆帕 MPa", 1000000.0),
        UnitDef("巴 bar", 100000.0), UnitDef("标准大气压 atm", 101325.0),
        UnitDef("毫米汞柱 mmHg", 133.322), UnitDef("磅/平方英寸 psi", 6894.76)
    )),
    UnitCategory("功率", listOf(
        UnitDef("瓦 W", 1.0), UnitDef("千瓦 kW", 1000.0), UnitDef("公制马力 PS", 735.499),
        UnitDef("英制马力 hp", 745.7), UnitDef("千卡/时 kcal/h", 1.163)
    )),
    UnitCategory("能量", listOf(
        UnitDef("焦耳 J", 1.0), UnitDef("千焦 kJ", 1000.0), UnitDef("卡 cal", 4.184),
        UnitDef("千卡 kcal", 4184.0), UnitDef("瓦时 Wh", 3600.0), UnitDef("千瓦时 kWh", 3600000.0),
        UnitDef("英热单位 BTU", 1055.06), UnitDef("电子伏特 eV", 1.602176634e-19)
    )),
    UnitCategory("温度", listOf(
        UnitDef("摄氏度 °C", 1.0), UnitDef("华氏度 °F", 1.0), UnitDef("开尔文 K", 1.0)
    ), temp = true)
)

private fun toBase(category: UnitCategory, unit: UnitDef, value: Double): Double = when {
    category.temp && unit.name.startsWith("华") -> (value - 32.0) * 5.0 / 9.0
    category.temp && unit.name.startsWith("开") -> value - 273.15
    else -> value * unit.factor
}

private fun fromBase(category: UnitCategory, unit: UnitDef, base: Double): Double = when {
    category.temp && unit.name.startsWith("华") -> base * 9.0 / 5.0 + 32.0
    category.temp && unit.name.startsWith("开") -> base + 273.15
    else -> base / unit.factor
}

private fun fmt(v: Double): String =
    try {
        BigDecimal(v).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    } catch (_: Exception) {
        v.toString()
    }

@Composable
fun UnitConverterTool() {
    var categoryIndex by remember { mutableStateOf(0) }
    val category = UNIT_CATEGORIES[categoryIndex]
    var fromUnit by remember(category.name) { mutableStateOf(category.units.first()) }
    var toUnit by remember(category.name) { mutableStateOf(category.units.last()) }
    var input by remember { mutableStateOf("1") }
    var result by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    fun convert() {
        val value = input.toDoubleOrNull()
        if (value == null) {
            error = "请输入有效数字"
            result = ""
            return
        }
        error = ""
        val base = toBase(category, fromUnit, value)
        result = "${fmt(base)} ${category.units[0].name.split(" ").firstOrNull() ?: ""}\n" +
            "= ${fmt(fromBase(category, toUnit, base))} ${toUnit.name}"
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "选择类别") {
            ChoiceChips(
                options = UNIT_CATEGORIES.indices.toList(),
                selected = categoryIndex,
                onSelect = { categoryIndex = it },
                label = { UNIT_CATEGORIES[it].name }
            )
        }

        SectionCard(title = "换算") {
            LabeledField(
                value = input,
                onChange = { input = it },
                label = "数值",
                keyboardType = KeyboardType.Decimal
            )
            Spacer(Modifier.height(10.dp))
            SelectorField(
                label = "从",
                value = fromUnit.name,
                options = category.units.map { it.name },
                onSelect = { name -> fromUnit = category.units.first { it.name == name } }
            )
            Spacer(Modifier.height(10.dp))
            SelectorField(
                label = "到",
                value = toUnit.name,
                options = category.units.map { it.name },
                onSelect = { name -> toUnit = category.units.first { it.name == name } }
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = { convert() }, modifier = Modifier.fillMaxWidth()) { Text("换算") }
        }

        ErrorText(error)

        if (result.isNotBlank()) {
            SectionCard(title = "结果") {
                Text(result, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                CopyButton(result)
            }
        }
    }
}
