package com.jisuanyusuiji.toolbox.tools.random

data class LotteryLot(
    val number: Int,
    val title: String,
    val level: String,
    val poem: List<String>,
    val interpretations: Map<String, String>
)

/**
 * 本地签库：参考《易经》64 卦与民间签诗风格，共 384 条（64 卦 × 6 爻）。
 * 内容为程序化生成的原创释义，供娱乐参考，不构成任何决策依据。
 */
object LotteryData {

    private val hexagrams = listOf(
        "乾为天", "坤为地", "水雷屯", "山水蒙", "水天需", "天水讼", "地水师", "水地比",
        "风天小畜", "天泽履", "地天泰", "天地否", "天火同人", "火天大有", "地山谦", "雷地豫",
        "泽雷随", "山风蛊", "地泽临", "风地观", "火雷噬嗑", "山火贲", "山地剥", "地雷复",
        "天雷无妄", "山天大畜", "山雷颐", "泽风大过", "坎为水", "离为火", "泽山咸", "雷风恒",
        "天山遁", "雷天大壮", "火地晋", "地火明夷", "风火家人", "火泽睽", "水山蹇", "雷水解",
        "山泽损", "风雷益", "泽天夬", "天风姤", "泽地萃", "地风升", "泽水困", "水风井",
        "泽火革", "火风鼎", "震为雷", "艮为山", "风山渐", "雷泽归妹", "雷火丰", "火山旅",
        "巽为风", "兑为泽", "风水涣", "水泽节", "风泽中孚", "雷山小过", "水火既济", "火水未济"
    )

    private val levels = listOf("上上签", "上签", "中签", "中签", "下签", "下下签")

    private val yaoNames = listOf("初爻", "二爻", "三爻", "四爻", "五爻", "上爻")

    private val poemA = listOf(
        "云开月出照山河", "春风送暖入庭柯", "一叶扁舟渡大江", "枯木逢春再发枝",
        "宝镜重磨分外明", "燕子归巢喜事临", "雨过天晴路自通", "锦上添花福自来"
    )
    private val poemB = listOf(
        "贵人指点迷津路", "凡事从容莫着急", "守正待时方得力", "进退之间细思量",
        "旧事已过莫回头", "新机初动宜把握", "心平气和百事顺", "一念之善福星随"
    )
    private val poemC = listOf(
        "莫向孤舟问渡头", "且将心事付东流", "若逢知己同携手", "自有前程不用愁",
        "花开花落自有时", "云卷云舒任由之", "静中观变知进退", "勤修德业待天时"
    )
    private val poemD = listOf(
        "万里鹏程从此始", "一朝得意莫忘形", "守得云开见月明", "前程似锦步步高",
        "诸事随缘免强求", "小心驶得万年船", "太平无事即是福", "否极泰来终有期"
    )

    private val careerTexts = listOf(
        "事业上宜稳中求进，先把眼前事情做扎实，机会自然会出现。",
        "工作上可能遇到新的调整或选择，建议多听取前辈意见，不要急着做决定。",
        "近期事业运势转好，适合主动争取项目或表达想法，容易得到认可。",
        "工作中需注意人际沟通，小事不要计较，避免因口舌引发不必要的是非。"
    )
    private val wealthTexts = listOf(
        "财运平稳，宜守不宜攻，避免冲动消费和高风险投入。",
        "近期有正财入账的机会，但偏财需谨慎，借贷之事要多留书面凭证。",
        "财源渐开，适合做长期规划与储蓄，不建议追逐短期暴利。",
        "求财需付出努力，脚踏实地可得回报；投机取巧反而容易破财。"
    )
    private val loveTexts = listOf(
        "感情上宜真诚沟通，把心里话说开，误会自然化解。",
        "单身者有机会遇到合适的人，但需慢慢了解，不宜一见倾心就投入全部。",
        "已有伴侣者要注意陪伴与体谅，多制造一些小惊喜，感情会更稳固。",
        "缘分之事不可强求，顺其自然反而更容易遇到对的人。"
    )
    private val healthTexts = listOf(
        "身体总体无恙，注意作息规律，少熬夜、少油腻即可。",
        "近期宜多休息，避免过度劳累，适当运动有助于舒缓压力。",
        "注意肠胃与颈肩问题，久坐后要起来活动，饮食宜清淡。",
        "心情对健康影响很大，保持乐观，别把小事放在心上。"
    )
    private val travelTexts = listOf(
        "出行总体顺利，出发前检查证件与车况即可。",
        "远行宜提前规划路线，留出充足时间，避免赶时间出岔子。",
        "出行路上可能遇到小插曲，保持耐心，以安全为第一。",
        "近期适合走动与拜访，外出容易遇到对你有帮助的人。"
    )
    private val overallTexts = listOf(
        "此签总体顺遂，凡事以和为贵，心存善念自有福报。",
        "此签提示宜静不宜动，先观察局势，等待合适时机再行动。",
        "此签有转机之意，过去的阻滞会慢慢化解，坚持正道即可。",
        "此签提醒谨慎行事，三思而后行，可避开许多麻烦。"
    )

    val all: List<LotteryLot> by lazy {
        val list = mutableListOf<LotteryLot>()
        var number = 1
        hexagrams.forEachIndexed { hIndex, hexName ->
            for (yao in 0 until 6) {
                val level = levels[(hIndex + yao * 2) % levels.size]
                val seed = hIndex * 6 + yao
                val poem = listOf(
                    poemA[seed % poemA.size],
                    poemB[(seed * 3 + 1) % poemB.size],
                    poemC[(seed * 5 + 2) % poemC.size],
                    poemD[(seed * 7 + 3) % poemD.size]
                )
                val interpretations = mapOf(
                    "综合" to overallTexts[(seed + hIndex) % overallTexts.size],
                    "事业" to careerTexts[(seed * 2 + yao) % careerTexts.size],
                    "财运" to wealthTexts[(seed * 3 + hIndex) % wealthTexts.size],
                    "感情" to loveTexts[(seed * 5 + yao) % loveTexts.size],
                    "健康" to healthTexts[(seed * 7 + hIndex + yao) % healthTexts.size],
                    "出行" to travelTexts[(seed * 11 + yao) % travelTexts.size]
                )
                list.add(
                    LotteryLot(
                        number = number++,
                        title = "第${number - 1}签 · $hexName · ${yaoNames[yao]}",
                        level = level,
                        poem = poem,
                        interpretations = interpretations
                    )
                )
            }
        }
        list
    }
}
