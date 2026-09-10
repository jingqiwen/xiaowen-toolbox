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
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.InfoRow
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

private val SHAPES = listOf(
    "圆", "扇形", "椭圆", "三角形", "直角三角形", "矩形", "平行四边形", "菱形",
    "梯形", "正多边形", "长方体", "正方体", "圆柱", "圆锥", "圆台", "球", "棱柱", "棱锥"
)

private fun f2(v: Double): String = try {
    java.math.BigDecimal(v).setScale(6, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
} catch (_: Exception) {
    v.toString()
}

@Composable
fun GeometryProTool() {
    var shape by remember { mutableStateOf("圆") }
    var a by remember { mutableStateOf("5") }
    var b by remember { mutableStateOf("4") }
    var c by remember { mutableStateOf("3") }
    var d by remember { mutableStateOf("2") }
    var h by remember { mutableStateOf("6") }
    var angle by remember { mutableStateOf("60") }
    var sides by remember { mutableStateOf("6") }
    var result by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var error by remember { mutableStateOf("") }

    fun num(text: String) = text.trim().toDoubleOrNull()

    fun calc() {
        error = ""
        result = emptyList()
        try {
            when (shape) {
                "圆" -> {
                    val r = num(a) ?: throw IllegalArgumentException("半径无效")
                    result = listOf(
                        "周长" to f2(2 * PI * r),
                        "面积" to f2(PI * r * r),
                        "直径" to f2(2 * r)
                    )
                }
                "扇形" -> {
                    val r = num(a) ?: throw IllegalArgumentException("半径无效")
                    val n = num(angle) ?: throw IllegalArgumentException("圆心角无效")
                    if (n <= 0 || n > 360) throw IllegalArgumentException("圆心角需在 0~360°")
                    val arc = 2 * PI * r * n / 360.0
                    result = listOf(
                        "弧长" to f2(arc),
                        "扇形面积" to f2(0.5 * arc * r),
                        "周长（含两条半径）" to f2(arc + 2 * r)
                    )
                }
                "椭圆" -> {
                    val x = num(a) ?: throw IllegalArgumentException("半长轴无效")
                    val y = num(b) ?: throw IllegalArgumentException("半短轴无效")
                    val perimeter = PI * (3 * (x + y) - sqrt((3 * x + y) * (x + 3 * y)))
                    result = listOf("面积" to f2(PI * x * y), "周长（近似）" to f2(perimeter))
                }
                "三角形" -> {
                    val x = num(a) ?: throw IllegalArgumentException("边 a 无效")
                    val y = num(b) ?: throw IllegalArgumentException("边 b 无效")
                    val z = num(c) ?: throw IllegalArgumentException("边 c 无效")
                    if (x + y <= z || x + z <= y || y + z <= x) throw IllegalArgumentException("三边无法构成三角形")
                    val p = (x + y + z) / 2
                    val area = sqrt(p * (p - x) * (p - y) * (p - z))
                    val inR = area / p
                    val outR = x * y * z / (4 * area)
                    result = listOf(
                        "周长" to f2(2 * p),
                        "面积（海伦公式）" to f2(area),
                        "内切圆半径 r = S/p" to f2(inR),
                        "外接圆半径 R = abc/4S" to f2(outR),
                        "半周长 p" to f2(p)
                    )
                }
                "直角三角形" -> {
                    val x = num(a) ?: throw IllegalArgumentException("直角边 a 无效")
                    val y = num(b) ?: throw IllegalArgumentException("直角边 b 无效")
                    val z = sqrt(x * x + y * y)
                    result = listOf(
                        "斜边 c" to f2(z),
                        "周长" to f2(x + y + z),
                        "面积" to f2(0.5 * x * y),
                        "内切圆半径 r = (a+b-c)/2" to f2((x + y - z) / 2),
                        "外接圆半径 R = c/2" to f2(z / 2)
                    )
                }
                "矩形" -> {
                    val x = num(a) ?: throw IllegalArgumentException("长无效")
                    val y = num(b) ?: throw IllegalArgumentException("宽无效")
                    result = listOf("周长" to f2(2 * (x + y)), "面积" to f2(x * y), "对角线" to f2(sqrt(x * x + y * y)))
                }
                "平行四边形" -> {
                    val base = num(a) ?: throw IllegalArgumentException("底边无效")
                    val high = num(h) ?: throw IllegalArgumentException("高无效")
                    val side = num(b) ?: throw IllegalArgumentException("斜边无效")
                    result = listOf("面积 = 底×高" to f2(base * high), "周长 = 2(底+斜边)" to f2(2 * (base + side)))
                }
                "菱形" -> {
                    val d1 = num(a) ?: throw IllegalArgumentException("对角线 1 无效")
                    val d2 = num(b) ?: throw IllegalArgumentException("对角线 2 无效")
                    val side = sqrt((d1 / 2) * (d1 / 2) + (d2 / 2) * (d2 / 2))
                    result = listOf("面积 = d₁d₂/2" to f2(d1 * d2 / 2), "边长" to f2(side), "周长" to f2(4 * side))
                }
                "梯形" -> {
                    val top = num(a) ?: throw IllegalArgumentException("上底无效")
                    val bottom = num(b) ?: throw IllegalArgumentException("下底无效")
                    val high = num(h) ?: throw IllegalArgumentException("高无效")
                    val left = num(c) ?: 0.0
                    val right = num(d) ?: 0.0
                    result = listOf(
                        "面积 = (上底+下底)×高÷2" to f2((top + bottom) * high / 2),
                        "周长（需两腰）" to f2(top + bottom + left + right)
                    )
                }
                "正多边形" -> {
                    val n = sides.trim().toIntOrNull() ?: throw IllegalArgumentException("边数无效")
                    val side = num(a) ?: throw IllegalArgumentException("边长无效")
                    if (n < 3) throw IllegalArgumentException("边数至少为 3")
                    val area = n * side * side / (4 * tan(PI / n))
                    val inR = side / (2 * tan(PI / n))
                    val outR = side / (2 * sin(PI / n))
                    result = listOf(
                        "内角和" to "${(n - 2) * 180}°",
                        "周长" to f2(n * side),
                        "面积" to f2(area),
                        "内切圆半径" to f2(inR),
                        "外接圆半径" to f2(outR)
                    )
                }
                "长方体" -> {
                    val x = num(a) ?: throw IllegalArgumentException("长无效")
                    val y = num(b) ?: throw IllegalArgumentException("宽无效")
                    val z = num(c) ?: throw IllegalArgumentException("高无效")
                    result = listOf(
                        "体积" to f2(x * y * z),
                        "表面积" to f2(2 * (x * y + y * z + x * z)),
                        "体对角线" to f2(sqrt(x * x + y * y + z * z))
                    )
                }
                "正方体" -> {
                    val x = num(a) ?: throw IllegalArgumentException("棱长无效")
                    result = listOf("体积" to f2(x * x * x), "表面积" to f2(6 * x * x), "体对角线" to f2(sqrt(3.0) * x))
                }
                "圆柱" -> {
                    val r = num(a) ?: throw IllegalArgumentException("底面半径无效")
                    val high = num(h) ?: throw IllegalArgumentException("高无效")
                    result = listOf(
                        "体积" to f2(PI * r * r * high),
                        "侧面积" to f2(2 * PI * r * high),
                        "表面积（含底）" to f2(2 * PI * r * r + 2 * PI * r * high)
                    )
                }
                "圆锥" -> {
                    val r = num(a) ?: throw IllegalArgumentException("底面半径无效")
                    val high = num(h) ?: throw IllegalArgumentException("高无效")
                    val l = sqrt(r * r + high * high)
                    result = listOf(
                        "母线 l" to f2(l),
                        "体积 = πr²h/3" to f2(PI * r * r * high / 3),
                        "侧面积 = πrl" to f2(PI * r * l),
                        "表面积（含底）" to f2(PI * r * l + PI * r * r)
                    )
                }
                "圆台" -> {
                    val R = num(a) ?: throw IllegalArgumentException("下底半径无效")
                    val r = num(b) ?: throw IllegalArgumentException("上底半径无效")
                    val high = num(h) ?: throw IllegalArgumentException("高无效")
                    result = listOf(
                        "体积" to f2(PI * high * (R * R + R * r + r * r) / 3),
                        "侧面积（近似母线）" to f2(PI * (R + r) * sqrt((R - r) * (R - r) + high * high))
                    )
                }
                "球" -> {
                    val r = num(a) ?: throw IllegalArgumentException("半径无效")
                    result = listOf("体积 = 4πr³/3" to f2(4.0 / 3.0 * PI * r * r * r), "表面积 = 4πr²" to f2(4 * PI * r * r))
                }
                "棱柱" -> {
                    val s = num(a) ?: throw IllegalArgumentException("底面积无效")
                    val high = num(h) ?: throw IllegalArgumentException("高无效")
                    result = listOf("体积 = 底面积×高" to f2(s * high))
                }
                "棱锥" -> {
                    val s = num(a) ?: throw IllegalArgumentException("底面积无效")
                    val high = num(h) ?: throw IllegalArgumentException("高无效")
                    result = listOf("体积 = 底面积×高÷3" to f2(s * high / 3))
                }
            }
        } catch (e: Exception) {
            error = e.message ?: "输入错误"
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "选择图形（18 种）") {
            ChoiceChips(SHAPES, shape, { shape = it }, { it })
        }
        SectionCard(title = "输入参数") {
            when (shape) {
                "圆", "球", "正方体" -> LabeledField(a, { a = it }, if (shape == "圆" || shape == "球") "半径 r" else "棱长 a", keyboardType = KeyboardType.Decimal)
                "扇形" -> {
                    LabeledField(a, { a = it }, "半径 r", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(8.dp))
                    LabeledField(angle, { angle = it }, "圆心角 n（度）", keyboardType = KeyboardType.Decimal)
                }
                "椭圆" -> {
                    LabeledField(a, { a = it }, "半长轴 a", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(8.dp))
                    LabeledField(b, { b = it }, "半短轴 b", keyboardType = KeyboardType.Decimal)
                }
                "三角形" -> {
                    LabeledField(a, { a = it }, "边 a", keyboardType = KeyboardType.Decimal)
                    LabeledField(b, { b = it }, "边 b", keyboardType = KeyboardType.Decimal)
                    LabeledField(c, { c = it }, "边 c", keyboardType = KeyboardType.Decimal)
                }
                "直角三角形", "矩形" -> {
                    LabeledField(a, { a = it }, "边 a", keyboardType = KeyboardType.Decimal)
                    LabeledField(b, { b = it }, "边 b", keyboardType = KeyboardType.Decimal)
                }
                "平行四边形" -> {
                    LabeledField(a, { a = it }, "底边", keyboardType = KeyboardType.Decimal)
                    LabeledField(h, { h = it }, "高", keyboardType = KeyboardType.Decimal)
                    LabeledField(b, { b = it }, "斜边", keyboardType = KeyboardType.Decimal)
                }
                "菱形" -> {
                    LabeledField(a, { a = it }, "对角线 d₁", keyboardType = KeyboardType.Decimal)
                    LabeledField(b, { b = it }, "对角线 d₂", keyboardType = KeyboardType.Decimal)
                }
                "梯形" -> {
                    LabeledField(a, { a = it }, "上底", keyboardType = KeyboardType.Decimal)
                    LabeledField(b, { b = it }, "下底", keyboardType = KeyboardType.Decimal)
                    LabeledField(h, { h = it }, "高", keyboardType = KeyboardType.Decimal)
                    LabeledField(c, { c = it }, "左腰（可空）", keyboardType = KeyboardType.Decimal)
                    LabeledField(d, { d = it }, "右腰（可空）", keyboardType = KeyboardType.Decimal)
                }
                "正多边形" -> {
                    LabeledField(sides, { sides = it }, "边数 n", keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(8.dp))
                    LabeledField(a, { a = it }, "边长 a", keyboardType = KeyboardType.Decimal)
                }
                "长方体" -> {
                    LabeledField(a, { a = it }, "长", keyboardType = KeyboardType.Decimal)
                    LabeledField(b, { b = it }, "宽", keyboardType = KeyboardType.Decimal)
                    LabeledField(c, { c = it }, "高", keyboardType = KeyboardType.Decimal)
                }
                "圆柱", "圆锥" -> {
                    LabeledField(a, { a = it }, "底面半径 r", keyboardType = KeyboardType.Decimal)
                    LabeledField(h, { h = it }, "高 h", keyboardType = KeyboardType.Decimal)
                }
                "圆台" -> {
                    LabeledField(a, { a = it }, "下底半径 R", keyboardType = KeyboardType.Decimal)
                    LabeledField(b, { b = it }, "上底半径 r", keyboardType = KeyboardType.Decimal)
                    LabeledField(h, { h = it }, "高 h", keyboardType = KeyboardType.Decimal)
                }
                "棱柱", "棱锥" -> {
                    LabeledField(a, { a = it }, "底面积 S", keyboardType = KeyboardType.Decimal)
                    LabeledField(h, { h = it }, "高 h", keyboardType = KeyboardType.Decimal)
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { calc() }, modifier = Modifier.fillMaxWidth()) { Text("计算") }
        }
        ErrorText(error)
        if (result.isNotEmpty()) {
            SectionCard(title = "结果") {
                result.forEach { (label, value) -> InfoRow("$label：", value) }
            }
        }
        SectionCard(title = "📘 常用几何公式") {
            Text(
                "• 圆：C=2πr，S=πr²\n" +
                    "• 扇形：l=rθ，S=½lr\n" +
                    "• 球：V=4πr³/3，S=4πr²\n" +
                    "• 圆柱：V=πr²h，S侧=2πrh\n" +
                    "• 圆锥：V=πr²h/3，S侧=πrl（l 为母线）\n" +
                    "• 圆台：V=πh(R²+Rr+r²)/3\n" +
                    "• 三角形：S=√[p(p-a)(p-b)(p-c)]，内切圆 r=S/p，外接圆 R=abc/(4S)\n" +
                    "• 正多边形：S=n·a²/(4tan(π/n))，内切圆 r=a/(2tan(π/n))",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
