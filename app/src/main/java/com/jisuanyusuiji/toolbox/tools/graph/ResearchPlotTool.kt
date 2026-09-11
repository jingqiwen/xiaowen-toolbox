package com.jisuanyusuiji.toolbox.tools.graph

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.CopyButton
import com.jisuanyusuiji.toolbox.ui.components.SectionCard

private data class OnlineTool(
    val category: String,
    val name: String,
    val desc: String,
    val freeNote: String,
    val url: String
)

/**
 * 科研绘图改为“在线工具导航”：本地绘制的图表样式有限、也没有示例数据，
 * 直接跳转到成熟且可免费导出结果的网站，效果更好、可保存。
 */
private val ONLINE_TOOLS = listOf(
    // ---------- 图表制作 ----------
    OnlineTool("图表制作", "Datawrapper", "柱状图 / 折线图 / 饼图 / 地图，中文友好，导出 PNG/SVG", "免费使用，免费导出", "https://www.datawrapper.de/"),
    OnlineTool("图表制作", "Flourish", "交互式动画图表、故事图表，适合汇报与公众号", "免费版可导出 PNG", "https://flourish.studio/"),
    OnlineTool("图表制作", "RAWGraphs", "粘贴 Excel 数据即可生成桑基图、气泡图、树图等几十种图表", "完全免费，导出 SVG/PNG", "https://app.rawgraphs.io/"),
    OnlineTool("图表制作", "Plotly Chart Studio", "在线数据拟合、3D 图、统计图，可保存分享", "免费注册，可导出 PNG", "https://chart-studio.plotly.com/"),
    OnlineTool("图表制作", "镝数图表", "中文在线图表，模板多，适合论文插图", "免费基础版，可导出", "https://dydata.io/"),
    OnlineTool("图表制作", "Meta-Chart", "快速生成柱状、饼状、折线图，无需注册", "免费下载图片", "https://www.meta-chart.com/"),

    // ---------- 数据分析与统计 ----------
    OnlineTool("数据分析与统计", "Statistics Kingdom", "t 检验、方差分析、回归、卡方等在线计算并配图", "免费使用", "https://www.statskingdom.com/"),
    OnlineTool("数据分析与统计", "VassarStats", "经典在线统计计算器：检验、相关、回归、列联表", "免费使用", "http://vassarstats.net/"),
    OnlineTool("数据分析与统计", "Social Science Statistics", "各类假设检验、效应量、样本量计算", "免费使用", "https://www.socscistatistics.com/"),
    OnlineTool("数据分析与统计", "GraphPad QuickCalcs", "常用的 t 检验、卡方、Fisher 精确检验等", "免费在线使用", "https://www.graphpad.com/quickcalcs/"),
    OnlineTool("数据分析与统计", "jamovi", "免费开源统计分析软件（桌面版），类似 SPSS", "完全免费开源", "https://www.jamovi.org/"),
    OnlineTool("数据分析与统计", "JASP", "免费开源统计软件，贝叶斯统计友好", "完全免费开源", "https://jasp-stats.org/"),

    // ---------- 专用图形 ----------
    OnlineTool("专用图形", "ClustVis", "在线 PCA、聚类与热图，上传表格即可出图", "免费使用，可导出", "https://biit.cs.ut.ee/clustvis/"),
    OnlineTool("专用图形", "Morpheus", "在线热图、聚类、富集分析可视化", "免费使用", "https://software.broadinstitute.org/morpheus/"),
    OnlineTool("专用图形", "InteractiVenn", "在线韦恩图，支持 2~6 组集合", "免费使用，可导出", "http://www.interactivenn.net/"),
    OnlineTool("专用图形", "SankeyMATIC", "在线桑基图（流向图）制作", "免费使用，导出 PNG", "https://sankeymatic.com/"),
    OnlineTool("专用图形", "WebPlotDigitizer", "从论文图片中反推数据点（曲线数据提取）", "免费在线使用", "https://automeris.io/WebPlotDigitizer/"),
    OnlineTool("专用图形", "diagrams.net（draw.io）", "流程图、实验装置示意图、结构框图", "完全免费，导出 PNG/SVG/PDF", "https://app.diagrams.net/"),
    OnlineTool("专用图形", "Excalidraw", "手绘风格示意图，适合讲义与课件", "免费使用，可导出 PNG/SVG", "https://excalidraw.com/"),

    // ---------- 公式与表格 ----------
    OnlineTool("公式与表格", "LaTeX 公式编辑器（中文）", "可视化输入公式，生成 LaTeX 代码与图片", "免费使用", "https://www.latexlive.com/"),
    OnlineTool("公式与表格", "CodeCogs 公式编辑器", "在线 LaTeX 公式渲染，可下载 PNG/GIF", "免费使用", "https://latex.codecogs.com/eqneditor/editor.php"),
    OnlineTool("公式与表格", "Tables Generator", "在线生成 LaTeX / Markdown / Word 表格", "免费使用", "https://www.tablesgenerator.com/"),

    // ---------- 配色与写作 ----------
    OnlineTool("配色与写作", "ColorBrewer", "科研论文常用配色方案（色盲友好）", "免费使用", "https://colorbrewer2.org/"),
    OnlineTool("配色与写作", "Coolors", "一键生成协调配色，可导出调色板", "免费使用", "https://coolors.co/"),
    OnlineTool("配色与写作", "Overleaf", "在线 LaTeX 写作与排版，模板丰富", "免费版可用", "https://www.overleaf.com/"),
    OnlineTool("配色与写作", "Mathpix Snip", "截图识别公式转 LaTeX（每月有免费额度）", "免费额度", "https://mathpix.com/")
)

@Composable
fun ResearchPlotTool() {
    val context = LocalContext.current
    var category by remember { mutableStateOf("全部") }
    val categories = remember { listOf("全部") + ONLINE_TOOLS.map { it.category }.distinct() }
    val list = remember(category) {
        if (category == "全部") ONLINE_TOOLS else ONLINE_TOOLS.filter { it.category == category }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "📊 科研绘图 · 在线工具导航（${ONLINE_TOOLS.size} 个免费网站）",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "本地绘图样式有限、也不方便保存；这里直接跳转到成熟网站，上传数据即可出图并免费导出 PNG/SVG。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
        ChoiceChips(
            options = categories,
            selected = category,
            onSelect = { category = it },
            label = { if (it == "全部") it else "$it ${ONLINE_TOOLS.count { t -> t.category == it }}" },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(list, key = { it.name }) { tool ->
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(tool.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${tool.category} · ${tool.freeNote}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(tool.desc, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            tool.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(tool.url)))
                                    } catch (_: Exception) {
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("🌐 打开网站") }
                            CopyButton(text = tool.url, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            item {
                SectionCard(title = "使用提示") {
                    Text(
                        "· 大多数网站上传的数据只在该网站处理，导出图片一般免费；部分需要免费注册账号。\n" +
                            "· 数据格式一般是 CSV / Excel：把实验数据整理成“表头 + 每行一条记录”即可。\n" +
                            "· 论文插图建议导出 SVG 或高分辨率 PNG（300 dpi 以上），再用 Word/LaTeX 排版。\n" +
                            "· ClustVis、Morpheus 适合热图与 PCA；WebPlotDigitizer 可以从别人的图里取数据做对比。\n" +
                            "· 打开网站需要联网（可在设置里关闭联网功能，关闭后本页链接仍可复制到浏览器使用）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
