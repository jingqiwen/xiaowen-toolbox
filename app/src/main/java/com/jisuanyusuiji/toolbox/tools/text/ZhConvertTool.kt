package com.jisuanyusuiji.toolbox.tools.text

import android.annotation.SuppressLint
import android.os.Build
import android.icu.text.Transliterator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.ui.components.CopyButton
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.ResultText
import com.jisuanyusuiji.toolbox.ui.components.SectionCard

/** 常用字简繁对照表（作为低版本系统的后备方案）。 */
private val S2T_PAIRS = listOf(
    "万" to "萬", "与" to "與", "业" to "業", "东" to "東", "严" to "嚴",
    "个" to "個", "们" to "們", "为" to "為", "习" to "習", "书" to "書",
    "买" to "買", "卖" to "賣", "争" to "爭", "产" to "產", "亲" to "親",
    "亿" to "億", "仅" to "僅", "从" to "從", "仓" to "倉", "仪" to "儀",
    "价" to "價", "众" to "眾", "优" to "優", "会" to "會", "传" to "傳",
    "伤" to "傷", "体" to "體", "佣" to "傭", "侠" to "俠", "侧" to "側",
    "侨" to "僑", "俭" to "儉", "债" to "債", "储" to "儲", "儿" to "兒",
    "兑" to "兌", "兰" to "蘭", "关" to "關", "兴" to "興", "养" to "養",
    "兽" to "獸", "内" to "內", "冈" to "岡", "册" to "冊", "写" to "寫",
    "军" to "軍", "农" to "農", "决" to "決", "况" to "況", "冻" to "凍",
    "净" to "淨", "凉" to "涼", "减" to "減", "几" to "幾", "凤" to "鳳",
    "凭" to "憑", "凯" to "凱", "击" to "擊", "凿" to "鑿", "刘" to "劉",
    "则" to "則", "刚" to "剛", "创" to "創", "删" to "刪", "剂" to "劑",
    "剑" to "劍", "剧" to "劇", "办" to "辦", "务" to "務", "动" to "動",
    "励" to "勵", "劲" to "勁", "劳" to "勞", "势" to "勢", "勋" to "勳",
    "区" to "區", "医" to "醫", "华" to "華", "协" to "協", "单" to "單",
    "卢" to "盧", "卤" to "鹵", "卫" to "衛", "厂" to "廠", "厅" to "廳",
    "历" to "歷", "厉" to "厲", "压" to "壓", "厌" to "厭", "厕" to "廁",
    "厢" to "廂", "厦" to "廈", "厨" to "廚", "县" to "縣", "发" to "發",
    "变" to "變", "号" to "號", "叹" to "嘆", "吓" to "嚇", "吗" to "嗎",
    "员" to "員", "听" to "聽", "启" to "啟", "吴" to "吳", "问" to "問",
    "响" to "響", "团" to "團", "园" to "園", "围" to "圍", "国" to "國",
    "图" to "圖", "圆" to "圓", "圣" to "聖", "场" to "場", "块" to "塊",
    "坚" to "堅", "坛" to "壇", "坝" to "壩", "坞" to "塢", "坟" to "墳",
    "坠" to "墜", "垄" to "壟", "垒" to "壘", "垦" to "墾", "垫" to "墊",
    "墙" to "牆", "壮" to "壯", "壳" to "殼", "壶" to "壺", "处" to "處",
    "备" to "備", "够" to "夠", "头" to "頭", "夹" to "夾", "夺" to "奪",
    "奖" to "獎", "妇" to "婦", "妈" to "媽", "学" to "學", "宝" to "寶",
    "实" to "實", "审" to "審", "宽" to "寬", "对" to "對", "寻" to "尋",
    "导" to "導", "寿" to "壽", "将" to "將", "尔" to "爾", "尘" to "塵",
    "尝" to "嘗", "尧" to "堯", "尽" to "盡", "层" to "層", "届" to "屆",
    "属" to "屬", "岁" to "歲", "岛" to "島", "岗" to "崗", "峡" to "峽",
    "师" to "師", "帐" to "帳", "带" to "帶", "帮" to "幫", "广" to "廣",
    "庄" to "莊", "庆" to "慶", "应" to "應", "庙" to "廟", "废" to "廢",
    "开" to "開", "异" to "異", "弃" to "棄", "张" to "張", "弹" to "彈",
    "强" to "強", "归" to "歸", "当" to "當", "录" to "錄", "彻" to "徹",
    "后" to "後", "术" to "術", "状" to "狀", "犹" to "猶", "独" to "獨",
    "狮" to "獅", "猎" to "獵", "献" to "獻", "现" to "現", "环" to "環",
    "电" to "電", "画" to "畫", "畅" to "暢", "疗" to "療", "盘" to "盤",
    "监" to "監", "盖" to "蓋", "盐" to "鹽", "积" to "積", "称" to "稱",
    "稳" to "穩", "穷" to "窮", "窃" to "竊", "笔" to "筆", "简" to "簡",
    "类" to "類", "粮" to "糧", "级" to "級", "纪" to "紀", "纳" to "納",
    "纸" to "紙", "线" to "線", "组" to "組", "细" to "細", "终" to "終",
    "结" to "結", "给" to "給", "统" to "統", "绩" to "績", "续" to "續",
    "绳" to "繩", "维" to "維", "综" to "綜", "绿" to "綠", "编" to "編",
    "缘" to "緣", "经" to "經", "网" to "網", "罗" to "羅", "罚" to "罰",
    "义" to "義", "聪" to "聰", "联" to "聯", "职" to "職", "胜" to "勝",
    "脑" to "腦", "脚" to "腳", "脸" to "臉", "举" to "舉", "乐" to "樂",
    "乔" to "喬", "乌" to "烏", "乡" to "鄉", "亏" to "虧", "云" to "雲",
    "亚" to "亞", "亩" to "畝", "丰" to "豐", "长" to "長", "门" to "門",
    "闭" to "閉", "闯" to "闖", "闻" to "聞", "闲" to "閒", "间" to "間",
    "闷" to "悶", "闹" to "鬧", "队" to "隊", "阳" to "陽", "阴" to "陰",
    "阵" to "陣", "阶" to "階", "际" to "際", "陆" to "陸", "陈" to "陳",
    "险" to "險", "难" to "難", "雾" to "霧", "静" to "靜", "项" to "項",
    "顺" to "順", "须" to "須", "顾" to "顧", "顿" to "頓", "预" to "預",
    "领" to "領", "频" to "頻", "颗" to "顆", "题" to "題", "颜" to "顏",
    "额" to "額", "风" to "風", "飞" to "飛", "饭" to "飯", "饮" to "飲",
    "饱" to "飽", "饰" to "飾", "饺" to "餃", "饼" to "餅", "饿" to "餓",
    "馆" to "館", "马" to "馬", "驰" to "馳", "驱" to "驅", "驴" to "驢",
    "驶" to "駛", "驼" to "駝", "驾" to "駕", "骄" to "驕", "验" to "驗",
    "骑" to "騎", "骗" to "騙", "鱼" to "魚", "鲁" to "魯", "鲜" to "鮮",
    "鸟" to "鳥", "鸡" to "雞", "鸭" to "鴨", "鸣" to "鳴", "鸿" to "鴻",
    "鹅" to "鵝", "鹏" to "鵬", "龙" to "龍", "龟" to "龜", "车" to "車",
    "转" to "轉", "轮" to "輪", "软" to "軟", "较" to "較", "轻" to "輕",
    "载" to "載", "辆" to "輛", "输" to "輸", "达" to "達", "迁" to "遷",
    "远" to "遠", "违" to "違", "连" to "連", "进" to "進", "运" to "運",
    "还" to "還", "这" to "這", "选" to "選", "递" to "遞", "遗" to "遺",
    "邮" to "郵", "邓" to "鄧", "郑" to "鄭", "邻" to "鄰", "针" to "針",
    "钉" to "釘", "钢" to "鋼", "钥" to "鑰", "钱" to "錢", "铁" to "鐵",
    "铅" to "鉛", "铜" to "銅", "银" to "銀", "铸" to "鑄", "铺" to "鋪",
    "链" to "鏈", "销" to "銷", "锁" to "鎖", "锅" to "鍋", "锋" to "鋒",
    "错" to "錯", "键" to "鍵", "镇" to "鎮", "镜" to "鏡", "页" to "頁",
    "顶" to "頂", "显" to "顯", "顽" to "頑", "颗" to "顆", "闹" to "鬧"
)

