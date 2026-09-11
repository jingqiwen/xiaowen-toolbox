package com.jisuanyusuiji.toolbox

import com.jisuanyusuiji.toolbox.ui.components.MathNode
import com.jisuanyusuiji.toolbox.ui.components.isPureNumber
import com.jisuanyusuiji.toolbox.ui.components.parseMathNodes
import com.jisuanyusuiji.toolbox.ui.components.plainText
import com.jisuanyusuiji.toolbox.ui.components.prettifyMath
import com.jisuanyusuiji.toolbox.ui.components.readMathToken
import com.jisuanyusuiji.toolbox.ui.components.stripWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 公式排版组件（MathText）纯逻辑的单元测试。 */
class MathTextLogicTest {

    private fun plain(raw: String): String = plainText(parseMathNodes(prettifyMath(raw)))

    @Test
    fun prettify_replacesAsciiOperators() {
        assertEquals("x = 1", prettifyMath("x=1"))
        assertEquals("a≤b", prettifyMath("a<=b"))
        assertEquals("c≥d", prettifyMath("c>=d"))
        assertEquals("x≠y", prettifyMath("x!=y"))
        assertEquals("√(2)", prettifyMath("sqrt(2)"))
        assertEquals("π", prettifyMath("pi"))
        assertEquals("a·b", prettifyMath("a*b"))
        assertEquals("n - m", prettifyMath("n-m"))
        assertEquals("x^2 - 1", prettifyMath("x^2-1"))
    }

    @Test
    fun combination_usesStackedSubSuperscript() {
        val nodes = parseMathNodes("C(n,m) = n!/[m!(n-m)!]")
        val indexed = nodes.filterIsInstance<MathNode.Indexed>()
        assertEquals(1, indexed.size)
        assertEquals("C", indexed[0].letter)
        assertEquals("n", plainText(indexed[0].sub))
        assertEquals("m", plainText(indexed[0].sup))
        // 不能退化成 C(n,m) 括号形式
        assertTrue(indexed.isNotEmpty())
    }

    @Test
    fun permutation_usesStackedSubSuperscript() {
        val nodes = parseMathNodes("A(n,m)")
        val indexed = nodes.filterIsInstance<MathNode.Indexed>()
        assertEquals(1, indexed.size)
        assertEquals("A", indexed[0].letter)
        assertEquals("n", plainText(indexed[0].sub))
        assertEquals("m", plainText(indexed[0].sup))
    }

    @Test
    fun fraction_withParenDenominator_isStacked() {
        val nodes = parseMathNodes("abc/(4S)")
        assertEquals(1, nodes.size)
        val frac = nodes[0] as MathNode.Frac
        assertEquals("abc", plainText(frac.num))
        assertEquals("(4S)", plainText(frac.den))
    }

    @Test
    fun fraction_halfCoefficient_stacksOnlyTheNumber() {
        // 1/2ab 在数学上表示 (1/2)·ab，不能堆成 1/(2ab)
        val nodes = parseMathNodes("S = 1/2ab·sinC")
        val frac = nodes.filterIsInstance<MathNode.Frac>().single()
        assertEquals("1", plainText(frac.num))
        assertEquals("2", plainText(frac.den))
        assertTrue(plainText(nodes).endsWith("ab·sinC"))
    }

    @Test
    fun fraction_groupedDenominator_keepsWholeGroup() {
        val nodes = parseMathNodes(prettifyMath("S = a_1(1-q^n)/(1-q) (q≠1)"))
        val frac = nodes.filterIsInstance<MathNode.Frac>().single()
        assertTrue(plainText(frac.num).startsWith("a_1"))
        assertEquals("(1 - q)", plainText(frac.den))
        // 后面的条件不能吞进分母
        assertTrue(plainText(nodes).contains("(q≠1)"))
    }

    @Test
    fun fraction_insideParenthesesWithFactor_keepsFullDenominator() {
        val nodes = parseMathNodes(prettifyMath("(1/2a)ln|(x-a)/(x+a)|+C"))
        val fracs = nodes.filterIsInstance<MathNode.Frac>()
        assertEquals(2, fracs.size)
        assertEquals("2a", plainText(fracs[0].den))
        assertEquals("(x + a)", plainText(fracs[1].den))
    }

    @Test
    fun fraction_chainedDivision_isNotStacked() {
        val nodes = parseMathNodes("a/b/c")
        assertTrue(nodes.filterIsInstance<MathNode.Frac>().isEmpty())
        assertEquals("a/b/c", plainText(nodes))
    }

    @Test
    fun fraction_pureNumbers_areStacked() {
        val nodes = parseMathNodes("w = 1/2(z+1/z)")
        val fracs = nodes.filterIsInstance<MathNode.Frac>()
        assertEquals("1", plainText(fracs[0].num))
        assertEquals("2", plainText(fracs[0].den))
        assertEquals("1", plainText(fracs[1].num))
        assertEquals("z", plainText(fracs[1].den))
    }

    @Test
    fun fraction_complexQuadratic_isStacked() {
        val nodes = parseMathNodes(prettifyMath("x = (-b ± √(b^2-4ac)) / (2a)"))
        val frac = nodes.filterIsInstance<MathNode.Frac>().single()
        assertEquals("(-b ± √(b^2 - 4ac))", plainText(frac.num))
        assertEquals("(2a)", plainText(frac.den))
    }

    @Test
    fun fraction_functionCallDenominator_isStacked() {
        val nodes = parseMathNodes(prettifyMath("P(A|B) = P(AB)/P(B)"))
        val frac = nodes.filterIsInstance<MathNode.Frac>().single()
        assertEquals("P(AB)", plainText(frac.num))
        assertEquals("P(B)", plainText(frac.den))
    }

    @Test
    fun fraction_trigDenominator_keepsFunctionCall() {
        val nodes = parseMathNodes(prettifyMath("n = sin((A+δ)/2)/sin(A/2)"))
        val fracs = nodes.filterIsInstance<MathNode.Frac>()
        assertTrue(fracs.any { plainText(it.den).startsWith("sin(A/2)") })
    }

    @Test
    fun fraction_differentialOperator_isNotSwallowingTheGroup() {
        val nodes = parseMathNodes(prettifyMath("f''(x) = d/dx(f'(x))"))
        assertTrue(nodes.filterIsInstance<MathNode.Frac>().isEmpty())
    }

    @Test
    fun readMathToken_readsGroupAndAtom() {
        val group = readMathToken("(-(x-μ)^2/(2σ^2))abc", 0)
        assertEquals("(-(x-μ)^2/(2σ^2))", group.first)
        assertEquals("abc", "(-(x-μ)^2/(2σ^2))abc".substring(group.second))

        val atom = readMathToken("n-k", 0)
        assertEquals("n", atom.first)
        assertEquals(1, atom.second)

        val signed = readMathToken("-11x", 0)
        assertEquals("-11", signed.first)
        assertEquals(3, signed.second)
    }

    @Test
    fun stripWrapper_removesOuterParens() {
        assertEquals("n-k", stripWrapper("(n-k)"))
        assertEquals("a", stripWrapper("a"))
        assertEquals("(a)(b)", stripWrapper("((a)(b))"))
    }

    @Test
    fun pureNumber_detection() {
        assertTrue(isPureNumber("12"))
        assertTrue(isPureNumber("2.5"))
        assertTrue(!isPureNumber("2a"))
    }
}
