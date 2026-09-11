package com.jisuanyusuiji.toolbox.tools.knowledge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.CopyButton
import com.jisuanyusuiji.toolbox.ui.components.MathText
import com.jisuanyusuiji.toolbox.ui.components.ShareButton

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
    val context = LocalContext.current
    val book = remember { FormulaBook(context) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("全部") }
    var chapter by remember { mutableStateOf("全部") }
    var bookOnly by remember { mutableStateOf(false) }
    var favRefresh by remember { mutableStateOf(0) }
    var noteFor by remember { mutableStateOf<FormulaItem?>(null) }
    var noteText by remember { mutableStateOf("") }

    val allFormulas = remember {
        (FormulaData.all + FormulaDataExtra.all + FormulaDataBooks.all +
            FormulaDataMore.all + FormulaDataMore2.all + FormulaDataMore3.all + FormulaDataMore4.all)
            .map { it.toSubject() }
    }
    val favorites = remember(favRefresh) { book.favorites() }
    val categories = remember {
        listOf("全部") + SUBJECT_ORDER.filter { subject -> allFormulas.any { it.category == subject } } +
            allFormulas.map { it.category }.distinct().filter { it !in SUBJECT_ORDER }
    }
    val chapters = remember(category) {
        if (category == "全部") emptyList()
        else listOf("全部") + allFormulas.filter { it.category == category }
            .map { it.chapter }.filter { it.isNotBlank() }.distinct()
    }
    val items = remember(query, category, chapter, bookOnly, favorites) {
        allFormulas.filter { item ->
            (!bookOnly || favorites.contains(book.keyOf(item))) &&
                (category == "全部" || item.category == category) &&
                (chapter == "全部" || item.chapter == chapter) &&
                (query.isBlank() ||
                    item.name.contains(query, true) ||
                    item.expression.contains(query, true) ||
                    item.note.contains(query, true) ||
                    item.category.contains(query, true) ||
                    item.chapter.contains(query, true) ||
                    book.note(book.keyOf(item)).contains(query, true))
        }
    }
    val bookText = remember(favorites, favRefresh) {
        buildString {
            appendLine("小温工具箱 · 我的公式本（${favorites.size} 条）")
            appendLine("——————————————")
            allFormulas.filter { favorites.contains(book.keyOf(it)) }.forEachIndexed { i, f ->
                appendLine("${i + 1}. ${f.name}")
                appendLine("${f.category}${if (f.chapter.isNotBlank()) " · ${f.chapter}" else ""}")
                appendLine(f.expression)
                val n = book.note(book.keyOf(f))
                if (n.isNotBlank()) appendLine("笔记：$n")
                appendLine()
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            if (bookOnly) "⭐ 我的公式本 · ${items.size} 条"
            else "🧮 数学 / 物理公式 · ${items.size} / ${allFormulas.size} 条（按科目分类）",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("搜索公式名称 / 表达式 / 说明 / 我的笔记") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
        ChoiceChips(
            options = listOf(false, true),
            selected = bookOnly,
            onSelect = { bookOnly = it },
            label = { if (it) "⭐ 我的公式本（${favorites.size}）" else "📚 全部公式" },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (bookOnly && favorites.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CopyButton(text = bookText, modifier = Modifier.weight(1f))
                ShareButton(text = bookText, title = "分享我的公式本", modifier = Modifier.weight(1f))
            }
        }
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
                val key = book.keyOf(formula)
                val favorite = favorites.contains(key)
                val myNote = book.note(key)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
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
                            }
                            TextButton(onClick = {
                                book.toggle(key)
                                favRefresh++
                            }) {
                                Text(
                                    if (favorite) "★" else "☆",
                                    fontSize = 22.sp,
                                    color = if (favorite) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = {
                                noteFor = formula
                                noteText = myNote
                            }) {
                                Text("📝", fontSize = 18.sp)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        // 公式主体：LaTeX 风格排版 + 淡色底
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            MathText(
                                text = formula.expression,
                                fontSize = 19.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (formula.note.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                formula.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (myNote.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "📝 我的笔记：$myNote",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        CopyButton(text = "${formula.name}\n${formula.expression}\n${formula.note}")
                    }
                }
            }
            if (items.isEmpty()) {
                item {
                    Text(
                        if (bookOnly) "公式本还是空的：在公式卡片右上角点 ☆ 即可收藏"
                        else "没有找到相关公式",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    noteFor?.let { formula ->
        AlertDialog(
            onDismissRequest = { noteFor = null },
            title = { Text("公式笔记：${formula.name}") },
            text = {
                Column {
                    Text(formula.expression, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("记录理解、适用条件、易错点…") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    book.setNote(book.keyOf(formula), noteText)
                    noteFor = null
                    favRefresh++
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { noteFor = null }) { Text("取消") }
            }
        )
    }
}
