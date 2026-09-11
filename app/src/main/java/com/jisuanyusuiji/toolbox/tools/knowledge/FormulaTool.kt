package com.jisuanyusuiji.toolbox.tools.knowledge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.CopyButton

/** 按“科目”归类的顺序（不用小学/初中/高中做分类）。 */
private val SUBJECT_ORDER = listOf(
    "中学数学",
    "中学物理",
    "高等数学",
    "线性代数",
    "概率论与数理统计",
    "复变函数与积分变换",
    "大学物理",
    "大学物理实验",
    "科学常数"
)

/**
 * 把数据文件里的旧分类（按学段/分册）统一映射成“科目 + 章节”。
 * 数据文件保持原样，这里做展示层归类，便于以后维护。
 */
private fun FormulaItem.toSubject(): FormulaItem = when (category) {
    "代数" -> copy(category = "中学数学", chapter = "代数")
    "三角" -> copy(category = "中学数学", chapter = "三角函数")
    "几何" -> copy(category = "中学数学", chapter = "平面几何")
    "解析几何" -> copy(category = "中学数学", chapter = "解析几何")
    "小学数学" -> copy(category = "中学数学", chapter = if (chapter.isBlank()) "小学基础" else "小学基础 · $chapter")
    "初中数学" -> copy(category = "中学数学", chapter = if (chapter.isBlank()) "初中数学" else "初中数学 · $chapter")
    "高中数学" -> copy(category = "中学数学", chapter = if (chapter.isBlank()) "高中数学" else "高中数学 · $chapter")
    "微积分" -> copy(category = "高等数学", chapter = "一元微积分")
    "大学数学" -> copy(category = "高等数学", chapter = if (chapter.isBlank()) "大学数学综合" else "大学数学综合 · $chapter")
    "力学" -> copy(category = "中学物理", chapter = "力学")
    "热学" -> copy(category = "中学物理", chapter = "热学")
    "电磁学" -> copy(category = "中学物理", chapter = "电磁学")
    "波与光" -> copy(category = "中学物理", chapter = "波与光")
    "初中物理" -> copy(category = "中学物理", chapter = "初中物理")
    "高中物理" -> copy(category = "中学物理", chapter = "高中物理")
    "大学物理" -> copy(category = "大学物理", chapter = if (chapter.isBlank()) "大学物理综合" else chapter)
    "概率统计" -> copy(category = "概率论与数理统计", chapter = "基础概率")
    "常数" -> copy(category = "科学常数", chapter = "常用常数")
    "高等数学上" -> copy(category = "高等数学", chapter = "上册 · ${chapter.ifBlank { "综合" }}")
    "高等数学下" -> copy(category = "高等数学", chapter = "下册 · ${chapter.ifBlank { "综合" }}")
    "大学物理上" -> copy(category = "大学物理", chapter = "上册 · ${chapter.ifBlank { "综合" }}")
    "大学物理下" -> copy(category = "大学物理", chapter = "下册 · ${chapter.ifBlank { "综合" }}")
    else -> this
}

@Composable
fun FormulaTool() {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("全部") }
    var chapter by remember { mutableStateOf("全部") }

    val allFormulas = remember {
        (FormulaData.all + FormulaDataExtra.all + FormulaDataBooks.all + FormulaDataMore.all + FormulaDataMore2.all)
            .map { it.toSubject() }
    }
    val categories = remember {
        listOf("全部") + SUBJECT_ORDER.filter { subject -> allFormulas.any { it.category == subject } } +
            allFormulas.map { it.category }.distinct().filter { it !in SUBJECT_ORDER }
    }
    val chapters = remember(category) {
        if (category == "全部") emptyList()
        else listOf("全部") + allFormulas.filter { it.category == category }
            .map { it.chapter }.filter { it.isNotBlank() }.distinct()
    }
    val items = remember(query, category, chapter) {
        allFormulas.filter { item ->
            (category == "全部" || item.category == category) &&
                (chapter == "全部" || item.chapter == chapter) &&
                (query.isBlank() ||
                    item.name.contains(query, true) ||
                    item.expression.contains(query, true) ||
                    item.note.contains(query, true) ||
                    item.category.contains(query, true) ||
                    item.chapter.contains(query, true))
        }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "🧮 数学 / 物理公式 · ${items.size} / ${allFormulas.size} 条（按科目分类）",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("搜索公式名称 / 表达式 / 说明") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
        ChoiceChips(
            options = categories,
            selected = category,
            onSelect = { category = it; chapter = "全部" },
            label = { subject ->
                if (subject == "全部") subject
                else "$subject ${allFormulas.count { it.category == subject }}"
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (chapters.size > 1) {
            Spacer(Modifier.height(6.dp))
            ChoiceChips(
                options = chapters,
                selected = chapter,
                onSelect = { chapter = it },
                label = { it },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(items, key = { index, item -> "$index|${item.category}|${item.name}" }) { _, formula ->
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            formula.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            buildString {
                                append(formula.category)
                                if (formula.chapter.isNotBlank()) append(" · ${formula.chapter}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            formula.expression,
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            formula.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        CopyButton(text = "${formula.name}\n${formula.expression}\n${formula.note}")
                    }
                }
            }
            if (items.isEmpty()) {
                item { Text("没有找到相关公式", modifier = Modifier.padding(16.dp)) }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
