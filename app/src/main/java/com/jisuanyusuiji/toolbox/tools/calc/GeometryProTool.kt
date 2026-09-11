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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.CopyButton
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.InfoRow
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.MathText
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

private val SHAPES = listOf(
    "圆", "扇形", "椭圆", "三角形", "直角三角形", "矩形", "平行四边形", "菱形",
    "梯形", "正多边形", "长方体", "正方体", "圆柱", "圆锥", "圆台", "球", "棱柱", "棱锥"
)

/** 常用几何公式速查：每条公式都配“每个字母什么意思”。 */
private data class GeoFormula(val name: String, val expr: String, val legend: String)

private val GEO_FORMULAS = listOf(
    GeoFormula(
        "圆", "C = 2πr，S = πr^2",
        "r：半径；π：圆周率（≈3.14159）；C：周长；S：面积"
    ),
    GeoFormula(
        "扇形", "l = rθ，S = 1/2lr = 1/2r^2θ",
        "l：弧长；r：半径；θ：圆心角（用弧度表示，1 弧度≈57.3°）；S：扇形面积"
    ),
    GeoFormula(
        "椭圆", "S = πab，L ≈ π[3(a+b) - √((3a+b)(a+3b))]",
        "S：椭圆面积；L：周长（近似公式）；a：半长轴；b：半短轴；π：圆周率"
    ),
    GeoFormula(
        "三角形", "S = 1/2ab·sinC = √[p(p-a)(p-b)(p-c)] = abc/(4R) = rp",
        "a、b、c：三边长；C：边 a、b 的夹角；S：面积；p=(a+b+c)/2：半周长；R：外接圆半径；r：内切圆半径"
    ),
    GeoFormula(
        "球", "V = 4/3πr^3，S = 4πr^2",
        "V：球体积；S：球表面积；r：半径；π：圆周率"
    ),
    GeoFormula(
        "圆柱", "V = πr^2h，S侧 = 2πrh，S表 = 2πr^2 + 2πrh，外接球 R = √(r^2 + h^2/4)",
        "V：体积；S侧：侧面积；S表：表面积；r：底面半径；h：高；R：外接球半径；π：圆周率"
    ),
    GeoFormula(
        "圆锥", "V = 1/3πr^2h，S侧 = πrl，外接球 R = (r^2+h^2)/(2h)，内切球 r内 = rh/(r+l)",
        "V：体积；S侧：侧面积；r：底面半径；h：高；l：母线长；R：外接球半径；r内：内切球半径"
    ),
    GeoFormula(
        "圆台", "V = 1/3πh(R^2+Rr+r^2)，S侧 = π(R+r)l",
        "V：体积；S侧：侧面积；R：下底半径；r：上底半径；h：高；l：母线长；π：圆周率"
    ),
    GeoFormula(
        "长方体", "V = abc，S = 2(ab+bc+ca)，体对角线 d = √(a^2+b^2+c^2)，外接球 R = d/2",
        "V：体积；S：表面积；a、b、c：长、宽、高；d：体对角线；R：外接球半径"
    ),
    GeoFormula(
        "正方体", "S = 6a^2，V = a^3，外接球 R = √3a/2，内切球 r = a/2",
        "S：表面积；V：体积；a：棱长；R：外接球半径；r：内切球半径"
    ),
    GeoFormula(
        "棱柱 / 棱锥", "棱柱 V = S_底·h；棱锥 V = 1/3·S_底·h",
        "V：体积；S_底：底面积；h：高"
    ),
    GeoFormula(
        "正多边形", "周长 C = na，面积 S = 1/2nar（r 为边心距，r = a/(2tan(π/n))）",
        "C：周长；S：面积；n：边数；a：边长；r：边心距（内切圆半径）；π：圆周率"
    )
)

