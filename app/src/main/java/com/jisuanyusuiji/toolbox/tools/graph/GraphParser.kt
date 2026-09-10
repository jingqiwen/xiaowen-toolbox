package com.jisuanyusuiji.toolbox.tools.graph

import kotlin.math.pow

/**
 * 本地函数表达式解析器，支持变量和常用数学函数。
 * 变量通过 variables 回调获取（x、y、t、theta、a、b、c、d 等）。
 */
class GraphParser(
    private val src: String,
    private val variables: (String) -> Double?
) {
    private val s = src.lowercase()
    private var pos = 0

    fun parse(): Double {
        val v = expression()
        skipSpaces()
        if (pos < s.length) fail("存在无法识别的字符")
        return v
    }

    private fun expression(): Double {
        var v = term()
        while (true) {
            skipSpaces()
            when (peek()) {
                '+' -> { pos++; v += term() }
                '-' -> { pos++; v -= term() }
                else -> return v
            }
        }
    }

    private fun term(): Double {
        var v = power()
        while (true) {
            skipSpaces()
            when (peek()) {
                '*' -> { pos++; v *= power() }
                '/' -> {
                    pos++
                    val d = power()
                    if (d == 0.0) throw IllegalArgumentException("除数不能为 0")
                    v /= d
                }
                else -> return v
            }
        }
    }

    private fun power(): Double {
        val base = unary()
        skipSpaces()
        if (peek() == '^') {
            pos++
            return base.pow(power())
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
        var v = primary()
        while (true) {
            skipSpaces()
            when (peek()) {
                '!' -> {
                    pos++
                    if (v < 0 || v != v.toLong().toDouble() || v > 170) throw IllegalArgumentException("阶乘参数无效")
                    var r = 1.0
                    for (i in 2..v.toInt()) r *= i
                    v = r
                }
                '%' -> { pos++; v /= 100.0 }
                else -> return v
            }
        }
    }

    private fun primary(): Double {
        skipSpaces()
        val c = peek() ?: throw IllegalArgumentException("表达式不完整")
        if (c == '(') {
            pos++
            val v = expression()
            skipSpaces()
            expect(')')
            return v
        }
        if (c.isDigit() || c == '.') return number()
        if (c.isLetter()) return identifier()
        throw IllegalArgumentException("意外的字符：$c")
    }

    private fun number(): Double {
        val start = pos
        while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
        if (pos < s.length && s[pos] == 'e') {
            val save = pos
            pos++
            if (pos < s.length && (s[pos] == '+' || s[pos] == '-')) pos++
            val eStart = pos
            while (pos < s.length && s[pos].isDigit()) pos++
            if (pos == eStart) pos = save
        }
        return s.substring(start, pos).toDoubleOrNull() ?: throw IllegalArgumentException("数字格式错误")
    }

    private fun identifier(): Double {
        val start = pos
        while (pos < s.length && (s[pos].isLetter() || s[pos] == '_')) pos++
        val name = s.substring(start, pos)
        skipSpaces()
        if (peek() == '(') {
            pos++
            val args = mutableListOf<Double>()
            skipSpaces()
            if (peek() != ')') {
                args.add(expression())
                skipSpaces()
                while (peek() == ',') {
                    pos++
                    args.add(expression())
                    skipSpaces()
                }
            }
            expect(')')
            return call(name, args)
        }
        return when (name) {
            "pi" -> Math.PI
            "e" -> Math.E
            "inf" -> Double.POSITIVE_INFINITY
            else -> variables(name) ?: throw IllegalArgumentException("未知变量：$name")
        }
    }

    private fun call(name: String, args: List<Double>): Double {
        fun one(): Double = args.getOrNull(0) ?: throw IllegalArgumentException("$name 需要参数")
        fun two(): Pair<Double, Double> = (args.getOrNull(0) ?: 0.0) to (args.getOrNull(1) ?: 0.0)
        return when (name) {
            "sin" -> kotlin.math.sin(one())
            "cos" -> kotlin.math.cos(one())
            "tan" -> kotlin.math.tan(one())
            "asin" -> kotlin.math.asin(one())
            "acos" -> kotlin.math.acos(one())
            "atan" -> kotlin.math.atan(one())
            "sinh" -> kotlin.math.sinh(one())
            "cosh" -> kotlin.math.cosh(one())
            "tanh" -> kotlin.math.tanh(one())
            "ln" -> kotlin.math.ln(one())
            "log" -> kotlin.math.log10(one())
            "log2" -> kotlin.math.ln(one()) / kotlin.math.ln(2.0)
            "sqrt" -> kotlin.math.sqrt(one())
            "cbrt" -> Math.cbrt(one())
            "abs" -> kotlin.math.abs(one())
            "exp" -> kotlin.math.exp(one())
            "floor" -> kotlin.math.floor(one())
            "ceil" -> kotlin.math.ceil(one())
            "round" -> kotlin.math.round(one())
            "sign" -> kotlin.math.sign(one())
            "min" -> { val (a, b) = two(); kotlin.math.min(a, b) }
            "max" -> { val (a, b) = two(); kotlin.math.max(a, b) }
            "pow" -> { val (a, b) = two(); a.pow(b) }
            "mod" -> { val (a, b) = two(); a % b }
            "atan2" -> { val (a, b) = two(); kotlin.math.atan2(a, b) }
            "if" -> { if (args.size >= 3) (if (args[0] != 0.0) args[1] else args[2]) else Double.NaN }
            else -> throw IllegalArgumentException("不支持的函数：$name")
        }
    }

    private fun peek(): Char? = if (pos < s.length) s[pos] else null

    private fun expect(c: Char) {
        skipSpaces()
        if (peek() != c) throw IllegalArgumentException("缺少 $c")
        pos++
    }

    private fun skipSpaces() {
        while (pos < s.length && s[pos].isWhitespace()) pos++
    }

    private fun fail(msg: String): Nothing = throw IllegalArgumentException(msg)
}
