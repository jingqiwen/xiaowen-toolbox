package com.jisuanyusuiji.toolbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 公式排版组件：把数据里的 ASCII 写法渲染成接近 LaTeX 的样子。
 *
 * 设计原则（重点解决“公式位置乱、框特别大”）：
 *  1. 默认整条公式作为一个可换行的文本排版，保证永远不会错位；
 *  2. 只有“纯数学、较短”的公式才拆成 左 = 右 两栏，且分数才做上下堆叠；
 *  3. 含中文、或过长的公式一律按整段文本渲染（中文说明更适合放 note 里）。
 */
@Composable
fun MathText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 17.sp,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight = FontWeight.Medium
) {
    val tint = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color
    val pretty = remember(text) { prettifyMath(text) }
    val hasCjk = remember(pretty) { pretty.any { isCjk(it) } }
    val eqPositions = remember(pretty) { topLevelPositions(pretty, '=') }
    val canSplit = !hasCjk && eqPositions.size == 1 && pretty.length <= 44

    if (canSplit) {
        val left = pretty.substring(0, eqPositions[0]).trim()
        val right = pretty.substring(eqPositions[0] + 1).trim()
        val leftFraction = shortFraction(left)
        val rightFraction = shortFraction(right)
        if (leftFraction == null && rightFraction == null) {
            SingleMathText(pretty, fontSize, tint, fontWeight, modifier.fillMaxWidth())
        } else {
            Row(
                modifier = modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leftFraction != null) {
                    FractionBlock(leftFraction.first, leftFraction.second, fontSize, tint, fontWeight)
                } else {
                    SingleMathText(left, fontSize, tint, fontWeight)
                }
                Text(
                    "  =  ",
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = fontSize,
                        color = tint,
                        fontWeight = fontWeight
                    )
                )
                if (rightFraction != null) {
                    FractionBlock(rightFraction.first, rightFraction.second, fontSize, tint, fontWeight)
                } else {
                    SingleMathText(right, fontSize, tint, fontWeight)
                }
            }
        }
    } else {
        val fraction = if (!hasCjk && pretty.length <= 40) shortFraction(pretty) else null
        if (fraction != null) {
            Box(modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                FractionBlock(fraction.first, fraction.second, fontSize, tint, fontWeight)
            }
        } else {
            SingleMathText(pretty, fontSize, tint, fontWeight, modifier.fillMaxWidth())
        }
    }
}

/** 单个公式文本：上/下标 + 换行，永远从左侧对齐。 */
@Composable
private fun SingleMathText(
    s: String,
    fontSize: TextUnit,
    color: Color,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier
) {
    val annotated = remember(s, fontSize) { buildMathAnnotated(s, fontSize, color) }
    Text(
        annotated,
        modifier = modifier,
        style = TextStyle(
            fontFamily = FontFamily.Serif,
            fontSize = fontSize,
            color = color,
            fontWeight = fontWeight,
            lineHeight = fontSize * 1.55f,
            letterSpacing = 0.3.sp
        )
    )
}

/** 上下结构的分数块：分子 / 分数线 / 分母，宽度取两者较宽者。 */
@Composable
private fun FractionBlock(
    numerator: String,
    denominator: String,
    fontSize: TextUnit,
    color: Color,
    fontWeight: FontWeight
) {
    Column(
        modifier = Modifier.width(IntrinsicSize.Max),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SingleMathText(numerator, fontSize * 0.92f, color, fontWeight)
        Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .height(1.2.dp)
                .background(color.copy(alpha = 0.7f))
        )
        SingleMathText(denominator, fontSize * 0.92f, color, fontWeight)
    }
}

/** 判断是否为中文/全角字符（用于决定是否需要拆分栏）。 */
internal fun isCjk(c: Char): Boolean {
    val code = c.code
    return code in 0x4E00..0x9FFF || code in 0x3400..0x4DBF ||
        code in 0x3000..0x303F || code in 0xFF00..0xFFEF
}

/**
 * 只有“短且无中文”的分式才做上下堆叠，避免长公式把卡片撑得很大。
 */
internal fun shortFraction(s: String): Pair<String, String>? {
    if (s.length > 40 || s.any { isCjk(it) }) return null
    val f = splitFraction(s) ?: return null
    if (f.first.length > 24 || f.second.length > 24) return null
    return f
}

