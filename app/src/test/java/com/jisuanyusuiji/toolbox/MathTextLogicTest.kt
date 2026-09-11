package com.jisuanyusuiji.toolbox

import com.jisuanyusuiji.toolbox.ui.components.prettifyMath
import com.jisuanyusuiji.toolbox.ui.components.readMathToken
import com.jisuanyusuiji.toolbox.ui.components.splitFraction
import com.jisuanyusuiji.toolbox.ui.components.stripWrapper
import com.jisuanyusuiji.toolbox.ui.components.topLevelPositions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** 公式排版组件（MathText）纯逻辑的单元测试。 */
class MathTextLogicTest {

    @Test
    fun prettify_replacesAsciiOperators() {
        assertEquals("x = 1", prettifyMath("x=1"))
        assertEquals("a≤b", prettifyMath("a<=b"))
        assertEquals("c≥d", prettifyMath("c>=d"))
        assertEquals("x≠y", prettifyMath("x!=y"))
        assertEquals("√(2)", prettifyMath("sqrt(2)"))
        assertEquals("π", prettifyMath("pi"))
        assertEquals("a·b", prettifyMath("a*b"))
    }

    @Test
    fun topLevelPositions_ignoresNested() {
        assertEquals(listOf(1), topLevelPositions("a=(b+c)/(d)", '='))
        assertEquals(emptyList<Int>(), topLevelPositions("(a=b)", '='))
        assertEquals(listOf(1, 7), topLevelPositions("a=b+c*d=e", '='))
    }

    @Test
    fun splitFraction_stacksSimpleFractions() {
        assertEquals("a" to "(1-q)", splitFraction("a/(1-q)"))
        assertEquals("pV" to "T", splitFraction("pV/T"))
        assertNotNull(splitFraction("(-b ± √(b^2-4ac))/(2a)"))
        assertNotNull(splitFraction("1/(2πi)"))
    }

    @Test
    fun splitFraction_rejectsAmbiguousFractions() {
        // 1/2mv^2 的 1/2 与后面因子连写，堆叠会改变含义，必须保持行内
        assertNull(splitFraction("1/2mv^2"))
        assertNull(splitFraction("x/2 + 1"))
        assertNull(splitFraction("a/b/c"))
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
}
