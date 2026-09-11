package com.jisuanyusuiji.toolbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// ============================================================
// 公式节点模型
// ============================================================

/**
 * 公式被解析成的排版节点。
 *
 * - [Txt]     普通文本（其中 ^ / _ 交给渲染层做上下标）
 * - [Frac]    分式：上下结构，分子在上、分母在下、中间一条分数线
 * - [Indexed] 排列数 / 组合数：字母 + 右上角“上一个下”的上下标（C 在上、n 在下）
 */
sealed interface MathNode {
    data class Txt(val text: String) : MathNode
    data class Frac(val num: List<MathNode>, val den: List<MathNode>) : MathNode
    data class Indexed(
        val letter: String,
        val sub: List<MathNode>,
        val sup: List<MathNode>
    ) : MathNode
}

internal data class ArgsCall(
    val letter: String,
    val first: String,
    val second: String,
    val end: Int
)

/**
 * 把 ASCII 公式解析成排版节点：
 *  - C(n,m) / A(n,m) → 排列数、组合数（字母 + 上下标，不使用括号形式）
 *  - a/b            → 分式（上下结构）
 *  - 其它           → 普通文本
 */
internal fun parseMathNodes(input: String): List<MathNode> {
    val result = mutableListOf<MathNode>()
    var textStart = 0
    var i = 0
    while (i < input.length) {
        // 1) 排列数 / 组合数
        if ((input[i] == 'A' || input[i] == 'C') &&
            i + 1 < input.length && (input[i + 1] == '(' || input[i + 1] == '（')
        ) {
            val call = parseArgsCall(input, i)
            if (call != null) {
                if (i > textStart) result += MathNode.Txt(input.substring(textStart, i))
                val a = parseMathNodes(call.first)
                val b = parseMathNodes(call.second)
                result += if (call.letter == "C") {
                    MathNode.Indexed("C", sub = a, sup = b)
                } else {
                    MathNode.Indexed("A", sub = a, sup = b)
                }
                i = call.end
                textStart = i
                continue
            }
        }
        // 2) 分式
        if (input[i] == '/' && i > 0) {
            val match = matchFraction(input, i, textStart)
            if (match != null) {
                if (match.numStart > textStart) {
                    result += MathNode.Txt(input.substring(textStart, match.numStart))
                }
                val num = parseMathNodes(input.substring(match.numStart, i).trim())
                val den = parseMathNodes(input.substring(i + 1, match.denEnd).trim())
                result += MathNode.Frac(num, den)
                i = match.denEnd
                textStart = match.denEnd
                continue
            }
        }
        i++
    }
    if (textStart < input.length) result += MathNode.Txt(input.substring(textStart))
    return result.filterNot { it is MathNode.Txt && it.text.isEmpty() }
}

/** 解析 A(n,m) / C(n,m)，支持中英文括号与嵌套括号；不是两参数调用时返回 null。 */
internal fun parseArgsCall(s: String, start: Int): ArgsCall? {
    val letter = s[start].toString()
    val openIndex = start + 1
    val open = s[openIndex]
    val close = if (open == '（') '）' else ')'
    var depth = 0
    var i = openIndex
    var comma = -1
    while (i < s.length) {
        val c = s[i]
        if (c == open) depth++
        else if (c == close) {
            depth--
            if (depth == 0) {
                if (comma < 0) return null
                val first = s.substring(openIndex + 1, comma).trim()
                val second = s.substring(comma + 1, i).trim()
                if (first.isEmpty() || second.isEmpty()) return null
                if (first.any { it == ',' || it == '，' }) return null
                if (second.any { it == ',' || it == '，' }) return null
                return ArgsCall(letter, first, second, i + 1)
            }
        } else if (c == ',' || c == '，') {
            if (depth == 1 && comma < 0) comma = i
        }
        i++
    }
    return null
}

internal data class FractionMatch(val numStart: Int, val denEnd: Int)

/**
 * 判断 [slash] 处的“/”能否安全地排版成竖式分数。
 *
 * ASCII 的“/”二义性很重：`1/2ab` 通常表示 (1/2)·ab，而 `ml^2/12` 表示 (ml²)/12。
 * 这里用几条保守规则处理常见写法，避免把公式改错：
 *  - 分子、分母都是完整括号组 → 直接堆叠；
 *  - 分母是纯数字开头的连乘（如 2ab、2at^2）且分子是纯数字 → 只把最前面的数字当分母，
 *    其余部分留在分数后面（1/2ab → ½·ab），但若该分母被右括号包住（(1/2a)）则整体作为分母；
 *  - 分母后面紧跟字母/数字/括号之外的连写因子、或“a/b/c”链式除法时不处理。
 */