/** ASCII → 数学符号，并自动整理运算符间距。 */
internal fun prettifyMath(raw: String): String {
    var s = raw.trim()
    s = s.replace("<=", "≤").replace(">=", "≥").replace("!=", "≠")
    s = s.replace("->", "→").replace("=>", "⇒").replace("...", "…")
    s = s.replace(Regex("\\bsqrt\\b"), "√")
    s = s.replace(Regex("\\bpi\\b"), "π")
    s = s.replace(Regex("\\binf\\b"), "∞")
    s = s.replace(Regex("\\btheta\\b"), "θ")
    s = s.replace("*", "·")
    s = s.replace(Regex("\\s*=\\s*"), " = ")
    s = s.replace(Regex("\\s*⇒\\s*"), " ⇒ ")
    // 在两个“操作数”之间补空格：a+b → a + b、x-1 → x - 1；指数里的 e^-x 不会被改动
    val atomBefore = "(?<=[0-9A-Za-zα-ωΑ-Ω)）])"
    val atomAfter = "(?=[0-9A-Za-zα-ωΑ-Ω(（])"
    s = s.replace(Regex(atomBefore + "\\s*([+\\-])\\s*" + atomAfter), " $1 ")
    s = s.replace(Regex(atomBefore + "\\s*±\\s*" + atomAfter), " ± ")
    s = s.replace(Regex("([A-Za-z]) \\("), "$1(")
    // 多空格合并为一个
    s = s.replace(Regex(" {2,}"), " ")
    return s.trim()
}

/** 返回 target 在顶层（括号外）出现的位置。 */
internal fun topLevelPositions(s: String, target: Char): List<Int> {
    val out = mutableListOf<Int>()
    var depth = 0
    s.forEachIndexed { i, c ->
        when (c) {
            '(', '[', '{' -> depth++
            ')', ']', '}' -> if (depth > 0) depth--
            target -> if (depth == 0) out.add(i)
        }
    }
    return out
}

private fun matchingClose(s: String, start: Int): Int {
    val open = s[start]
    val close = when (open) {
        '(' -> ')'
        '[' -> ']'
        else -> '}'
    }
    var depth = 0
    for (i in start until s.length) {
        if (s[i] == open) depth++
        else if (s[i] == close) {
            depth--
            if (depth == 0) return i
        }
    }
    return -1
}

/**
 * 判断 s 是否是“一个分子 / 一个分母”的简单分数：
 * 要求只有 1 个顶层斜杠，且分子分母各自是单个原子或一个完整括号组。
 */
internal fun splitFraction(s: String): Pair<String, String>? {
    val text = s.trim()
    val slashes = topLevelPositions(text, '/')
    if (slashes.size != 1) return null
    val idx = slashes[0]
    val num = text.substring(0, idx).trim()
    val den = text.substring(idx + 1).trim()
    if (num.isEmpty() || den.isEmpty()) return null

    fun isAtomOrGroup(part: String): Boolean {
        if (part.startsWith("(") || part.startsWith("[")) {
            val close = matchingClose(part, 0)
            return close == part.length - 1
        }
        return Regex("^[+-]?[0-9A-Za-z.]+$").matches(part)
    }
    val numBody = if (num.startsWith("-")) num.substring(1).trim() else num
    if (!isAtomOrGroup(numBody)) return null
    if (!isAtomOrGroup(den)) return null
    return num to den
}

/** 读取 ^ 或 _ 后面的一个参数：括号组或单个原子（可带正负号）。 */
internal fun readMathToken(s: String, start: Int): Pair<String, Int> {
    if (start >= s.length) return "" to start
    return if (s[start] == '(' || s[start] == '[') {
        val close = matchingClose(s, start)
        if (close < 0) s.substring(start) to s.length
        else s.substring(start, close + 1) to close + 1
    } else {
        var i = start
        if (i < s.length && (s[i] == '+' || s[i] == '-')) i++
        if (i < s.length && s[i].isDigit()) {
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
        } else {
            while (i < s.length && s[i].isLetter()) i++
        }
        if (i == start || (i == start + 1 && (s[start] == '+' || s[start] == '-'))) {
            s.substring(start, (start + 1).coerceAtMost(s.length)) to (start + 1).coerceAtMost(s.length)
        } else {
            s.substring(start, i) to i
        }
    }
}

internal fun stripWrapper(s: String): String {
    val t = s.trim()
    return if (t.length >= 2 && ((t.first() == '(' && t.last() == ')') || (t.first() == '[' && t.last() == ']'))) {
        t.substring(1, t.length - 1)
    } else t
}

/** 递归构建带上下标的文本。 */
internal fun buildMathAnnotated(src: String, base: TextUnit, color: Color): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0
    while (i < src.length) {
        val c = src[i]
        if (c == '^' || c == '_') {
            val shift = if (c == '^') BaselineShift.Superscript else BaselineShift.Subscript
            i++
            val (token, next) = readMathToken(src, i)
            i = next
            val inner = stripWrapper(token)
            val innerText = buildMathAnnotated(inner, base * 0.72f, color)
            val start = builder.length
            builder.append(innerText)
            builder.addStyle(
                SpanStyle(baselineShift = shift, color = color),
                start,
                builder.length
            )
        } else {
            builder.append(c)
            i++
        }
    }
    return builder.toAnnotatedString()
}