private fun f2(v: Double): String = try {
    java.math.BigDecimal(v).setScale(6, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
} catch (_: Exception) {
    v.toString()
}

private data class GField(val key: String, val label: String)

private fun fieldsOf(shape: String): List<GField> = when (shape) {
    "圆" -> listOf(GField("r", "半径 r"), GField("d", "直径 d"), GField("C", "周长 C"), GField("S", "面积 S"))
    "扇形" -> listOf(GField("r", "半径 r"), GField("n", "圆心角 n（度）"), GField("l", "弧长 l"), GField("S", "面积 S"))
    "椭圆" -> listOf(GField("a", "半长轴 a"), GField("b", "半短轴 b"), GField("S", "面积 S"))
    "三角形" -> listOf(
        GField("a", "边 a"), GField("b", "边 b"), GField("c", "边 c"),
        GField("base", "底边"), GField("height", "高"), GField("angleC", "a、b 夹角（度）")
    )
    "直角三角形" -> listOf(GField("a", "直角边 a"), GField("b", "直角边 b"), GField("c", "斜边 c"))
    "矩形" -> listOf(GField("a", "长 a"), GField("b", "宽 b"), GField("S", "面积 S"), GField("C", "周长 C"), GField("d", "对角线 d"))
    "平行四边形" -> listOf(GField("base", "底边"), GField("height", "高"), GField("side", "斜边/侧边"))
    "菱形" -> listOf(GField("d1", "对角线 d₁"), GField("d2", "对角线 d₂"), GField("side", "边长"), GField("angle", "顶角（度）"))
    "梯形" -> listOf(
        GField("top", "上底"), GField("bottom", "下底"), GField("height", "高"),
        GField("left", "左腰（可空）"), GField("right", "右腰（可空）")
    )
    "正多边形" -> listOf(GField("n", "边数 n"), GField("side", "边长 a"), GField("S", "面积 S"), GField("outR", "外接圆半径 R"))
    "长方体" -> listOf(
        GField("a", "长 a"), GField("b", "宽 b"), GField("c", "高 c"),
        GField("S底", "底面积 S底"), GField("S表", "表面积 S表"), GField("V", "体积 V"), GField("d", "体对角线 d")
    )
    "正方体" -> listOf(GField("a", "棱长 a"), GField("S表", "表面积 S表"), GField("V", "体积 V"), GField("d", "体对角线 d"))
    "圆柱" -> listOf(
        GField("r", "底面半径 r"), GField("d", "底面直径 d"), GField("h", "高 h"),
        GField("S底", "底面积 S底"), GField("C底", "底面周长 C底"),
        GField("S侧", "侧面积 S侧"), GField("S表", "表面积 S表"), GField("V", "体积 V")
    )
    "圆锥" -> listOf(
        GField("r", "底面半径 r"), GField("h", "高 h"), GField("l", "母线 l"),
        GField("S底", "底面积 S底"), GField("S侧", "侧面积 S侧"), GField("S表", "表面积 S表"), GField("V", "体积 V")
    )
    "圆台" -> listOf(
        GField("R", "下底半径 R"), GField("r", "上底半径 r"), GField("h", "高 h"), GField("l", "母线 l"),
        GField("S侧", "侧面积 S侧"), GField("S表", "表面积 S表"), GField("V", "体积 V")
    )
    "球" -> listOf(GField("r", "半径 r"), GField("d", "直径 d"), GField("S", "表面积 S"), GField("V", "体积 V"))
    "棱柱" -> listOf(GField("S底", "底面积 S底"), GField("C底", "底面周长 C底"), GField("h", "高 h"), GField("V", "体积 V"))
    else -> listOf(GField("S底", "底面积 S底"), GField("C底", "底面周长 C底"), GField("h", "高 h"), GField("V", "体积 V"))
}

/** 逐步代入求解器：只记录正数，已存在的键不再覆盖。 */
private class GS {
    val m = LinkedHashMap<String, Double>()
    fun get(k: String) = m[k]
    fun set(k: String, value: Double?) {
        if (m.containsKey(k)) return
        if (value != null && value.isFinite() && value > 0) m[k] = value
    }
}

private fun derive(s: GS, block: GS.() -> Unit) {
    var changed = true
    var guard = 0
    while (changed && guard++ < 20) {
        val before = s.m.size
        s.block()
        changed = s.m.size > before
    }
}

private fun out(s: GS, given: Set<String>, key: String, label: String): Pair<String, String>? {
    val v = s.get(key) ?: return null
    val tag = if (key in given) "" else "（自动求得）"
    return "$label$tag" to f2(v)
}

/** 解算：返回“结果列表”或错误信息。 */
private fun solveShape(shape: String, input: Map<String, Double>): Pair<List<Pair<String, String>>?, String?> {
    val given = input.keys
    val s = GS()
    input.forEach { (k, v) -> s.set(k, v) }
    val res = mutableListOf<Pair<String, String>>()

    fun addAll(vararg items: Pair<String, String>?) {
        items.forEach { if (it != null) res.add(it) }
    }

    when (shape) {
        "圆" -> {
            derive(s) {
                set("d", get("r")?.times(2)); set("r", get("d")?.div(2))
                set("C", get("r")?.times(2 * PI)); set("r", get("C")?.div(2 * PI))
                set("S", get("r")?.let { PI * it * it }); set("r", get("S")?.let { sqrt(it / PI) })
            }
            if (s.get("r") == null) return null to "请至少填写半径、直径、周长或面积中的任意一个"
            addAll(out(s, given, "r", "半径 r"), out(s, given, "d", "直径 d"), out(s, given, "C", "周长 C"), out(s, given, "S", "面积 S"))
        }
        "扇形" -> {
            derive(s) {
                set("l", get("r")?.let { r -> get("n")?.let { n -> 2 * PI * r * n / 360.0 } })
                set("n", get("r")?.let { r -> get("l")?.let { l -> l * 360.0 / (2 * PI * r) } })
                set("S", get("l")?.let { l -> get("r")?.let { r -> 0.5 * l * r } })
                set("l", get("S")?.let { a -> get("r")?.let { r -> 2 * a / r } })
                set("r", get("S")?.let { a -> get("l")?.let { l -> 2 * a / l } })
                set("S", get("r")?.let { r -> get("n")?.let { n -> PI * r * r * n / 360.0 } })
                set("n", get("r")?.let { r -> get("S")?.let { a -> a * 360.0 / (PI * r * r) } })
            }
            if (s.get("S") == null || s.get("l") == null) return null to "请填写半径与圆心角（或弧长/面积）中的足够条件"
            addAll(
                out(s, given, "r", "半径 r"), out(s, given, "n", "圆心角 n"),
                out(s, given, "l", "弧长 l"), out(s, given, "S", "扇形面积 S"),
                "周长（弧长+两条半径）" to f2((s.get("l") ?: 0.0) + 2 * (s.get("r") ?: 0.0))
            )
        }
        "椭圆" -> {
            derive(s) {
                set("S", get("a")?.let { a -> get("b")?.let { b -> PI * a * b } })
                set("b", get("a")?.let { a -> get("S")?.let { S -> S / (PI * a) } })
                set("a", get("b")?.let { b -> get("S")?.let { S -> S / (PI * b) } })
            }
            if (s.get("a") == null || s.get("b") == null) return null to "请至少填写半长轴与半短轴（或面积+其中一个）"
            val a = s.get("a")!!; val b = s.get("b")!!
            val perimeter = PI * (3 * (a + b) - sqrt((3 * a + b) * (a + 3 * b)))
            addAll(
                out(s, given, "a", "半长轴 a"), out(s, given, "b", "半短轴 b"),
                out(s, given, "S", "面积 S"), "周长（近似）" to f2(perimeter),
                "离心率 e" to f2(sqrt(1 - minOf(a, b).pow(2) / maxOf(a, b).pow(2))),
                "焦距 c" to f2(sqrt(abs(a * a - b * b)))
            )
        }
        "三角形" -> {
            derive(s) {
                set("p", get("a")?.let { a -> get("b")?.let { b -> get("c")?.let { c -> (a + b + c) / 2 } } })
                set("S", (get("base") ?: get("c"))?.let { base -> get("height")?.let { h -> 0.5 * base * h } })
                set("c", get("a")?.let { a -> get("b")?.let { b -> get("angleC")?.let { ang -> sqrt(a * a + b * b - 2 * a * b * cos(Math.toRadians(ang))) } } })
                set("height", (get("base") ?: get("c"))?.let { base -> get("S")?.let { area -> 2 * area / base } })
                set("angleC", get("a")?.let { a -> get("b")?.let { b -> get("c")?.let { c ->
                    Math.toDegrees(acos(((a * a + b * b - c * c) / (2 * a * b)).coerceIn(-1.0, 1.0)))
                } } })
                set("S", get("p")?.let { p -> get("a")?.let { a -> get("b")?.let { b -> get("c")?.let { c -> sqrt(p * (p - a) * (p - b) * (p - c)) } } } })
                set("inR", get("S")?.let { area -> get("p")?.let { p -> area / p } })
                set("outR", get("a")?.let { a -> get("b")?.let { b -> get("c")?.let { c -> get("S")?.let { area -> a * b * c / (4 * area) } } } })
            }
            val need = listOf("a", "b", "c").all { s.get(it) != null }
            if (!need && s.get("S") == null) return null to "请填三边，或底边+高，或两边+夹角"
            addAll(
                out(s, given, "a", "边 a"), out(s, given, "b", "边 b"), out(s, given, "c", "边 c"),
                out(s, given, "p", "半周长 p"), out(s, given, "S", "面积 S"),
                out(s, given, "height", "对应底边的高 h"),
                out(s, given, "inR", "内切圆半径 r"), out(s, given, "outR", "外接圆半径 R"),
                out(s, given, "angleC", "a、b 夹角（度）")
            )
        }
        "直角三角形" -> {
            derive(s) {
                set("c", get("a")?.let { a -> get("b")?.let { b -> sqrt(a * a + b * b) } })
                set("a", get("c")?.let { c -> get("b")?.let { b -> if (c > b) sqrt(c * c - b * b) else null } })
                set("b", get("c")?.let { c -> get("a")?.let { a -> if (c > a) sqrt(c * c - a * a) else null } })
                set("S", get("a")?.let { a -> get("b")?.let { b -> 0.5 * a * b } })
                set("inR", get("a")?.let { a -> get("b")?.let { b -> get("c")?.let { c -> (a + b - c) / 2 } } })
                set("outR", get("c")?.div(2))
            }
            if (s.get("c") == null) return null to "请至少填写两条边"
            addAll(
                out(s, given, "a", "直角边 a"), out(s, given, "b", "直角边 b"), out(s, given, "c", "斜边 c"),
                out(s, given, "S", "面积 S"), out(s, given, "inR", "内切圆半径 r"),
                out(s, given, "outR", "外接圆半径 R")
            )
        }
        "矩形" -> {
            derive(s) {
                set("S", get("a")?.let { a -> get("b")?.let { b -> a * b } })
                set("b", get("a")?.let { a -> get("S")?.let { S -> S / a } })
                set("a", get("b")?.let { b -> get("S")?.let { S -> S / b } })
                set("C", get("a")?.let { a -> get("b")?.let { b -> 2 * (a + b) } })
                set("a", get("b")?.let { b -> get("C")?.let { C -> C / 2 - b } })
                set("b", get("a")?.let { a -> get("C")?.let { C -> C / 2 - a } })
                set("d", get("a")?.let { a -> get("b")?.let { b -> sqrt(a * a + b * b) } })
                set("b", get("a")?.let { a -> get("d")?.let { d -> if (d > a) sqrt(d * d - a * a) else null } })
                set("a", get("b")?.let { b -> get("d")?.let { d -> if (d > b) sqrt(d * d - b * b) else null } })
            }
            if (s.get("a") == null || s.get("b") == null) return null to "请填写长、宽、面积、周长、对角线中的足够条件"
            addAll(
                out(s, given, "a", "长 a"), out(s, given, "b", "宽 b"), out(s, given, "S", "面积 S"),
                out(s, given, "C", "周长 C"), out(s, given, "d", "对角线 d"),
                "外接圆半径 R" to f2((s.get("d") ?: 0.0) / 2)
            )
        }
        "平行四边形" -> {
            derive(s) {
                set("S", get("base")?.let { b -> get("height")?.let { h -> b * h } })
                set("height", get("base")?.let { b -> get("S")?.let { S -> S / b } })
                set("base", get("height")?.let { h -> get("S")?.let { S -> S / h } })
                set("C", get("base")?.let { b -> get("side")?.let { a -> 2 * (a + b) } })
            }
            if (s.get("S") == null) return null to "请填写 底边+高（或面积+其中一个）"
            addAll(
                out(s, given, "base", "底边"), out(s, given, "height", "高"), out(s, given, "S", "面积 S"),
                out(s, given, "side", "侧边/斜边"), out(s, given, "C", "周长 C（需侧边）")
            )
        }
        "菱形" -> {
            derive(s) {
                set("S", get("d1")?.let { d1 -> get("d2")?.let { d2 -> d1 * d2 / 2 } })
                set("side", get("d1")?.let { d1 -> get("d2")?.let { d2 -> sqrt((d1 / 2).pow(2) + (d2 / 2).pow(2)) } })
                set("S", get("side")?.let { a -> get("angle")?.let { ang -> a * a * sin(Math.toRadians(ang)) } })
                set("C", get("side")?.times(4))
                set("d1", get("side")?.let { a -> get("angle")?.let { ang -> 2 * a * sin(Math.toRadians(ang) / 2) } })
                set("d2", get("side")?.let { a -> get("angle")?.let { ang -> 2 * a * cos(Math.toRadians(ang) / 2) } })
            }
            if (s.get("S") == null) return null to "请填写两条对角线，或边长+顶角"
            addAll(
                out(s, given, "d1", "对角线 d₁"), out(s, given, "d2", "对角线 d₂"), out(s, given, "side", "边长"),
                out(s, given, "C", "周长"), out(s, given, "S", "面积 S")
            )
        }
        "梯形" -> {
            derive(s) {
                set("S", get("top")?.let { t -> get("bottom")?.let { b -> get("height")?.let { h -> (t + b) * h / 2 } } })
                set("height", get("S")?.let { S -> get("top")?.let { t -> get("bottom")?.let { b -> if (t + b > 0) 2 * S / (t + b) else null } } })
                set("C", get("top")?.let { t -> get("bottom")?.let { b -> get("left")?.let { l -> get("right")?.let { r -> t + b + l + r } } } })
            }
            if (s.get("S") == null) return null to "请填写上底、下底与高（或面积+其中两个）"
            addAll(
                out(s, given, "top", "上底"), out(s, given, "bottom", "下底"), out(s, given, "height", "高"),
                out(s, given, "S", "面积 S"), out(s, given, "left", "左腰"), out(s, given, "right", "右腰"),
                out(s, given, "C", "周长 C（需两腰）")
            )
        }
        "正多边形" -> {
            derive(s) {
                set("side", get("n")?.let { n -> get("outR")?.let { R -> 2 * R * sin(PI / n) } })
                set("outR", get("n")?.let { n -> get("side")?.let { a -> a / (2 * sin(PI / n)) } })
                set("inR", get("n")?.let { n -> get("side")?.let { a -> a / (2 * tan(PI / n)) } })
                set("S", get("n")?.let { n -> get("side")?.let { a -> n * a * a / (4 * tan(PI / n)) } })
                set("side", get("n")?.let { n -> get("S")?.let { S -> sqrt(4 * S * tan(PI / n) / n) } })
                set("C", get("n")?.let { n -> get("side")?.let { a -> n * a } })
            }
            if (s.get("side") == null || s.get("n") == null) return null to "请填写边数 n 与边长（或面积/外接圆半径）"
            addAll(
                out(s, given, "n", "边数 n"), out(s, given, "side", "边长 a"), out(s, given, "C", "周长"),
                out(s, given, "S", "面积 S"), out(s, given, "inR", "内切圆半径 r"),
                out(s, given, "outR", "外接圆半径 R"), "内角" to f2((s.get("n")!! - 2) * 180.0 / s.get("n")!!)
            )
        }
        "长方体" -> {
            derive(s) {
                set("S底", get("a")?.let { a -> get("b")?.let { b -> a * b } })
                set("b", get("a")?.let { a -> get("S底")?.let { S -> S / a } })
                set("a", get("b")?.let { b -> get("S底")?.let { S -> S / b } })
                set("c", get("a")?.let { a -> get("b")?.let { b -> get("V")?.let { V -> V / (a * b) } } })
                set("V", get("S底")?.let { S -> get("c")?.let { c -> S * c } })
                set("c", get("a")?.let { a -> get("b")?.let { b -> get("S表")?.let { S -> (S / 2 - a * b) / (a + b) } } })
                set("S表", get("S底")?.let { S -> get("c")?.let { c -> get("a")?.let { a -> get("b")?.let { b -> 2 * S + 2 * c * (a + b) } } } })
                set("S表", get("a")?.let { a -> get("b")?.let { b -> get("c")?.let { c -> 2 * (a * b + b * c + a * c) } } })
                set("c", get("a")?.let { a -> get("b")?.let { b -> get("d")?.let { d -> if (d * d > a * a + b * b) sqrt(d * d - a * a - b * b) else null } } })
                set("d", get("a")?.let { a -> get("b")?.let { b -> get("c")?.let { c -> sqrt(a * a + b * b + c * c) } } })
            }
            if (s.get("a") == null || s.get("b") == null || s.get("c") == null) return null to "请填写长宽高中的至少两个，外加体积/表面积/对角线之一（或直接填长宽高）"
            val a = s.get("a")!!; val b = s.get("b")!!; val c = s.get("c")!!
            addAll(
                out(s, given, "a", "长 a"), out(s, given, "b", "宽 b"), out(s, given, "c", "高 c"),
                out(s, given, "S底", "底面积 S底"), out(s, given, "S表", "表面积 S表"), out(s, given, "V", "体积 V"),
                out(s, given, "d", "体对角线 d"),
                "各面面积" to "ab=${f2(a * b)}，bc=${f2(b * c)}，ac=${f2(a * c)}",
                "外接球半径 R" to f2(sqrt(a * a + b * b + c * c) / 2),
                "最大内切球半径 r" to f2(minOf(a, b, c) / 2)
            )
        }
        "正方体" -> {
            derive(s) {
                set("a", get("S表")?.let { sqrt(it / 6) })
                set("a", get("V")?.let { Math.cbrt(it) })
                set("a", get("d")?.let { it / sqrt(3.0) })
                set("S表", get("a")?.let { 6 * it * it })
                set("V", get("a")?.let { it * it * it })
                set("d", get("a")?.let { it * sqrt(3.0) })
            }
            val a = s.get("a") ?: return null to "请填写棱长、表面积、体积或体对角线中的任意一个"
            addAll(
                out(s, given, "a", "棱长 a"), out(s, given, "S表", "表面积 S表"), out(s, given, "V", "体积 V"),
                out(s, given, "d", "体对角线 d"),
                "外接球半径 R" to f2(a * sqrt(3.0) / 2),
                "内切球半径 r" to f2(a / 2),
                "棱切球半径" to f2(a * sqrt(2.0) / 2)
            )
        }
        "圆柱" -> {
            derive(s) {
                set("d", get("r")?.times(2)); set("r", get("d")?.div(2))
                set("C底", get("r")?.times(2 * PI)); set("r", get("C底")?.div(2 * PI))
                set("S底", get("r")?.let { PI * it * it }); set("r", get("S底")?.let { sqrt(it / PI) })
                set("S侧", get("r")?.let { r -> get("h")?.let { h -> 2 * PI * r * h } })
                set("h", get("r")?.let { r -> get("S侧")?.let { S -> S / (2 * PI * r) } })
                set("r", get("h")?.let { h -> get("S侧")?.let { S -> S / (2 * PI * h) } })
                set("V", get("S底")?.let { S -> get("h")?.let { h -> S * h } })
                set("h", get("S底")?.let { S -> get("V")?.let { V -> V / S } })
                set("S底", get("h")?.let { h -> get("V")?.let { V -> V / h } })
                set("S表", get("S底")?.let { S -> get("S侧")?.let { L -> 2 * S + L } })
                set("S侧", get("S表")?.let { T -> get("S底")?.let { S -> T - 2 * S } })
                set("S底", get("S表")?.let { T -> get("S侧")?.let { L -> (T - L) / 2 } })
            }
            if (s.get("r") == null || s.get("h") == null) return null to "请至少填写半径（或直径/底面积/周长）与高（或体积/侧面积）"
            val r = s.get("r")!!; val h = s.get("h")!!
            addAll(
                out(s, given, "r", "底面半径 r"), out(s, given, "d", "底面直径 d"), out(s, given, "h", "高 h"),
                out(s, given, "S底", "底面积 S底"), out(s, given, "C底", "底面周长 C底"),
                out(s, given, "S侧", "侧面积 S侧"), out(s, given, "S表", "表面积 S表"), out(s, given, "V", "体积 V"),
                "外接球半径 R" to f2(sqrt(r * r + h * h / 4)),
                "内切球半径 r内" to f2(minOf(r, h / 2))
            )
        }
        "圆锥" -> {
            derive(s) {
                set("l", get("r")?.let { r -> get("h")?.let { h -> sqrt(r * r + h * h) } })
                set("h", get("r")?.let { r -> get("l")?.let { l -> if (l > r) sqrt(l * l - r * r) else null } })
                set("r", get("h")?.let { h -> get("l")?.let { l -> if (l > h) sqrt(l * l - h * h) else null } })
                set("S底", get("r")?.let { PI * it * it })
                set("S侧", get("r")?.let { r -> get("l")?.let { l -> PI * r * l } })
                set("V", get("S底")?.let { S -> get("h")?.let { h -> S * h / 3 } })
                set("S表", get("S底")?.let { S -> get("S侧")?.let { L -> S + L } })
            }
            if (s.get("r") == null || s.get("h") == null) return null to "请填写底面半径与高（或母线/体积/侧面积配合）"
            val r = s.get("r")!!; val h = s.get("h")!!; val l = s.get("l") ?: sqrt(r * r + h * h)
            addAll(
                out(s, given, "r", "底面半径 r"), out(s, given, "h", "高 h"), out(s, given, "l", "母线 l"),
                out(s, given, "S底", "底面积 S底"), out(s, given, "S侧", "侧面积 S侧"),
                out(s, given, "S表", "表面积 S表"), out(s, given, "V", "体积 V"),
                "外接球半径 R" to f2((r * r + h * h) / (2 * h)),
                "内切球半径 r内" to f2(r * h / (r + l))
            )
        }
        "圆台" -> {
            derive(s) {
                set("l", get("R")?.let { R -> get("r")?.let { r -> get("h")?.let { h -> sqrt((R - r) * (R - r) + h * h) } } })
                set("h", get("R")?.let { R -> get("r")?.let { r -> get("l")?.let { l -> if (l > abs(R - r)) sqrt(l * l - (R - r) * (R - r)) else null } } })
                set("S侧", get("R")?.let { R -> get("r")?.let { r -> get("l")?.let { l -> PI * (R + r) * l } } })
                set("V", get("R")?.let { R -> get("r")?.let { r -> get("h")?.let { h -> PI * h * (R * R + R * r + r * r) / 3 } } })
                set("S表", get("R")?.let { R -> get("r")?.let { r -> get("S侧")?.let { L -> L + PI * R * R + PI * r * r } } })
            }
            if (s.get("R") == null || s.get("r") == null || s.get("h") == null) return null to "请填写下底半径、上底半径与高（或母线/侧面积/体积配合）"
            val R = s.get("R")!!; val r = s.get("r")!!; val h = s.get("h")!!; val l = s.get("l") ?: sqrt((R - r) * (R - r) + h * h)
            val z = (r * r + h * h - R * R) / (2 * h)
            addAll(
                out(s, given, "R", "下底半径 R"), out(s, given, "r", "上底半径 r"), out(s, given, "h", "高 h"),
                out(s, given, "l", "母线 l"), out(s, given, "S侧", "侧面积 S侧"), out(s, given, "S表", "表面积 S表"),
                out(s, given, "V", "体积 V"),
                "外接球半径 R外" to f2(sqrt(R * R + z * z)),
                if (abs((R + r) - l) < 1e-6) "内切球半径 r内" to f2(h / 2) else "内切球" to "不存在（需满足 下底半径+上底半径=母线）"
            )
        }
        "球" -> {
            derive(s) {
                set("d", get("r")?.times(2)); set("r", get("d")?.div(2))
                set("S", get("r")?.let { 4 * PI * it * it })
                set("V", get("r")?.let { 4.0 / 3.0 * PI * it * it * it })
                set("r", get("S")?.let { sqrt(it / (4 * PI)) })
                set("r", get("V")?.let { Math.cbrt(3 * it / (4 * PI)) })
            }
            if (s.get("r") == null) return null to "请填写半径、直径、表面积或体积中的任意一个"
            addAll(
                out(s, given, "r", "半径 r"), out(s, given, "d", "直径 d"),
                out(s, given, "S", "表面积 S"), out(s, given, "V", "体积 V"),
                "外接球半径 = 内切球半径" to f2(s.get("r")!!)
            )
        }
        else -> {
            derive(s) {
                set("V", if (shape == "棱柱") get("S底")?.let { S -> get("h")?.let { h -> S * h } }
                else get("S底")?.let { S -> get("h")?.let { h -> S * h / 3 } })
                set("h", get("S底")?.let { S -> get("V")?.let { V -> if (shape == "棱柱") V / S else 3 * V / S } })
                set("S底", get("h")?.let { h -> get("V")?.let { V -> if (shape == "棱柱") V / h else 3 * V / h } })
                set("S侧", get("C底")?.let { C -> get("h")?.let { h -> C * h } })
                set("S表", get("S底")?.let { S -> get("S侧")?.let { L -> if (shape == "棱柱") 2 * S + L else S + L } })
            }
            if (s.get("V") == null) return null to "请填写底面积与高（或体积+其中一个）"
            addAll(
                out(s, given, "S底", "底面积 S底"), out(s, given, "C底", "底面周长 C底"), out(s, given, "h", "高 h"),
                out(s, given, "V", "体积 V"), out(s, given, "S侧", "侧面积 S侧"), out(s, given, "S表", "表面积 S表")
            )
        }
    }
    return res to null
}

@Composable
fun GeometryProTool() {
    var shape by remember { mutableStateOf("圆") }
    val inputs = remember(shape) { mutableStateMapOf<String, String>() }
    var result by remember(shape) { mutableStateOf(listOf<Pair<String, String>>()) }
    var error by remember(shape) { mutableStateOf("") }

    fun compute() {
        error = ""
        result = emptyList()
        val parsed = mutableMapOf<String, Double>()
        fieldsOf(shape).forEach { f ->
            val text = inputs[f.key]?.trim().orEmpty()
            if (text.isNotEmpty()) {
                val v = text.toDoubleOrNull() ?: run {
                    error = "${f.label} 不是有效数字"
                    return
                }
                if (v <= 0) {
                    error = "${f.label} 必须大于 0"
                    return
                }
                parsed[f.key] = v
            }
        }
        if (parsed.isEmpty()) {
            error = "请至少填写一个已知量，其余会自动算出来"
            return
        }
        val (res, err) = solveShape(shape, parsed)
        if (err != null) error = err else result = res ?: emptyList()
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "选择图形（18 种）") {
            ChoiceChips(SHAPES, shape, { shape = it }, { it })
        }
        SectionCard(title = "已知条件（填你有的，留空自动求）") {
            Text(
                "只填已知的量即可，例如圆柱只知道“底面积 + 高”也能算出体积、侧面积、表面积、外接球半径等。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "🔤 符号说明：" + fieldsOf(shape).joinToString("；") { it.label },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(10.dp))
            fieldsOf(shape).forEach { f ->
                LabeledField(
                    value = inputs[f.key].orEmpty(),
                    onChange = { inputs[f.key] = it },
                    label = f.label,
                    keyboardType = KeyboardType.Decimal
                )
                Spacer(Modifier.height(8.dp))
            }
            Button(onClick = { compute() }, modifier = Modifier.fillMaxWidth()) { Text("计算剩余量") }
        }
        ErrorText(error)
        if (result.isNotEmpty()) {
            SectionCard(title = "结果（未填写的为自动求出）") {
                result.forEach { (label, value) -> InfoRow("$label：", value) }
                Spacer(Modifier.height(8.dp))
                Text(
                    "🔤 符号说明：" + fieldsOf(shape).joinToString("；") { it.label },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(8.dp))
                CopyButton(result.joinToString("\n") { "${it.first}：${it.second}" })
            }
        }
        SectionCard(title = "📘 常用几何公式（含符号说明）") {
            GEO_FORMULAS.forEachIndexed { index, formula ->
                if (index > 0) Spacer(Modifier.height(10.dp))
                Text(
                    formula.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(2.dp))
                MathText(
                    text = formula.expr,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "🔤 ${formula.legend}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
