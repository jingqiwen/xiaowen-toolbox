package com.jisuanyusuiji.toolbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.jisuanyusuiji.toolbox.tools.calc.BaseConverterToolKt;
import com.jisuanyusuiji.toolbox.tools.calc.CalcExpr;
import com.jisuanyusuiji.toolbox.tools.calc.ComplexCalc;
import com.jisuanyusuiji.toolbox.tools.calc.RmbToolKt;
import com.jisuanyusuiji.toolbox.tools.extra.LunarCalendarToolKt;

import org.junit.Test;

import java.time.LocalDate;

public class LogicCheckTest {

    @Test
    public void rmbUpper() {
        assertEquals("壹佰贰拾叁万肆仟伍佰陆拾柒元捌角玖分", RmbToolKt.amountToRmbUpper("1234567.89"));
        assertEquals("壹亿零壹元整", RmbToolKt.amountToRmbUpper("100000001"));
        assertEquals("壹仟零壹元整", RmbToolKt.amountToRmbUpper("1001"));
        assertEquals("零元零伍分", RmbToolKt.amountToRmbUpper("0.05"));
        assertEquals("壹拾元整", RmbToolKt.amountToRmbUpper("10"));
    }

    @Test
    public void baseConversion() {
        assertEquals("11111111.1", BaseConverterToolKt.convertBase("255.5", 10, 2, 20));
        assertEquals("26.5", BaseConverterToolKt.convertBase("1A.8", 16, 10, 20));
        assertEquals("5.5", BaseConverterToolKt.convertBase("101.1", 2, 10, 20));
    }

    @Test
    public void calcExpression() {
        assertEquals(13.9, CalcExpr.INSTANCE.evaluate("2*(3+4)-10%", false).getValue(), 1e-9);
        assertEquals(18.0, CalcExpr.INSTANCE.evaluate("sqrt(16)+3!+2^3", false).getValue(), 1e-9);
    }

    @Test
    public void complexExpression() {
        assertEquals("11+2i", ComplexCalc.INSTANCE.evaluate("(1+2i)*(3-4i)").getValue().toString());
    }

    @Test
    public void ganzhi() {
        assertEquals("丙午", LunarCalendarToolKt.yearGanZhi(2026));
        assertEquals("马", LunarCalendarToolKt.zodiac(2026));
        assertTrue(!LunarCalendarToolKt.dayGanZhi(LocalDate.of(2000, 1, 1)).isBlank());
    }
}
