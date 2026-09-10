package com.jisuanyusuiji.toolbox.data

import androidx.compose.runtime.Composable
import com.jisuanyusuiji.toolbox.R
import com.jisuanyusuiji.toolbox.tools.calc.AnnuityTool
import com.jisuanyusuiji.toolbox.tools.calc.AsciiTool
import com.jisuanyusuiji.toolbox.tools.calc.Base64Tool
import com.jisuanyusuiji.toolbox.tools.calc.BaseConverterTool
import com.jisuanyusuiji.toolbox.tools.calc.BasicCalculatorTool
import com.jisuanyusuiji.toolbox.tools.calc.CasioCalculatorTool
import com.jisuanyusuiji.toolbox.tools.calc.ColorPickerTool
import com.jisuanyusuiji.toolbox.tools.calc.DateTimeCalculatorTool
import com.jisuanyusuiji.toolbox.tools.calc.DepositInterestTool
import com.jisuanyusuiji.toolbox.tools.calc.EquationTool
import com.jisuanyusuiji.toolbox.tools.calc.FractionTool
import com.jisuanyusuiji.toolbox.tools.calc.GeometryTool
import com.jisuanyusuiji.toolbox.tools.calc.GeometryProTool
import com.jisuanyusuiji.toolbox.tools.calc.HashTool
import com.jisuanyusuiji.toolbox.tools.calc.IpSubnetTool
import com.jisuanyusuiji.toolbox.tools.calc.LedResistorTool
import com.jisuanyusuiji.toolbox.tools.calc.MatrixTool
import com.jisuanyusuiji.toolbox.tools.calc.MortgageTool
import com.jisuanyusuiji.toolbox.tools.calc.RatioTool
import com.jisuanyusuiji.toolbox.tools.calc.ResistorColorTool
import com.jisuanyusuiji.toolbox.tools.calc.RmbTool
import com.jisuanyusuiji.toolbox.tools.calc.ScientificCalculatorTool
import com.jisuanyusuiji.toolbox.tools.calc.UnitConverterTool
import com.jisuanyusuiji.toolbox.tools.calc.UrlCodecTool
import com.jisuanyusuiji.toolbox.tools.extra.AaSplitTool
import com.jisuanyusuiji.toolbox.tools.extra.BmiTool
import com.jisuanyusuiji.toolbox.tools.extra.CompassTool
import com.jisuanyusuiji.toolbox.tools.extra.FlashlightTool
import com.jisuanyusuiji.toolbox.tools.extra.FuelConsumptionTool
import com.jisuanyusuiji.toolbox.tools.extra.GanzhiTool
import com.jisuanyusuiji.toolbox.tools.extra.IdPhotoTool
import com.jisuanyusuiji.toolbox.tools.extra.ImageCompressTool
import com.jisuanyusuiji.toolbox.tools.extra.ImageCropTool
import com.jisuanyusuiji.toolbox.tools.extra.ImageFormatConvertTool
import com.jisuanyusuiji.toolbox.tools.extra.ImageResizeTool
import com.jisuanyusuiji.toolbox.tools.extra.ImageStitchTool
import com.jisuanyusuiji.toolbox.tools.extra.LongImageToPdfTool
import com.jisuanyusuiji.toolbox.tools.extra.LunarCalendarTool
import com.jisuanyusuiji.toolbox.tools.extra.MirrorTool
import com.jisuanyusuiji.toolbox.tools.extra.NotesTool
import com.jisuanyusuiji.toolbox.tools.extra.PdfToLongImageTool
import com.jisuanyusuiji.toolbox.tools.extra.QrCodeTool
import com.jisuanyusuiji.toolbox.tools.extra.RecorderTool
import com.jisuanyusuiji.toolbox.tools.extra.VideoEditTool
import com.jisuanyusuiji.toolbox.tools.extra.VideoToAudioTool
import com.jisuanyusuiji.toolbox.tools.extra.VideoToGifTool
import com.jisuanyusuiji.toolbox.tools.extra.WordToPdfTool
import com.jisuanyusuiji.toolbox.tools.graph.FunctionGraphTool
import com.jisuanyusuiji.toolbox.tools.graph.Surface3DTool
import com.jisuanyusuiji.toolbox.tools.probability.ProbabilityTool
import com.jisuanyusuiji.toolbox.tools.vocab.VocabTool
import com.jisuanyusuiji.toolbox.tools.knowledge.CarLogoTool
import com.jisuanyusuiji.toolbox.tools.knowledge.FormulaTool
import com.jisuanyusuiji.toolbox.tools.knowledge.ShortcutTool
import com.jisuanyusuiji.toolbox.tools.random.CoinFlipTool
import com.jisuanyusuiji.toolbox.tools.random.DiceTool
import com.jisuanyusuiji.toolbox.tools.random.GachaTool
import com.jisuanyusuiji.toolbox.tools.random.ListShuffleTool
import com.jisuanyusuiji.toolbox.tools.random.LotteryTool
import com.jisuanyusuiji.toolbox.tools.random.LuckyWheelTool
import com.jisuanyusuiji.toolbox.tools.random.MultiSelectTool
import com.jisuanyusuiji.toolbox.tools.random.RandomNumberTool
import com.jisuanyusuiji.toolbox.tools.random.RandomPickerTool
import com.jisuanyusuiji.toolbox.tools.random.SlotMachineTool
import com.jisuanyusuiji.toolbox.tools.text.CaseConvertTool
import com.jisuanyusuiji.toolbox.tools.text.FindReplaceTool
import com.jisuanyusuiji.toolbox.tools.text.PasswordTool
import com.jisuanyusuiji.toolbox.tools.text.SplitMergeTool
import com.jisuanyusuiji.toolbox.tools.text.TextCountTool
import com.jisuanyusuiji.toolbox.tools.text.TextDedupeTool
import com.jisuanyusuiji.toolbox.tools.text.TextSortTool
import com.jisuanyusuiji.toolbox.tools.text.WhitespaceTool
import com.jisuanyusuiji.toolbox.tools.text.ZhConvertTool

