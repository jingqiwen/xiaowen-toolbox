package com.jisuanyusuiji.toolbox.tools.calc

/**
 * 简单的复数运算器：支持 a+bi 形式与 + - * / 括号运算。
 * 示例：(1+2i)*(3-4i)  =>  11+2i
 */
data class Complex(val re: Double, val im: Double) {
    operator fun plus(o: Complex) = Complex(re + o.re, im + o.im)
    operator fun minus(o: Complex) = Complex(re - o.re, im - o.im)
    operator fun times(o: Complex) = Complex(re * o.re - im * o.im, re * o.im + im * o.re)
    operator fun unaryMinus() = Complex(-re, -im)

    operator fun div(o: Complex): Complex {
        val den = o.re * o.re + o.im * o.im
        if (den == 0.0) throw IllegalArgumentException("复数除法分母为 0")
        return Complex((re * o.re + im * o.im) / den, (im * o.re - re * o.im) / den)
    }

    override fun toString(): String = ComplexCalc.formatComplex(re, im)
}

object ComplexCalc {

    data class Result(val value: Complex? = null, val error: String? = null)

    fun evaluate(input: String): Result {
        if (input.isBlank()) return Result(error = "请输入复数表达式，如 (1+2i)*(3-4i)")
        return try {
            Result(value = Parser(input.replace(" ", "")).parse())
        } catch (e: Exception) {
            Result(error = e.message ?: "表达式格式错误")
        }
    }

    fun formatComplex(re: Double, im: Double): String {
        fun f(x: Double): String {
            val v = if (absSmall(x)) 0.0 else x
            val s = if (v == v.toLong().toDouble()) v.toLong().toString()
            else String.format("%.4f", v).trimEnd('0').trimEnd('.')
            return s
        }
        return when {
            absSmall(re) && absSmall(im) -> "0"
            absSmall(re) -> "${f(im)}i"
            absSmall(im) -> f(re)
            im < 0 -> "${f(re)}-${f(-im)}i"
            else -> "${f(re)}+${f(im)}i"
        }
    }

    private fun absSmall(x: Double) = kotlin.math.abs(x) < 1e-10

    private class Parser(private val s: String) {
        private var pos = 0

        fun parse(): Complex {
            val v = expression()
            if (pos < s.length) fail("存在无法识别的字符")
            return v
        }

        private fun expression(): Complex {
            var v = term()
            while (true) {
                when (peek()) {
                    '+' -> { pos++; v += term() }
                    '-' -> { pos++; v -= term() }
                    else -> return v
                }
            }
        }

        private fun term(): Complex {
            var v = unary()
            while (true) {
                when (peek()) {
                    '*' -> { pos++; v *= unary() }
                    '/' -> { pos++; v /= unary() }
                    else -> return v
                }
            }
        }

        private fun unary(): Complex = when (peek()) {
            '+' -> { pos++; unary() }
            '-' -> { pos++; -unary() }
            else -> primary()
        }

        private fun primary(): Complex {
            if (pos >= s.length) fail("表达式不完整")
            if (peek() == '(') {
                pos++
                val v = expression()
                if (peek() != ')') fail("缺少右括号")
                pos++
                return v
            }
            if (peek() == 'i') {
                pos++
                return Complex(0.0, 1.0)
            }
            if (peek() == '.') return number()
            if (peek() == '-') { pos++; return -primary() }
            if (peek() == '+') { pos++; return primary() }
            if (peek()?.isDigit() == true) return number()
            fail("意外的字符：${peek()}")
            return Complex(0.0, 0.0)
        }

        private fun number(): Complex {
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
            val real = s.substring(start, pos).toDoubleOrNull() ?: fail("数字格式错误")
            if (peek() == 'i') {
                pos++
                return Complex(0.0, real)
            }
            return Complex(real, 0.0)
        }

        private fun peek(): Char? = if (pos < s.length) s[pos] else null

        private fun fail(message: String): Nothing = throw IllegalArgumentException(message)
    }
}