private val S2T: Map<String, String> = S2T_PAIRS.toMap()
private val T2S: Map<String, String> = S2T_PAIRS.associate { (s, t) -> t to s }

@SuppressLint("NewApi")
fun convertSimplifiedTraditional(text: String, toTraditional: Boolean): String {
    // Android 10+ 优先使用系统 ICU 完整转换；低版本使用常用字表
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            val id = if (toTraditional) "Simplified-Traditional" else "Traditional-Simplified"
            return Transliterator.getInstance(id).transliterate(text)
        } catch (_: Exception) {
            // 个别设备 ICU 数据缺失时回退到字表
        }
    }
    val map: Map<String, String> = if (toTraditional) S2T else T2S
    val sb = StringBuilder(text.length)
    for (c in text) {
        sb.append(map[c.toString()] ?: c)
    }
    return sb.toString()
}

@Composable
fun ZhConvertTool() {
    var input by remember { mutableStateOf("简体中文转换工具，支持常用字。") }
    var output by remember { mutableStateOf("") }
    var lastMode by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "简体 ↔ 繁体") {
            LabeledField(input, { input = it }, "输入文本", singleLine = false, minLines = 5)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = {
                    output = convertSimplifiedTraditional(input, true)
                    lastMode = "简体 → 繁体"
                }, modifier = Modifier.weight(1f)) { Text("转为繁体") }
                OutlinedButton(onClick = {
                    output = convertSimplifiedTraditional(input, false)
                    lastMode = "繁体 → 简体"
                }, modifier = Modifier.weight(1f)) { Text("转为简体") }
            }
        }
        if (output.isNotBlank()) {
            SectionCard(title = "结果（$lastMode）") {
                ResultText(output)
                Spacer(Modifier.height(10.dp))
                CopyButton(output)
            }
            Text(
                "说明：Android 10+ 使用系统 ICU 完整字库；低版本使用内置常用字表，多音字/语境字可能不完整。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