internal fun matchFraction(s: String, slash: Int, limit: Int): FractionMatch? {
    val numStart = atomStartBefore(s, slash, limit)
    if (numStart < 0 || numStart >= slash) return null
    if (numStart > limit && s[numStart - 1] == '/') return null // a/b/c 链式除法
    val num = s.substring(numStart, slash).trim()
    if (num.isEmpty() || num.any { isCjk(it) }) return null
    if (num.length > 30) return null

    val denScan = scanDenominator(s, slash + 1) ?: return null
    var denEnd = denScan.end
    if (denEnd <= slash + 1) return null
    var den = s.substring(slash + 1, denEnd).trim()
    if (den.isEmpty() || den.any { isCjk(it) }) return null
    if (den.length > 30) return null

    // 分母以纯数字开头、后面又紧跟因子的情况：1/2ab、1/2at^2、2/5mR^2 …
    // 特例：整个分式被括号包住且括号后紧跟函数/因子时，形如 (1/2a)ln|…|，分母应取 2a。
    var cut = false
    if (!denScan.grouped && isPureNumber(num)) {
        val lead = leadingNumberLength(den)
        if (lead in 1 until denEnd - (slash + 1)) {
            val enclosed = numStart > limit && (s[numStart - 1] == '(' || s[numStart - 1] == '[') &&
                matchingClose(s, numStart - 1) == denEnd
            val followedByFactor = enclosed && isFactorStart(s, denEnd + 1)
            if (!(enclosed && followedByFactor)) {
                denEnd = slash + 1 + lead
                den = s.substring(slash + 1, denEnd).trim()
                cut = true
            }
        }
    }

    // 分母后面若紧跟连写因子（既不是运算符号也不是空格/结尾），不堆叠以免改变含义
    val tailIndex = skipSpaces(s, denEnd)
    val tail = s.getOrNull(tailIndex)
    if (tail == '/' || tail == '∕') return null // a/b/c 链式除法
    val simpleNumbers = isPureNumber(num) && isPureNumber(den)
    if (!denScan.grouped && !cut && !simpleNumbers && tail != null) {
        val stops = tail == '=' || tail == '+' || tail == '-' || tail == '·' || tail == '×' ||
            tail == '÷' || tail == '/' || tail == '∕' || tail == ',' || tail == '，' ||
            tail == ';' || tail == '；' || tail == ':' || tail == '：' || tail == ')' ||
            tail == ']' || tail == '）' || tail == '}' || tail == '>' || tail == '<' ||
            tail == '≥' || tail == '≤' || tail == '→' || tail == '⇒' || tail == '±' || tail == '∓'
        val directlyAttached = tailIndex == denEnd
        if (!stops && directlyAttached && (tail.isLetterOrDigit() || tail == '(' || tail == '[')) {
            return null
        }
    }
    return FractionMatch(numStart, denEnd)
}

internal data class DenScan(val end: Int, val grouped: Boolean)

/** 从分母起点向后扫描一个“分母原子”。 */
internal fun scanDenominator(s: String, from: Int): DenScan? {
    var i = skipSpaces(s, from)
    if (i >= s.length) return null
    val first = s[i]
    if (first == '(' || first == '[' || first == '（' || first == '【') {
        val close = matchingClose(s, i) ?: return null
        return DenScan(close + 1, grouped = true)
    }
    var end = i
    while (end < s.length && isAtomChar(s[end])) end++
    if (end == i) return null

    // 继续吞并“以空格分隔的连乘原子”（如 log_c a），但遇到括号组/运算符就停
    var j = skipSpaces(s, end)
    while (j < s.length && isAtomChar(s[j])) {
        var k = j
        while (k < s.length && isAtomChar(s[k])) k++
        end = k
        j = skipSpaces(s, k)
    }
    return DenScan(end, grouped = false)
}

internal fun skipSpaces(s: String, from: Int): Int {
    var i = from
    while (i < s.length && (s[i] == ' ' || s[i] == '\u3000')) i++
    return i
}

internal fun isPureNumber(s: String): Boolean =
    s.isNotEmpty() && s.length <= 8 && s.all { it.isDigit() || it == '.' }

internal fun leadingNumberLength(s: String): Int {
    var i = 0
    while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
    return i
}

/** 判断 [from] 之后（跳过空格）是否是一个可连写的因子/函数起点。 */
internal fun isFactorStart(s: String, from: Int): Boolean {
    val i = skipSpaces(s, from)
    val c = s.getOrNull(i) ?: return false
    return c.isLetterOrDigit() || c == '(' || c == '（' || c == '[' || c == '√' || c == '∫'
}