enum class ToolCategory(val title: String) {
    RANDOM("🎲 随机工具"),
    MEDIA("🎬 影音与图像"),
    CALC("🧮 计算与转换"),
    GRAPH("📈 函数绘图"),
    TEXT("📝 文本与编码"),
    KNOWLEDGE("📚 图鉴查询"),
    EXTRA("📦 实用工具")
}

class ToolDef(
    val id: String,
    val name: String,
    val desc: String,
    val icon: String,
    val category: ToolCategory,
    val iconRes: Int? = null,
    val screen: @Composable () -> Unit
)

object ToolRegistry {

    val all: List<ToolDef> = listOf(
        // ---------- 随机工具 ----------
        ToolDef("dice", "多面骰子", "D4~D100、多骰同掷、优势/劣势、自定义面数", "🎲", ToolCategory.RANDOM) { DiceTool() },
        ToolDef("wheel", "幸运转盘", "自定义条目与权重、抽中移除、模板保存", "🎡", ToolCategory.RANDOM) { LuckyWheelTool() },
        ToolDef("picker", "随机点名器", "批量名单、可/不可重复抽取、记录导出", "🎯", ToolCategory.RANDOM) { RandomPickerTool() },
        ToolDef("random_number", "随机数生成器", "自定义范围、整数/小数、批量生成", "🔢", ToolCategory.RANDOM) { RandomNumberTool() },
        ToolDef("coin", "抛硬币模拟器", "正反面动画、连续抛掷与统计", "🪙", ToolCategory.RANDOM) { CoinFlipTool() },
        ToolDef("lottery", "抽签筒", "吉/凶/空签分类、摇签动画、签库保存", "🏮", ToolCategory.RANDOM) { LotteryTool() },
        ToolDef("shuffle", "列表打乱器", "多行文本一键打乱、随机分组排序", "🔀", ToolCategory.RANDOM) { ListShuffleTool() },
        ToolDef("multi_select", "多项随机选择", "输入多个选项，一次选出 N 个结果", "☑️", ToolCategory.RANDOM) { MultiSelectTool() },
        ToolDef("gacha", "抽卡模拟器", "自定义卡池概率、本地配置、抽卡统计", "🃏", ToolCategory.RANDOM) { GachaTool() },
        ToolDef("slot_machine", "老虎机", "三个相同才停，符号样本空间可编辑", "🎰", ToolCategory.RANDOM) { SlotMachineTool() },

        // ---------- 计算与转换 ----------
        ToolDef("basic_calc", "基础计算器", "四则运算、括号、百分号、计算历史", "🧮", ToolCategory.CALC) { BasicCalculatorTool() },
        ToolDef("scientific_calc", "科学计算器", "三角函数、对数、指数、根号、阶乘、复数", "📐", ToolCategory.CALC) { ScientificCalculatorTool() },
        ToolDef("casio_calc", "卡西欧风格计算器", "仿 CASIO 键盘布局与显示屏风格", "🖩", ToolCategory.CALC, R.drawable.ic_tool_casio) { CasioCalculatorTool() },
        ToolDef("unit_converter", "通用单位换算器", "长度、重量、面积、体积、速度、压力、功率、能量、温度", "📏", ToolCategory.CALC) { UnitConverterTool() },
        ToolDef("base_converter", "进制转换工具", "二/八/十/十六进制互转，支持小数", "2️⃣", ToolCategory.CALC) { BaseConverterTool() },
        ToolDef("date_calc", "日期时间计算器", "日期间隔、N 天前后、工作日、倒计时", "📅", ToolCategory.CALC) { DateTimeCalculatorTool() },
        ToolDef("deposit", "金融存款利息", "定期/活期利息、单利复利", "🏦", ToolCategory.CALC) { DepositInterestTool() },
        ToolDef("mortgage", "房贷计算器", "等额本息 / 等额本金", "🏠", ToolCategory.CALC) { MortgageTool() },
        ToolDef("annuity", "复利年金计算器", "复利终值、年金终值、年金现值", "📈", ToolCategory.CALC) { AnnuityTool() },
        ToolDef("matrix", "矩阵计算器", "二/三阶矩阵加减乘、行列式、求逆", "🔲", ToolCategory.CALC) { MatrixTool() },
        ToolDef("equation", "方程求解器", "一元一次、一元二次方程", "➗", ToolCategory.CALC) { EquationTool() },
        ToolDef("fraction", "分数计算器", "分数四则运算、约分、假分数/带分数", "½", ToolCategory.CALC) { FractionTool() },
        ToolDef("geometry", "几何计算器", "18 种平面/立体图形，面积体积内切圆外接圆", "🔺", ToolCategory.CALC) { GeometryProTool() },
        ToolDef("ratio", "比例计算器", "A:B=C:D 求解未知项", "⚖️", ToolCategory.CALC) { RatioTool() },
        ToolDef("resistor_color", "电阻色环计算器", "色环读阻值、阻值反查色环", "🌈", ToolCategory.CALC) { ResistorColorTool() },
        ToolDef("led_resistor", "LED 限流电阻", "计算限流电阻与功率", "💡", ToolCategory.CALC) { LedResistorTool() },
        ToolDef("ip_subnet", "IP 子网计算器", "子网掩码、网段、可用主机本地计算", "🌐", ToolCategory.CALC) { IpSubnetTool() },
        ToolDef("hash", "哈希计算器", "本地计算 MD5、SHA1、SHA256", "#️⃣", ToolCategory.CALC) { HashTool() },
        ToolDef("ascii", "ASCII 码互查", "字符 ↔ ASCII 码、常用码表", "🔤", ToolCategory.CALC) { AsciiTool() },
        ToolDef("rmb", "数字转财务大写", "阿拉伯数字转人民币大写金额", "💴", ToolCategory.CALC) { RmbTool() },

        // ---------- 文本与编码 ----------
        ToolDef("base64", "Base64 编码/解码", "本地文本 Base64 互转", "6️⃣4️⃣", ToolCategory.TEXT) { Base64Tool() },
        ToolDef("url_codec", "URL 编码/解码", "本地 URL 编码互转", "🔗", ToolCategory.TEXT) { UrlCodecTool() },
        ToolDef("password", "密码生成器", "长度与字符集自定义、高强度随机", "🔑", ToolCategory.TEXT) { PasswordTool() },
        ToolDef("text_count", "文本字数计算器", "字符、中文、行数、单词、字节统计", "📊", ToolCategory.TEXT) { TextCountTool() },
        ToolDef("case_convert", "大小写转换", "大写/小写/首字母大写", "🔠", ToolCategory.TEXT) { CaseConvertTool() },
        ToolDef("text_dedupe", "文本去重工具", "按行去重、查看重复项", "🧹", ToolCategory.TEXT) { TextDedupeTool() },
        ToolDef("text_sort", "文本排序工具", "字典序 / 长度排序", "🔃", ToolCategory.TEXT) { TextSortTool() },
        ToolDef("find_replace", "文本查找替换", "全部替换并统计次数", "🔍", ToolCategory.TEXT) { FindReplaceTool() },
        ToolDef("whitespace", "空白清理工具", "删除多余空格与空行", "🧽", ToolCategory.TEXT) { WhitespaceTool() },
        ToolDef("split_merge", "文本分割/合并", "按分隔符拆分、多行合并", "✂️", ToolCategory.TEXT) { SplitMergeTool() },
        ToolDef("zh_convert", "简繁转换工具", "简体 ↔ 繁体（本地字库）", "🀄", ToolCategory.TEXT) { ZhConvertTool() },

        // ---------- 影音与图像 ----------
        ToolDef("image_format", "图片格式转换", "jpg / png / bmp / tif / webp 互转", "🖼️", ToolCategory.MEDIA) { ImageFormatConvertTool() },
        ToolDef("image_resize", "修改图片尺寸", "按像素精确缩放图片", "📐", ToolCategory.MEDIA) { ImageResizeTool() },
        ToolDef("id_photo", "证件照尺寸调整", "一寸、二寸、护照等规格自动裁剪", "🪪", ToolCategory.MEDIA) { IdPhotoTool() },
        ToolDef("image_compress", "图片压缩", "质量与边长控制，减小文件体积", "🗜️", ToolCategory.MEDIA) { ImageCompressTool() },
        ToolDef("image_crop", "图片裁剪", "拖动框选、比例锁定、缩放裁剪", "✂️", ToolCategory.MEDIA) { ImageCropTool() },
        ToolDef("image_stitch", "图片拼接", "多图竖向 / 横向拼接", "🧩", ToolCategory.MEDIA) { ImageStitchTool() },
        ToolDef("word_to_pdf", "Word 转 PDF", ".docx 文本段落转 PDF（离线）", "📄", ToolCategory.MEDIA) { WordToPdfTool() },
        ToolDef("long_image_to_pdf", "长图转 PDF", "长截图、长海报生成为 PDF", "📜", ToolCategory.MEDIA) { LongImageToPdfTool() },
        ToolDef("pdf_to_long_image", "PDF 转长图", "PDF 各页渲染后竖向拼接", "🧾", ToolCategory.MEDIA) { PdfToLongImageTool() },
        ToolDef("video_to_audio", "视频转音频", "提取视频原声为 M4A", "🎵", ToolCategory.MEDIA) { VideoToAudioTool() },
        ToolDef("video_to_gif", "视频转 GIF", "抽帧生成动图", "🎞️", ToolCategory.MEDIA) { VideoToGifTool() },
        ToolDef("video_edit", "视频格式转换/变速/压缩", "输出 MP4(H.264)，支持变速与重编码", "🎬", ToolCategory.MEDIA) { VideoEditTool() },

        // ---------- 函数绘图 ----------
        ToolDef("graph_2d", "函数绘图器（2D）", "显函数/极坐标/参数方程/隐函数/不等式，滑块与导出", "📈", ToolCategory.GRAPH) { FunctionGraphTool() },
        ToolDef("graph_3d", "三维函数绘图器", "z=f(x,y) 曲面，旋转查看与导出", "🧊", ToolCategory.GRAPH) { Surface3DTool() },

        // ---------- 图鉴查询 ----------
        ToolDef("car_badges", "车标图鉴", "170+ 车标，自动加载公开版权车标，可自行导入", "🚗", ToolCategory.KNOWLEDGE) { CarLogoTool() },
        ToolDef("shortcut_keys", "电脑快捷键查询", "Windows / macOS 常用与进阶快捷键", "⌨️", ToolCategory.KNOWLEDGE) { ShortcutTool() },
        ToolDef("formula_query", "数学物理公式查询", "约 150 条数学/物理公式，可搜索分类", "🧮", ToolCategory.KNOWLEDGE) { FormulaTool() },
        ToolDef("probability", "概率论公式与分布", "概率公式大全 + 正态/二项/泊松等分布计算", "🎲", ToolCategory.KNOWLEDGE) { ProbabilityTool() },
        ToolDef("vocab", "考研背单词", "词库搜索、卡片背诵、自定义词库导入", "📚", ToolCategory.KNOWLEDGE) { VocabTool() },

        // ---------- 实用工具 ----------
        ToolDef("qr_code", "二维码生成器", "本地批量生成二维码、保存与分享", "📱", ToolCategory.EXTRA) { QrCodeTool() },
        ToolDef("bmi", "BMI 计算器", "身体质量指数计算", "🧍", ToolCategory.EXTRA) { BmiTool() },
        ToolDef("fuel", "油耗计算器", "记录油耗、本地统计", "⛽", ToolCategory.EXTRA) { FuelConsumptionTool() },
        ToolDef("aa_split", "AA 分摊计算器", "多人消费自动分摊到分", "👥", ToolCategory.EXTRA) { AaSplitTool() },
        ToolDef("lunar", "农历公历互查", "内置 ICU 万年历，离线查询", "🗓️", ToolCategory.EXTRA) { LunarCalendarTool() },
        ToolDef("ganzhi", "生肖天干地支", "生肖、年干支、日干支查询", "🐉", ToolCategory.EXTRA) { GanzhiTool() },
        ToolDef("color_picker", "颜色拾取器", "RGB 滑块与 HEX 互转", "🎨", ToolCategory.EXTRA) { ColorPickerTool() },
        ToolDef("notes", "备忘录", "本地新建、编辑、搜索、删除备忘", "📝", ToolCategory.EXTRA) { NotesTool() },
        ToolDef("flashlight", "手电筒", "一键开关闪光灯，退出自动关闭", "🔦", ToolCategory.EXTRA) { FlashlightTool() },
        ToolDef("compass", "指南针", "传感器实时方向与角度", "🧭", ToolCategory.EXTRA) { CompassTool() },
        ToolDef("mirror", "镜子", "前置摄像头全屏镜像预览", "🪞", ToolCategory.EXTRA) { MirrorTool() },
        ToolDef("recorder", "录音机", "本地录音、播放、分享、删除", "🎙️", ToolCategory.EXTRA) { RecorderTool() }
    )

    fun byId(id: String): ToolDef? = all.firstOrNull { it.id == id }

    fun byCategory(category: ToolCategory): List<ToolDef> =
        all.filter { it.category == category }
}
