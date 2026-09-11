package com.jisuanyusuiji.toolbox.tools.calc

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

/**
 * 本地安全表达式求值器。
 * 支持：四则运算、括号（可自动补齐）、百分号、幂、阶乘、常见函数与常量 pi/e/π，
 * 支持隐式乘法（2(3+1)、9π、3sin(2)、2pi 等）。
 */
object CalcExpr {

    data class EvalResult(val value: Double? = null, val error: String? = null) {
        val ok: Boolean get() = error == null
    }

    fun evaluate(input: String, degrees: Boolean = false): EvalResult {
        if (input.isBlank()) return EvalResult(error = "请输入表达式")
        return try {
            var normalized = input
                .replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")
                .replace("π", "pi")
                .replace("Π", "pi")
                .replace("％", "%")
            // 自动补齐缺少的右括号：tan(2pi → tan(2pi)
            val open = normalized.count { it == '(' }
            val close = normalized.count { it == ')' }
            if (open > close) normalized += ")".repeat(open - close)
            EvalResult(value = Parser(normalized, degrees).parse())
        } catch (e: Exception) {
            EvalResult(error = e.message ?: "表达式格式错误")
        }
    }

    private class Parser(src: String, private val degrees: Boolean) {
        private val s = src.lowercase()
        private var pos = 0

        fun parse(): Double {
            val value = expression()
            skipSpaces()
            if (pos < s.length) fail("存在无法识别的字符")
            return value
        }

        private fun expression(): Double {
            var value = term()
            while (true) {
                skipSpaces()
                when (peek()) {
                    '+' -> { pos++; value += term() }
                    '-' -> { pos++; value -= term() }
                    else -> return value
                }
            }
        }

        private fun term(): Double {
            var value = power()
            while (true) {
                skipSpaces()
                when (val c = peek()) {
                    '*' -> { pos++; value *= power() }
                    '/' -> {
                        pos++
                        val divisor = power()
                        if (divisor == 0.0) fail("除数不能为 0")
                        value /= divisor
                    }
                    // 隐式乘法：9π、2pi、3sin(2)、2(3+1)、(1+2)(3+4)、(2)3 等
                    '(' -> value *= power()
                    else -> {
                        if (c != null && (c.isLetter() || c.isDigit() || c == '.')) value *= power()
                        else return value
                    }
                }
            }
        }

        private fun power(): Double {
            val base = unary()
            skipSpaces()
            if (peek() == '^') {
                pos++
                val exponent = power() // 右结合：2^3^2 = 2^(3^2)
                return base.pow(exponent)
            }
            return base
        }

        private fun unary(): Double {
            skipSpaces()
            return when (peek()) {
                '+' -> { pos++; unary() }
                '-' -> { pos++; -unary() }
                else -> postfix()
            }
        }

        private fun postfix(): Double {
            var value = primary()
            while (true) {
                skipSpaces()
                when (peek()) {
                    '!' -> {
                        pos++
                        if (value < 0 || value != value.toLong().toDouble()) fail("阶乘只支持非负整数")
                        if (value > 170) fail("阶乘数值过大")
                        var r = 1.0
                        for (i in 2..value.toInt()) r *= i
                        value = r
                    }
                    '%' -> {
                        pos++
                        value /= 100.0
                    }
                    else -> return value
                }
            }
        }

        private fun primary(): Double {
            skipSpaces()
            if (pos >= s.length) fail("表达式不完整")
            val c = peek() ?: fail("表达式不完整")
            if (c == '(') {
                pos++
                val v = expression()
                skipSpaces()
                expect(')')
                return v
            }
            if (c.isDigit() || c == '.') return number()
            if (c.isLetter()) return identifier()
            fail("意外的字符：$c")
            return 0.0
        }

        private fun identifier(): Double {
            val start = pos
            while (pos < s.length && (s[pos].isLetter())) pos++
            val name = s.substring(start, pos)
            skipSpaces()
            if (peek() == '(') {
                pos++
                val first = expression()
                skipSpaces()
                if (peek() == ',') {
                    pos++
                    val second = expression()
                    skipSpaces()
                    expect(')')
                    return function2(name, first, second)
                }
                expect(')')
                return function(name, first)
            }
            return when (name) {
                "pi" -> kotlin.math.PI
                "e" -> kotlin.math.E
                else -> fail("未知常量或函数：$name")
            }
        }

        private fun toRadians(x: Double): Double = if (degrees) Math.toRadians(x) else x
        private fun fromRadians(x: Double): Double = if (degrees) Math.toDegrees(x) else x

        private fun function(name: String, x: Double): Double = when (name) {
            "sin" -> sin(toRadians(x))
            "cos" -> cos(toRadians(x))
            "tan" -> tan(toRadians(x))
            "asin", "arcsin" -> fromRadians(asin(x))
            "acos", "arccos" -> fromRadians(acos(x))
            "atan", "arctan" -> fromRadians(atan(x))
            "sinh" -> sinh(x)
            "cosh" -> cosh(x)
            "tanh" -> tanh(x)
            "log", "lg" -> if (x <= 0) fail("log 参数必须大于 0") else log10(x)
            "ln" -> if (x <= 0) fail("ln 参数必须大于 0") else ln(x)
            "lb" -> if (x <= 0) fail("lb 参数必须大于 0") else ln(x) / ln(2.0)
            "sqrt" -> if (x < 0) fail("sqrt 参数不能为负数") else sqrt(x)
            "cbrt" -> Math.cbrt(x)
            "abs" -> abs(x)
            "exp" -> exp(x)
            else -> fail("不支持的函数：$name")
        }

        private fun function2(name: String, x: Double, y: Double): Double = when (name) {
            "ncr", "c" -> combination(x, y)
            "npr", "p" -> permutation(x, y)
            "log" -> {
                if (x <= 0 || y <= 0 || y == 1.0) fail("log 底数/真数无效") else ln(x) / ln(y)
            }
            else -> fail("不支持的双参数函数：$name")
        }

        private fun combination(n: Double, r: Double): Double {
            if (r < 0 || n < 0 || r > n) fail("组合数参数无效")
            var result = 1.0
            for (i in 1..r.toInt()) result = result * (n - r.toInt() + i) / i
            return result
        }

        private fun permutation(n: Double, r: Double): Double {
            if (r < 0 || n < 0 || r > n) fail("排列数参数无效")
            var result = 1.0
            for (i in 0 until r.toInt()) result *= (n - i)
            return result
        }

        private fun number(): Double {
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
            if (pos < s.length && (s[pos] == 'e')) {
                // 科学计数法，如 1e-3
                val save = pos
                pos++
                if (pos < s.length && (s[pos] == '+' || s[pos] == '-')) pos++
                val expStart = pos
                while (pos < s.length && s[pos].isDigit()) pos++
                if (pos == expStart) pos = save // 回退，避免把函数 e 吞掉
            }
            return s.substring(start, pos).toDoubleOrNull() ?: fail("数字格式错误")
        }

        private fun peek(): Char? = if (pos < s.length) s[pos] else null

        private fun expect(c: Char) {
            if (peek() != c) fail("缺少 $c")
            pos++
        }

        private fun skipSpaces() {
            while (pos < s.length && s[pos].isWhitespace()) pos++
        }

        private fun fail(message: String): Nothing = throw IllegalArgumentException(message)
    }
}