private fun isAtomChar(c: Char): Boolean =
    c.isLetterOrDigit() || c == '.' || c == '!' || c == '\'' || c == '′' || c == '^' || c == '_'

/** 从斜杠位置向左找出分子原子的起点；找不到返回 -1。 */
internal fun atomStartBefore(s: String, slashIndex: Int, limit: Int): Int {
    var i = slashIndex - 1
    var start = -1
    while (i >= limit) {
        when {
            s[i] == ' ' || s[i] == '\u3000' -> i--
            s[i] == ')' || s[i] == ']' || s[i] == '）' || s[i] == '】' -> {
                val close = s[i]
                val open = when (close) {
                    ')' -> '('
                    ']' -> '['
                    '）' -> '（'
                    else -> '【'
                }
                var depth = 0
                var j = i
                var found = -1
                while (j >= limit) {
                    if (s[j] == close) depth++
                    if (s[j] == open) {
                        depth--
                        if (depth == 0) {
                            found = j
                            break
                        }
                    }
                    j--
                }
                if (found < 0) return start
                start = found
                i = found - 1
            }
            isAtomChar(s[i]) -> {
                start = i
                i--
            }
            else -> return start
        }
    }
    if (start < 0) return start
    // 允许一元负号并入分子：-b/2a、(-b±√…)/(2a)
    var k = start - 1
    while (k >= limit && (s[k] == ' ' || s[k] == '\u3000')) k--
    if (k >= limit && (s[k] == '-' || s[k] == '+')) {
        val before = if (k - 1 >= limit) s[k - 1] else ' '
        val unary = k - 1 < limit || before in charArrayOf(
            '=', '(', '[', '{', '（', '【', ',', '，', '；', ';', ':', '：',
            '+', '-', '*', '/', '·', '×', '÷', '⇒', '→', ' '
        )
        if (unary) start = k
    }
    return start
}

internal fun matchingClose(s: String, start: Int): Int? {
    val open = s[start]
    val close = when (open) {
        '(' -> ')'
        '[' -> ']'
        '（' -> '）'
        '【' -> '】'
        else -> return null
    }
    var depth = 0
    for (i in start until s.length) {
        if (s[i] == open) depth++
        else if (s[i] == close) {
            depth--
            if (depth == 0) return i
        }
    }
    return null
}

// ============================================================
// 渲染
// ============================================================

/**
 * 公式排版组件：分式上下结构、排列数/组合数“字母 + 上下标”、普通文本自动上下标。
 * 内容作为一整段文本排版，可自动换行且不会左右错位。
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
    val remembered = remember(text, fontSize, tint, fontWeight) {
        val nodes = parseMathNodes(prettifyMath(text))
        val inlineContents = HashMap<String, InlineTextContent>()
        val annotated = buildMathParagraph(nodes, fontSize, tint, fontWeight, inlineContents)
        annotated to inlineContents
    }
    Text(
        remembered.first,
        modifier = modifier.fillMaxWidth(),
        inlineContent = remembered.second,
        style = TextStyle(
            fontFamily = FontFamily.Serif,
            fontSize = fontSize,
            color = tint,
            fontWeight = fontWeight,
            lineHeight = fontSize * 1.65f,
            letterSpacing = 0.2.sp,
            textAlign = TextAlign.Start
        )
    )
}

private fun buildMathParagraph(
    nodes: List<MathNode>,
    fontSize: TextUnit,
    color: Color,
    fontWeight: FontWeight,
    inlineContents: HashMap<String, InlineTextContent>
): AnnotatedString = buildAnnotatedString {
    var index = 0
    fun emit(list: List<MathNode>) {
        list.forEach { node ->
            when (node) {
                is MathNode.Txt -> append(buildMathAnnotated(node.text, fontSize, color))
                is MathNode.Frac -> {
                    val id = "frac${index++}"
                    val plain = plainText(node)
                    appendInlineContent(id, plain)
                    inlineContents[id] = InlineTextContent(
                        placeholder = Placeholder(
                            width = estimateWidth(node).em,
                            height = 2.55.em,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                        )
                    ) { _ ->
                        FractionContent(node.num, node.den, fontSize, color, fontWeight)
                    }
                }
                is MathNode.Indexed -> {
                    val id = "idx${index++}"
                    val plain = plainText(node)
                    appendInlineContent(id, plain)
                    inlineContents[id] = InlineTextContent(
                        placeholder = Placeholder(
                            width = (estimateWidth(node) + 0.15f).em,
                            height = 2.15.em,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                        )
                    ) { _ ->
                        IndexedContent(node, fontSize, color, fontWeight)
                    }
                }
            }
        }
    }
    emit(nodes)
}

/** 分式：分子 / 分数线 / 分母。 */
@Composable
private fun FractionContent(
    num: List<MathNode>,
    den: List<MathNode>,
    fontSize: TextUnit,
    color: Color,
    fontWeight: FontWeight
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        InlineNodesLine(num, fontSize * 0.82f, color, fontWeight)
        Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
                .height(1.2.dp)
                .background(color.copy(alpha = 0.8f))
        )
        InlineNodesLine(den, fontSize * 0.82f, color, fontWeight)
    }
}

