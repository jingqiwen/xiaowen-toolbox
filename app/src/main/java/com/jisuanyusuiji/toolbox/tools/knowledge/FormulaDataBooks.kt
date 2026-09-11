package com.jisuanyusuiji.toolbox.tools.knowledge

/**
 * 大学教材公式总入口。
 *
 * 按 8 本教材分册整理，每个分册按章节归类，便于在“数学物理公式查询”里
 * 先按教材筛选、再按章节筛选：
 *  - 《高等数学（上）》《高等数学（下）》 → FormulaBooksCalculus.kt
 *  - 《线性代数》《概率论与数理统计》    → FormulaBooksAlgebra.kt
 *  - 《复变函数与积分变换》              → FormulaBooksComplex.kt
 *  - 《大学物理（上）》《大学物理（下）》 → FormulaBooksPhysics.kt
 *  - 《大学物理实验》                    → FormulaBooksPhysicsLab.kt
 */
object FormulaDataBooks {
    val all: List<FormulaItem> =
        FormulaBooksCalculus.all +
            FormulaBooksAlgebra.all +
            FormulaBooksComplex.all +
            FormulaBooksPhysics.all +
            FormulaBooksPhysicsLab.all
}
