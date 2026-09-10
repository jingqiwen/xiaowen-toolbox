package com.jisuanyusuiji.toolbox.tools.probability

data class ProbabilityFormula(
    val category: String,
    val name: String,
    val expression: String,
    val note: String
)

object ProbabilityData {
    val all: List<ProbabilityFormula> = listOf(
        // 基础概率
        ProbabilityFormula("基础概率", "古典概型", "P(A) = m / n", "样本空间有限且等可能，m 为有利结果数"),
        ProbabilityFormula("基础概率", "几何概型", "P(A) = 测度(A) / 测度(Ω)", "用长度/面积/体积之比计算"),
        ProbabilityFormula("基础概率", "概率公理", "0 ≤ P(A) ≤ 1，P(Ω) = 1", "概率的基本性质"),
        ProbabilityFormula("基础概率", "加法公式", "P(A∪B) = P(A) + P(B) - P(AB)", "任意两个事件"),
        ProbabilityFormula("基础概率", "对立事件", "P(Ā) = 1 - P(A)", "A 不发生"),
        ProbabilityFormula("基础概率", "互斥事件", "P(A∪B) = P(A) + P(B)", "A、B 不能同时发生"),
        ProbabilityFormula("基础概率", "独立事件", "P(AB) = P(A)P(B)", "相互独立"),
        ProbabilityFormula("基础概率", "条件概率", "P(A|B) = P(AB) / P(B)", "P(B) > 0"),
        ProbabilityFormula("基础概率", "乘法公式", "P(AB) = P(A)P(B|A) = P(B)P(A|B)", "计算积事件概率"),
        ProbabilityFormula("基础概率", "全概率公式", "P(A) = Σ P(Bᵢ)P(A|Bᵢ)", "Bᵢ 为完备事件组"),
        ProbabilityFormula("基础概率", "贝叶斯公式", "P(Bᵢ|A) = P(Bᵢ)P(A|Bᵢ) / ΣP(Bⱼ)P(A|Bⱼ)", "由结果反推原因"),
        ProbabilityFormula("基础概率", "伯努利试验", "只有成功/失败两种结果，重复 n 次独立进行", "二项分布的前置模型"),
        ProbabilityFormula("基础概率", "排列数", "A(n,m) = n! / (n-m)!", "有序选取"),
        ProbabilityFormula("基础概率", "组合数", "C(n,m) = n! / [m!(n-m)!]", "无序选取"),
        ProbabilityFormula("基础概率", "二项式系数性质", "C(n,m) = C(n,n-m)，ΣC(n,k) = 2ⁿ", "组合恒等式"),

        // 随机变量
        ProbabilityFormula("随机变量", "分布函数", "F(x) = P(X ≤ x)", "单调不减、右连续"),
        ProbabilityFormula("随机变量", "离散型分布律", "pₖ = P(X = xₖ)，Σpₖ = 1", "概率质量函数"),
        ProbabilityFormula("随机变量", "概率密度", "f(x) ≥ 0，∫f(x)dx = 1", "连续型随机变量"),
        ProbabilityFormula("随机变量", "密度与分布函数", "F(x) = ∫₋∞ˣ f(t)dt，F'(x) = f(x)", "连续型关系"),
        ProbabilityFormula("随机变量", "离散期望", "E(X) = Σ xₖpₖ", "加权平均"),
        ProbabilityFormula("随机变量", "连续期望", "E(X) = ∫ x f(x) dx", "连续型期望"),
        ProbabilityFormula("随机变量", "期望线性性质", "E(aX+bY) = aE(X) + bE(Y)", "不需要独立性"),
        ProbabilityFormula("随机变量", "方差", "D(X) = E[(X-E(X))²] = E(X²) - [E(X)]²", "离散程度"),
        ProbabilityFormula("随机变量", "方差性质", "D(aX+b) = a²D(X)", "常数不影响方差"),
        ProbabilityFormula("随机变量", "独立和方差", "D(X±Y) = D(X) + D(Y)", "X、Y 独立时"),
        ProbabilityFormula("随机变量", "标准差", "σ(X) = √D(X)", "与 X 同量纲"),
        ProbabilityFormula("随机变量", "协方差", "Cov(X,Y) = E(XY) - E(X)E(Y)", "衡量线性相关"),
        ProbabilityFormula("随机变量", "相关系数", "ρ = Cov(X,Y) / [σ(X)σ(Y)]", "|ρ| ≤ 1"),
        ProbabilityFormula("随机变量", "切比雪夫不等式", "P(|X-μ| ≥ ε) ≤ σ² / ε²", "任意分布下估计尾部概率"),
        ProbabilityFormula("随机变量", "k 阶原点矩", "E(Xᵏ)", "k=1 即期望"),
        ProbabilityFormula("随机变量", "k 阶中心矩", "E[(X-E(X))ᵏ]", "k=2 即方差"),
        ProbabilityFormula("随机变量", "偏度", "Sk = E[(X-μ)³] / σ³", "分布的不对称程度"),
        ProbabilityFormula("随机变量", "峰度", "Ku = E[(X-μ)⁴] / σ⁴", "正态分布峰度为 3"),

        // 常见分布
        ProbabilityFormula("常见分布", "0-1 分布", "P(X=1)=p，P(X=0)=1-p；E=p，D=p(1-p)", "一次伯努利试验"),
        ProbabilityFormula("常见分布", "二项分布 B(n,p)", "P(X=k)=C(n,k)pᵏ(1-p)ⁿ⁻ᵏ；E=np，D=np(1-p)", "n 次独立重复试验成功次数"),
        ProbabilityFormula("常见分布", "泊松分布 P(λ)", "P(X=k)=λᵏe⁻λ/k!；E=λ，D=λ", "稀有事件计数"),
        ProbabilityFormula("常见分布", "几何分布 G(p)", "P(X=k)=(1-p)ᵏ⁻¹p；E=1/p，D=(1-p)/p²", "首次成功所需试验次数"),
        ProbabilityFormula("常见分布", "超几何分布", "P(X=k)=C(M,k)C(N-M,n-k)/C(N,n)", "不放回抽样"),
        ProbabilityFormula("常见分布", "均匀分布 U(a,b)", "f(x)=1/(b-a)；E=(a+b)/2，D=(b-a)²/12", "等可能区间"),
        ProbabilityFormula("常见分布", "指数分布 Exp(λ)", "f(x)=λe⁻λˣ；E=1/λ，D=1/λ²", "等待时间"),
        ProbabilityFormula("常见分布", "正态分布 N(μ,σ²)", "f(x)=1/(σ√(2π))e^(-(x-μ)²/2σ²)", "钟形曲线"),
        ProbabilityFormula("常见分布", "标准正态分布", "φ(x)=1/√(2π)e^(-x²/2)；Φ(x)=∫φ", "μ=0，σ=1"),
        ProbabilityFormula("常见分布", "正态标准化", "Z = (X-μ)/σ ~ N(0,1)", "查表与计算核心"),
        ProbabilityFormula("常见分布", "正态线性变换", "aX+b ~ N(aμ+b, a²σ²)", "正态分布的可加性"),
        ProbabilityFormula("常见分布", "卡方分布 χ²(n)", "n 个独立标准正态平方和；E=n，D=2n", "用于方差检验"),
        ProbabilityFormula("常见分布", "t 分布 t(n)", "Z/√(χ²(n)/n)；E=0（n>1）", "小样本均值检验"),
        ProbabilityFormula("常见分布", "F 分布 F(m,n)", "(χ²(m)/m)/(χ²(n)/n)", "方差比检验"),
        ProbabilityFormula("常见分布", "对数正态分布", "lnX ~ N(μ,σ²)", "乘性随机因素"),
        ProbabilityFormula("常见分布", "伽马分布 Ga(α,β)", "f(x)=βᵅxᵅ⁻¹e⁻ᵝˣ/Γ(α)；E=α/β", "指数分布的推广"),
        ProbabilityFormula("常见分布", "贝塔分布 Be(a,b)", "定义在 [0,1] 上的分布；E=a/(a+b)", "比例建模"),
        ProbabilityFormula("常见分布", "多项分布", "n 次试验落入 k 类的联合概率", "二项分布推广"),

        // 极限定理
        ProbabilityFormula("极限定理", "大数定律", "样本均值依概率收敛于期望 μ", "频率稳定性的理论依据"),
        ProbabilityFormula("极限定理", "中心极限定理", "(ΣXᵢ - nμ)/(σ√n) → N(0,1)", "独立同分布和的近似正态"),
        ProbabilityFormula("极限定理", "棣莫弗-拉普拉斯", "二项分布当 n 很大时近似正态", "CLT 的特例"),
        ProbabilityFormula("极限定理", "正态近似条件", "np ≥ 5 且 n(1-p) ≥ 5", "二项分布近似判断"),

        // 参数估计
        ProbabilityFormula("参数估计", "样本均值", "x̄ = (1/n)Σxᵢ；E(x̄)=μ", "μ 的无偏估计"),
        ProbabilityFormula("参数估计", "样本方差", "S² = Σ(xᵢ-x̄)²/(n-1)；E(S²)=σ²", "无偏估计"),
        ProbabilityFormula("参数估计", "矩估计", "用样本矩估计总体矩", "简单但可能不唯一"),
        ProbabilityFormula("参数估计", "极大似然估计", "L(θ)=Πf(xᵢ;θ)，取 lnL 最大", "常用点估计方法"),
        ProbabilityFormula("参数估计", "标准误", "SE = σ/√n（或 S/√n）", "样本均值的标准差"),
        ProbabilityFormula("参数估计", "均值置信区间（σ 已知）", "x̄ ± z_{α/2}·σ/√n", "正态总体"),
        ProbabilityFormula("参数估计", "均值置信区间（σ 未知）", "x̄ ± t_{α/2}(n-1)·S/√n", "小样本 t 分布"),
        ProbabilityFormula("参数估计", "比例置信区间", "p̂ ± z_{α/2}√(p̂(1-p̂)/n)", "大样本比例"),

        // 假设检验
        ProbabilityFormula("假设检验", "原假设与备择假设", "H₀ 与 H₁ 互斥", "检验的出发点"),
        ProbabilityFormula("假设检验", "第一类错误", "H₀ 为真却拒绝，概率 α", "弃真"),
        ProbabilityFormula("假设检验", "第二类错误", "H₀ 为假却接受，概率 β", "取伪，1-β 为功效"),
        ProbabilityFormula("假设检验", "Z 检验统计量", "Z = (x̄-μ₀)/(σ/√n)", "σ 已知或大样本"),
        ProbabilityFormula("假设检验", "t 检验统计量", "t = (x̄-μ₀)/(S/√n)", "σ 未知小样本"),
        ProbabilityFormula("假设检验", "卡方检验统计量", "χ² = Σ(观测-期望)²/期望", "拟合优度/独立性检验"),
        ProbabilityFormula("假设检验", "F 检验统计量", "F = S₁²/S₂²", "两正态总体方差比较"),
        ProbabilityFormula("假设检验", "p 值", "H₀ 成立时观测到更极端结果的概率", "p < α 时拒绝 H₀"),

        // 回归与相关
        ProbabilityFormula("回归与相关", "样本相关系数", "r = Σ(xᵢ-x̄)(yᵢ-ȳ) / √[Σ(xᵢ-x̄)²Σ(yᵢ-ȳ)²]", "|r| ≤ 1"),
        ProbabilityFormula("回归与相关", "一元线性回归", "ŷ = a + bx", "最小二乘拟合"),
        ProbabilityFormula("回归与相关", "回归系数", "b = Σ(xᵢ-x̄)(yᵢ-ȳ)/Σ(xᵢ-x̄)²", "斜率估计"),
        ProbabilityFormula("回归与相关", "截距", "a = ȳ - bx̄", "回归直线过样本中心"),
        ProbabilityFormula("回归与相关", "决定系数", "R² = 1 - SSE/SST", "解释变异的比例，越接近 1 拟合越好"),
        ProbabilityFormula("回归与相关", "残差", "eᵢ = yᵢ - ŷᵢ", "实际值与预测值之差"),

        // 运算性质
        ProbabilityFormula("运算性质", "期望平方关系", "E(X²) = D(X) + [E(X)]²", "计算方差的常用变形"),
        ProbabilityFormula("运算性质", "随机变量函数期望", "E[g(X)] = Σg(xₖ)pₖ 或 ∫g(x)f(x)dx", "LOTUS 公式"),
        ProbabilityFormula("运算性质", "独立乘积期望", "E(XY) = E(X)E(Y)", "X、Y 独立"),
        ProbabilityFormula("运算性质", "泊松可加性", "P(λ₁)+P(λ₂) = P(λ₁+λ₂)", "独立泊松变量之和"),
        ProbabilityFormula("运算性质", "正态可加性", "N(μ₁,σ₁²)+N(μ₂,σ₂²) = N(μ₁+μ₂,σ₁²+σ₂²)", "独立正态变量之和"),
        ProbabilityFormula("运算性质", "二项可加性", "B(n₁,p)+B(n₂,p) = B(n₁+n₂,p)", "相同成功概率")
    )
}