/** 排列数 / 组合数：字母 + 右侧“上标在上、下标在下”。 */
@Composable
private fun IndexedContent(
    node: MathNode.Indexed,
    fontSize: TextUnit,
    color: Color,
    fontWeight: FontWeight
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            node.letter,
            fontFamily = FontFamily.Serif,
            fontSize = fontSize,
            color = color,
            fontWeight = fontWeight,
            softWrap = false
        )
        Column(horizontalAlignment = Alignment.Start) {
            InlineNodesLine(node.sup, fontSize * 0.62f, color, fontWeight)
            InlineNodesLine(node.sub, fontSize * 0.62f, color, fontWeight)
        }
    }
}

/** 内联节点行（用于分式 / 上下标内部，不再换行）。 */
@Composable
private fun InlineNodesLine(
    nodes: List<MathNode>,
    fontSize: TextUnit,
    color: Color,
    fontWeight: FontWeight
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        nodes.forEach { node ->
            when (node) {
                is MathNode.Txt -> Text(
                    buildMathAnnotated(node.text, fontSize, color),
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = fontSize,
                        color = color,
                        fontWeight = fontWeight
                    ),
                    softWrap = false
                )
                is MathNode.Frac -> FractionContent(node.num, node.den, fontSize, color, fontWeight)
                is MathNode.Indexed -> IndexedContent(node, fontSize, color, fontWeight)
            }
        }
    }
}

/** 估算节点宽度（单位 em），用于 InlineTextContent 占位。 */
internal fun estimateWidth(nodes: List<MathNode>): Float {
    var w = 0f
    nodes.forEach { node ->
        w += when (node) {
            is MathNode.Txt -> node.text.sumOf { if (isCjk(it)) 1.0 else 0.56 }.toFloat()
            is MathNode.Frac -> estimateWidth(node.num).coerceAtLeast(estimateWidth(node.den)) + 0.5f
            is MathNode.Indexed -> 0.85f +
                estimateWidth(node.sub).coerceAtLeast(estimateWidth(node.sup))
        }
    }
    return w.coerceAtLeast(0.9f)
}

private fun estimateWidth(node: MathNode): Float = estimateWidth(listOf(node))

internal fun plainText(nodes: List<MathNode>): String = nodes.joinToString("") { plainText(it) }

internal fun plainText(node: MathNode): String = when (node) {
    is MathNode.Txt -> node.text
    is MathNode.Frac -> "${plainText(node.num)}/${plainText(node.den)}"
    is MathNode.Indexed -> "${node.letter}(${plainText(node.sub)},${plainText(node.sup)})"
}

// ============================================================
// 文本与上下标
// ============================================================

/** 判断是否为中文/全角字符。 */
internal fun isCjk(c: Char): Boolean {
    val code = c.code
    return code in 0x4E00..0x9FFF || code in 0x3400..0x4DBF ||
        code in 0x3000..0x303F || code in 0xFF00..0xFFEF
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
    val atomBefore = "(?<=[0-9A-Za-zα-ωΑ-Ω)）])"
    val atomAfter = "(?=[0-9A-Za-zα-ωΑ-Ω(（])"
    s = s.replace(Regex(atomBefore + "\\s*([+\\-])\\s*" + atomAfter), " $1 ")
    s = s.replace(Regex(atomBefore + "\\s*±\\s*" + atomAfter), " ± ")
    s = s.replace(Regex("([A-Za-z]) \\("), "$1(")
    s = s.replace(Regex(" {2,}"), " ")
    return s.trim()
}

/** 读取 ^ 或 _ 后面的一个参数：括号组或单个原子（可带正负号）。 */
internal fun readMathToken(s: String, start: Int): Pair<String, Int> {
    if (start >= s.length) return "" to start
    return if (s[start] == '(' || s[start] == '[') {
        val close = matchingClose(s, start)
        if (close == null) s.substring(start) to s.length
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
            if (inner.isEmpty()) {
                builder.append(c)
                continue
            }
            val innerText = buildMathAnnotated(inner, base * 0.72f, color)
            val start = builder.length
            builder.append(innerText)
            builder.addStyle(SpanStyle(baselineShift = shift, color = color), start, builder.length)
        } else {
            builder.append(c)
            i++
        }
    }
    return builder.toAnnotatedString()
}
