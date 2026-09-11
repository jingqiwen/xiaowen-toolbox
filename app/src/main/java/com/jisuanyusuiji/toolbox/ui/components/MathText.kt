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
 * 支持：
 *  - 上标 / 下标：x^2、x_i、∫_a^b、Σ_(k=0)^n（使用 BaselineShift，不依赖特殊字体）
 *  - 分数堆叠：a/(1-q)、(-b±√(b^2-4ac))/(2a) 等会渲染成上下结构
 *  - 符号美化：sqrt→√、pi→π、inf→∞、*→·、<=→≤、>=→≥、!=→≠、->→→
 *  - 衬线字体，接近论文中的数学字体
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
    // 只有一个顶层 = 时，左右分别排版，右侧是纯分数时堆叠显示
    if (pretty.length <= 64 && topLevelPositions(pretty, '=').size == 1) {
        val idx = topLevelPositions(pretty, '=')[0]
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            MathExpr(pretty.substring(0, idx).trim(), fontSize, tint, fontWeight)
            Text(
                " = ",
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontSize = fontSize,
                    color = tint,
                    fontWeight = fontWeight
                ),
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            MathExpr(pretty.substring(idx + 1).trim(), fontSize, tint, fontWeight)
        }
    } else {
        MathExpr(pretty, fontSize, tint, fontWeight, modifier)
    }
}

@Composable
private fun MathExpr(
    s: String,
    fontSize: TextUnit,
    color: Color,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier
) {
    val frac = remember(s) { splitFraction(s) }
    if (frac != null) {
        Column(
            modifier = modifier.width(IntrinsicSize.Max),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MathExpr(frac.first, fontSize * 0.95f, color, fontWeight)
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .height(1.2.dp)
                    .background(color.copy(alpha = 0.75f))
            )
            MathExpr(frac.second, fontSize * 0.95f, color, fontWeight)
        }
    } else {
        val annotated = remember(s, fontSize) { buildMathAnnotated(s, fontSize, color) }
        Text(
            annotated,
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontSize = fontSize,
                color = color,
                fontWeight = fontWeight,
                lineHeight = fontSize * 1.5f
            ),
            modifier = modifier
        )
    }
}

/** ASCII → 数学符号。 */
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
    s = s.replace(Regex("([A-Za-z]) \\("), "$1(")
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
 * 这样 a/(1-q)、(-b±√(b^2-4ac))/(2a) 会被堆叠，而 1/2mv^2 不会（避免歧义）。
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
    // 分子允许带前导负号
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
            // 单独的正负号或特殊符号：取一个字符
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
