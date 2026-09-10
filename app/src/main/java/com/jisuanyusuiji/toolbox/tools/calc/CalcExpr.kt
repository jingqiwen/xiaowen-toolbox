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
 * 支持：四则运算、括号、百分号、幂、阶乘、常见函数与常量 pi/e。
 * 不支持隐式乘法（如 2(3+1) 请写成 2*(3+1)）。
 */
object CalcExpr {

    data class EvalResult(val value: Double? = null, val error: String? = null) {
        val ok: Boolean get() = error == null
    }

    fun evaluate(input: String): EvalResult {
        if (input.isBlank()) return EvalResult(error = "请输入表达式")
        return try {
            val normalized = input
                .replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")
                .replace("π", "pi")
                .replace("％", "%")
            EvalResult(value = Parser(normalized).parse())
        } catch (e: Exception) {
            EvalResult(error = e.message ?: "表达式格式错误")
        }
    }

    private class Parser(src: String) {
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
                when (peek()) {
                    '*' -> { pos++; value *= power() }
                    '/' -> {
                        pos++
                        val divisor = power()
                        if (divisor == 0.0) fail("除数不能为 0")
                        value /= divisor
                    }
                    else -> return value
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
                val arg = expression()
                skipSpaces()
                expect(')')
                return function(name, arg)
            }
            return when (name) {
                "pi" -> kotlin.math.PI
                "e" -> kotlin.math.E
                else -> fail("未知常量或函数：$name")
            }
        }

        private fun function(name: String, x: Double): Double = when (name) {
            "sin" -> sin(x)
            "cos" -> cos(x)
            "tan" -> tan(x)
            "asin", "arcsin" -> asin(x)
            "acos", "arccos" -> acos(x)
            "atan", "arctan" -> atan(x)
            "sinh" -> sinh(x)
            "cosh" -> cosh(x)
            "tanh" -> tanh(x)
            "log" -> if (x <= 0) fail("log 参数必须大于 0") else log10(x)
            "ln" -> if (x <= 0) fail("ln 参数必须大于 0") else ln(x)
            "sqrt" -> if (x < 0) fail("sqrt 参数不能为负数") else sqrt(x)
            "abs" -> abs(x)
            "exp" -> exp(x)
            else -> fail("不支持的函数：$name")
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
