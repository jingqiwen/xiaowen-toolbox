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
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.ResultText
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

private fun fmt2(v: Double): String = ComplexCalc.formatComplex(v, 0.0)

private val EQUATION_TYPES = listOf(
    "一元一次 ax+b=0",
    "一元二次 ax²+bx+c=0",
    "绝对值 |ax+b|=c",
    "分式 (ax+b)/(cx+d)=e",
    "根式 √(ax+b)=c",
    "指数 aˣ=b",
    "对数 logₐx=b",
    "二元一次方程组",
    "三元一次方程组",
    "一元三次 ax³+bx²+cx+d=0"
)

@Composable
fun EquationTool() {
    var mode by remember { mutableStateOf(EQUATION_TYPES.first()) }
    var a by remember { mutableStateOf("2") }
    var b by remember { mutableStateOf("-8") }
    var c by remember { mutableStateOf("1") }
    var d by remember { mutableStateOf("0") }
    var e by remember { mutableStateOf("1") }
    // 方程组
    var a2 by remember { mutableStateOf("1") }
    var b2 by remember { mutableStateOf("1") }
    var c2 by remember { mutableStateOf("5") }
    var a3 by remember { mutableStateOf("1") }
    var b3 by remember { mutableStateOf("1") }
    var c3 by remember { mutableStateOf("1") }
    var d3 by remember { mutableStateOf("6") }
    var result by remember { mutableStateOf(listOf<String>()) }
    var error by remember { mutableStateOf("") }

    // 系数支持表达式：π / pi / e、sqrt(2)、3/2、2^3 等
    fun d2(s: String): Double? {
        val t = s.trim()
        if (t.isEmpty()) return null
        val r = CalcExpr.evaluate(t)
        return if (r.ok) r.value else null
    }

    fun solve() {
        error = ""
        result = emptyList()
        when (mode) {
            "一元一次 ax+b=0" -> {
                val av = d2(a) ?: run { error = "a 无效"; return }
                val bv = d2(b) ?: run { error = "b 无效"; return }
                if (av == 0.0) {
                    error = if (bv == 0.0) "a=0 且 b=0：方程有无穷多解" else "a=0 且 b≠0：方程无解"
                } else {
                    result = listOf("移项：${fmt2(av)}x = ${fmt2(-bv)}", "x = ${fmt2(-bv / av)}")
                }
            }
            "一元二次 ax²+bx+c=0" -> {
                val av = d2(a) ?: run { error = "a 无效"; return }
                val bv = d2(b) ?: run { error = "b 无效"; return }
                val cv = d2(c) ?: run { error = "c 无效"; return }
                if (av == 0.0) { error = "a 不能为 0（否则不是二次方程）"; return }
                val delta = bv * bv - 4 * av * cv
                if (delta >= 0) {
                    result = listOf(
                        "判别式 Δ = ${fmt2(delta)}",
                        "x₁ = ${fmt2((-bv + sqrt(delta)) / (2 * av))}",
                        "x₂ = ${fmt2((-bv - sqrt(delta)) / (2 * av))}"
                    )
                } else {
                    val re = -bv / (2 * av)
                    val im = sqrt(-delta) / (2 * av)
                    result = listOf(
                        "判别式 Δ = ${fmt2(delta)} < 0，有两个共轭复根",
                        "x₁ = ${ComplexCalc.formatComplex(re, im)}",
                        "x₂ = ${ComplexCalc.formatComplex(re, -im)}"
                    )
                }
            }
            "绝对值 |ax+b|=c" -> {
                val av = d2(a) ?: run { error = "a 无效"; return }
                val bv = d2(b) ?: run { error = "b 无效"; return }
                val cv = d2(c) ?: run { error = "c 无效"; return }
                if (av == 0.0) { error = "a 不能为 0"; return }
                when {
                    cv < 0 -> error = "绝对值不可能等于负数，方程无解"
                    cv == 0.0 -> result = listOf("ax+b=0", "x = ${fmt2(-bv / av)}")
                    else -> result = listOf(
                        "ax+b = ${fmt2(cv)} 或 ax+b = ${fmt2(-cv)}",
                        "x₁ = ${fmt2((cv - bv) / av)}",
                        "x₂ = ${fmt2((-cv - bv) / av)}"
                    )
                }
            }
            "分式 (ax+b)/(cx+d)=e" -> {
                val av = d2(a) ?: run { error = "a 无效"; return }
                val bv = d2(b) ?: run { error = "b 无效"; return }
                val cv = d2(c) ?: run { error = "c 无效"; return }
                val dv = d2(d) ?: run { error = "d 无效"; return }
                val ev = d2(e) ?: run { error = "e 无效"; return }
                val denom = av - ev * cv
                if (denom == 0.0) { error = "化简后 x 的系数为 0，需分情况讨论"; return }
                val x = (ev * dv - bv) / denom
                if (cv * x + dv == 0.0) {
                    error = "解得 x = ${fmt2(x)}，但会使分母为 0（增根），方程无解"
                } else {
                    result = listOf("去分母：ax+b = e(cx+d)", "x = ${fmt2(x)}", "检验：分母 ≠ 0，成立")
                }
            }
            "根式 √(ax+b)=c" -> {
                val av = d2(a) ?: run { error = "a 无效"; return }
                val bv = d2(b) ?: run { error = "b 无效"; return }
                val cv = d2(c) ?: run { error = "c 无效"; return }
                if (cv < 0) { error = "算术平方根不可能等于负数，方程无解"; return }
                if (av == 0.0) {
                    if (bv == cv * cv) result = listOf("0=0 恒成立，x 可取任意实数") else error = "无解"
                    return
                }
                val x = (cv * cv - bv) / av
                result = listOf("两边平方：ax+b = c²", "x = ${fmt2(x)}", "检验：被开方数 = ${fmt2(av * x + bv)} ≥ 0，成立")
            }
            "指数 aˣ=b" -> {
                val av = d2(a) ?: run { error = "底数 a 无效"; return }
                val bv = d2(b) ?: run { error = "b 无效"; return }
                if (av <= 0 || av == 1.0) { error = "底数 a 必须大于 0 且不等于 1"; return }
                if (bv <= 0) { error = "b 必须大于 0，否则无实数解"; return }
                val x = ln(bv) / ln(av)
                result = listOf("取自然对数：x·ln(${fmt2(av)}) = ln(${fmt2(bv)})", "x = ${fmt2(x)}")
            }
            "对数 logₐx=b" -> {
                val av = d2(a) ?: run { error = "底数 a 无效"; return }
                val bv = d2(b) ?: run { error = "b 无效"; return }
                if (av <= 0 || av == 1.0) { error = "底数 a 必须大于 0 且不等于 1"; return }
                val x = av.pow(bv)
                result = listOf("化为指数式：x = a^b", "x = ${fmt2(x)}", "检验：x > 0，成立")
            }
            "二元一次方程组" -> {
                val a1 = d2(a) ?: run { error = "a₁ 无效"; return }
                val b1 = d2(b) ?: run { error = "b₁ 无效"; return }
                val c1 = d2(c) ?: run { error = "c₁ 无效"; return }
                val a22 = d2(a2) ?: run { error = "a₂ 无效"; return }
                val b22 = d2(b2) ?: run { error = "b₂ 无效"; return }
                val c22 = d2(c2) ?: run { error = "c₂ 无效"; return }
                val det = a1 * b22 - a22 * b1
                val eq1 = "${fmt2(a1)}x + ${fmt2(b1)}y = ${fmt2(c1)}"
                val eq2 = "${fmt2(a22)}x + ${fmt2(b22)}y = ${fmt2(c22)}"
                if (det == 0.0) {
                    // 用系数与常数项的成比例关系判断“无解”还是“无穷多解”
                    val consistent = (a1 * c22 - a22 * c1 == 0.0) && (b1 * c22 - b22 * c1 == 0.0) && (a1 * b22 - a22 * b1 == 0.0)
                    result = listOf(
                        "方程组：", eq1, eq2,
                        if (consistent) "系数行列式为 0，且两方程相容 → 有无穷多组解（两直线重合）"
                        else "系数行列式为 0，两方程矛盾 → 无解（两直线平行）"
                    )
                    return
                }
                val x = (c1 * b22 - c22 * b1) / det
                val y = (a1 * c22 - a22 * c1) / det
                result = listOf(
                    "方程组：", eq1, eq2,
                    "系数行列式 D = ${fmt2(det)} ≠ 0，有唯一解",
                    "克莱姆法则：",
                    "x = ${fmt2(x)}",
                    "y = ${fmt2(y)}"
                )
            }
            "三元一次方程组" -> {
                val a1 = d2(a) ?: run { error = "a₁ 无效"; return }
                val b1 = d2(b) ?: run { error = "b₁ 无效"; return }
                val c1 = d2(c) ?: run { error = "c₁ 无效"; return }
                val d1 = d2(d) ?: run { error = "d₁ 无效"; return }
                val a22 = d2(a2) ?: run { error = "a₂ 无效"; return }
                val b22 = d2(b2) ?: run { error = "b₂ 无效"; return }
                val c22 = d2(c2) ?: run { error = "c₂ 无效"; return }
                val d22 = d2(d3) ?: run { error = "d₂ 无效"; return }
                val a33 = d2(a3) ?: run { error = "a₃ 无效"; return }
                val b33 = d2(b3) ?: run { error = "b₃ 无效"; return }
                val c33 = d2(c3) ?: run { error = "c₃ 无效"; return }
                val d33 = d2(e) ?: run { error = "d₃ 无效"; return }
                val det = a1 * (b22 * c33 - b33 * c22) - b1 * (a22 * c33 - a33 * c22) + c1 * (a22 * b33 - a33 * b22)
                if (det == 0.0) { error = "系数行列式为 0：可能无解或有无穷多解"; return }
                val dx = d1 * (b22 * c33 - b33 * c22) - b1 * (d22 * c33 - d33 * c22) + c1 * (d22 * b33 - d33 * b22)
                val dy = a1 * (d22 * c33 - d33 * c22) - d1 * (a22 * c33 - a33 * c22) + c1 * (a22 * d33 - a33 * d22)
                val dz = a1 * (b22 * d33 - b33 * d22) - b1 * (a22 * d33 - a33 * d22) + d1 * (a22 * b33 - a33 * b22)
                result = listOf("克莱姆法则：", "x = ${fmt2(dx / det)}", "y = ${fmt2(dy / det)}", "z = ${fmt2(dz / det)}")
            }
            else -> {
                val av = d2(a) ?: run { error = "a 无效"; return }
                val bv = d2(b) ?: run { error = "b 无效"; return }
                val cv = d2(c) ?: run { error = "c 无效"; return }
                val dv = d2(d) ?: run { error = "d 无效"; return }
                if (av == 0.0) { error = "a 不能为 0"; return }
                val roots = solveCubic(av, bv, cv, dv)
                result = listOf("求出 ${roots.size} 个实根（近似值）：") + roots.mapIndexed { i, r -> "x${i + 1} ≈ ${fmt2(r)}" }
            }
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "方程类型（10 种）") {
            ChoiceChips(EQUATION_TYPES, mode, { mode = it }, { it })
        }

        SectionCard(title = "输入系数") {
            when (mode) {
                "一元一次 ax+b=0", "一元二次 ax²+bx+c=0", "绝对值 |ax+b|=c",
                "根式 √(ax+b)=c", "指数 aˣ=b" -> {
                    LabeledField(a, { a = it }, if (mode.startsWith("指数")) "底数 a" else "a", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(8.dp))
                    LabeledField(b, { b = it }, if (mode.startsWith("指数")) "b（aˣ=b）" else "b", keyboardType = KeyboardType.Decimal)
                    if (mode == "一元二次 ax²+bx+c=0") {
                        Spacer(Modifier.height(8.dp))
                        LabeledField(c, { c = it }, "c", keyboardType = KeyboardType.Decimal)
                    }
                    if (mode.startsWith("绝对值") || mode.startsWith("根式")) {
                        Spacer(Modifier.height(8.dp))
                        LabeledField(c, { c = it }, "c（右边常数）", keyboardType = KeyboardType.Decimal)
                    }
                }
                "分式 (ax+b)/(cx+d)=e" -> {
                    LabeledField(a, { a = it }, "a", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(8.dp))
                    LabeledField(b, { b = it }, "b", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(8.dp))
                    LabeledField(c, { c = it }, "c", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(8.dp))
                    LabeledField(d, { d = it }, "d", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(8.dp))
                    LabeledField(e, { e = it }, "e", keyboardType = KeyboardType.Decimal)
                }
                "对数 logₐx=b" -> {
                    LabeledField(a, { a = it }, "底数 a", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(8.dp))
                    LabeledField(b, { b = it }, "b（logₐx=b）", keyboardType = KeyboardType.Decimal)
                }
                "二元一次方程组" -> {
                    Text("方程组：a₁x + b₁y = c₁，a₂x + b₂y = c₂", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    LabeledField(a, { a = it }, "a₁（x 的系数）", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(6.dp))
                    LabeledField(b, { b = it }, "b₁（y 的系数）", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(6.dp))
                    LabeledField(c, { c = it }, "c₁（等号右边常数）", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(10.dp))
                    LabeledField(a2, { a2 = it }, "a₂（x 的系数）", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(6.dp))
                    LabeledField(b2, { b2 = it }, "b₂（y 的系数）", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(6.dp))
                    LabeledField(c2, { c2 = it }, "c₂（等号右边常数）", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "当前方程：\n${a.ifBlank { "a₁" }}x + ${b.ifBlank { "b₁" }}y = ${c.ifBlank { "c₁" }}\n" +
                            "${a2.ifBlank { "a₂" }}x + ${b2.ifBlank { "b₂" }}y = ${c2.ifBlank { "c₂" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                "三元一次方程组" -> {
                    Text("方程 1：a₁x + b₁y + c₁z = d₁", style = MaterialTheme.typography.bodyMedium)
                    LabeledField(a, { a = it }, "a₁", keyboardType = KeyboardType.Decimal)
                    LabeledField(b, { b = it }, "b₁", keyboardType = KeyboardType.Decimal)
                    LabeledField(c, { c = it }, "c₁", keyboardType = KeyboardType.Decimal)
                    LabeledField(d, { d = it }, "d₁", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(6.dp))
                    Text("方程 2：a₂x + b₂y + c₂z = d₂", style = MaterialTheme.typography.bodyMedium)
                    LabeledField(a2, { a2 = it }, "a₂", keyboardType = KeyboardType.Decimal)
                    LabeledField(b2, { b2 = it }, "b₂", keyboardType = KeyboardType.Decimal)
                    LabeledField(c2, { c2 = it }, "c₂", keyboardType = KeyboardType.Decimal)
                    LabeledField(d3, { d3 = it }, "d₂", keyboardType = KeyboardType.Decimal)
                    Spacer(Modifier.height(6.dp))
                    Text("方程 3：a₃x + b₃y + c₃z = d₃", style = MaterialTheme.typography.bodyMedium)
                    LabeledField(a3, { a3 = it }, "a₃", keyboardType = KeyboardType.Decimal)
                    LabeledField(b3, { b3 = it }, "b₃", keyboardType = KeyboardType.Decimal)
                    LabeledField(c3, { c3 = it }, "c₃", keyboardType = KeyboardType.Decimal)
                    LabeledField(e, { e = it }, "d₃", keyboardType = KeyboardType.Decimal)
                }
                else -> {
                    LabeledField(a, { a = it }, "a（三次项系数）", keyboardType = KeyboardType.Decimal)
                    LabeledField(b, { b = it }, "b（二次项系数）", keyboardType = KeyboardType.Decimal)
                    LabeledField(c, { c = it }, "c（一次项系数）", keyboardType = KeyboardType.Decimal)
                    LabeledField(d, { d = it }, "d（常数项）", keyboardType = KeyboardType.Decimal)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "系数可输入表达式：π、pi、e、sqrt(2)、2/3、2^3 等",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = { solve() }, modifier = Modifier.fillMaxWidth()) { Text("求解") }
        }

        ErrorText(error)
        if (result.isNotEmpty()) {
            SectionCard(title = "解与过程") {
                result.forEach { line ->
                    Text(line, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                }
            }
        }

        SectionCard(title = "📘 方程知识点") {
            Text(
                "• 一元一次：ax+b=0，a≠0 时 x=-b/a。\n" +
                    "• 一元二次：求根公式与判别式 Δ=b²-4ac。\n" +
                    "• 绝对值方程：|A|=c 分 c<0（无解）、c=0、c>0 三种情况。\n" +
                    "• 分式方程：去分母后必须检验，防止分母为 0 的增根。\n" +
                    "• 根式方程：两边平方后必须检验被开方数 ≥ 0。\n" +
                    "• 指数/对数方程：注意底数范围（a>0 且 a≠1）与真数 >0。\n" +
                    "• 线性方程组：克莱姆法则（行列式不为 0 时有唯一解）。\n" +
                    "• 三次方程：本工具用卡尔达诺公式给出实根近似值。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 卡尔达诺公式求一元三次方程 ax³+bx²+cx+d=0 的实根。 */
private fun solveCubic(a: Double, b: Double, c: Double, d: Double): List<Double> {
    val b1 = b / a
    val c1 = c / a
    val d1 = d / a
    val p = c1 - b1 * b1 / 3.0
    val q = 2 * b1 * b1 * b1 / 27.0 - b1 * c1 / 3.0 + d1
    val delta = q * q / 4.0 + p * p * p / 27.0
    val shift = -b1 / 3.0
    return when {
        delta > 1e-12 -> {
            val sqrtDelta = sqrt(delta)
            val u = Math.cbrt(-q / 2.0 + sqrtDelta)
            val v = Math.cbrt(-q / 2.0 - sqrtDelta)
            listOf(u + v + shift)
        }
        delta > -1e-12 -> {
            val u = Math.cbrt(-q / 2.0)
            val roots = linkedSetOf<Double>()
            roots.add(2 * u + shift)
            roots.add(-u + shift)
            roots.toList()
        }
        else -> {
            val r = sqrt(-p * p * p / 27.0)
            val phi = acos((-q / 2.0) / r)
            val m = 2.0 * sqrt(-p / 3.0)
            listOf(
                m * cos(phi / 3.0) + shift,
                m * cos((phi + 2 * PI) / 3.0) + shift,
                m * cos((phi + 4 * PI) / 3.0) + shift
            )
        }
    }
}
