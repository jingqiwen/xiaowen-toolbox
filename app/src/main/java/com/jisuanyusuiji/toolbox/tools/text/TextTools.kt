package com.jisuanyusuiji.toolbox.tools.text

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.CopyButton
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.ResultText
import com.jisuanyusuiji.toolbox.ui.components.SectionCard

// ============================================================
// 33. 文本字数计算器
// ============================================================
@Composable
fun TextCountTool() {
    var input by remember { mutableStateOf("") }
    var result by remember { mutableStateOf(listOf<Pair<String, String>>()) }

    fun count() {
        val text = input
        val lines = text.lines()
        result = listOf(
            "总字符数（含空格换行）" to text.length.toString(),
            "不含空白字符" to text.count { !it.isWhitespace() }.toString(),
            "中文字符数" to text.count { it in '\u4e00'..'\u9fff' }.toString(),
            "英文字母数" to text.count { it.isLetter() && it.code < 128 }.toString(),
            "数字个数" to text.count { it.isDigit() }.toString(),
            "行数" to lines.size.toString(),
            "单词数（按空白切分）" to text.split(Regex("\\s+")).count { it.isNotEmpty() }.toString(),
            "UTF-8 字节数" to text.toByteArray(Charsets.UTF_8).size.toString()
        )
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "文本统计") {
            LabeledField(input, { input = it }, "输入或粘贴文本", singleLine = false, minLines = 6)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { count() }, modifier = Modifier.fillMaxWidth()) { Text("统计") }
        }
        if (result.isNotEmpty()) {
            SectionCard(title = "统计结果") {
                result.forEach { (label, value) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text(value, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

// ============================================================
// 34. 文本大小写转换工具
// ============================================================
@Composable
fun CaseConvertTool() {
    var input by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("全部大写") }
    var output by remember { mutableStateOf("") }

    fun convert() {
        output = when (mode) {
            "全部大写" -> input.uppercase()
            "全部小写" -> input.lowercase()
            "单词首字母大写" -> input.split(Regex("\\s+")).joinToString(" ") { w ->
                w.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            else -> { // 句子首字母大写（按句号、问号、感叹号分段简化处理）
                val sb = StringBuilder()
                var start = true
                for (c in input) {
                    sb.append(if (start && c.isLetter()) c.uppercaseChar() else c)
                    start = c in ".!?。！？\n"
                }
                sb.toString()
            }
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "大小写转换") {
            ChoiceChips(
                options = listOf("全部大写", "全部小写", "单词首字母大写", "句子首字母大写"),
                selected = mode,
                onSelect = { mode = it },
                label = { it }
            )
            Spacer(Modifier.height(10.dp))
            LabeledField(input, { input = it }, "输入文本", singleLine = false, minLines = 5)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { convert() }, modifier = Modifier.fillMaxWidth()) { Text("转换") }
        }
        if (output.isNotBlank()) {
            SectionCard(title = "结果") {
                ResultText(output)
                Spacer(Modifier.height(10.dp))
                CopyButton(output)
            }
        }
    }
}

// ============================================================
// 35. 文本去重工具
// ============================================================
@Composable
fun TextDedupeTool() {
    var input by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("保留首次出现") }
    var ignoreEmpty by remember { mutableStateOf(true) }
    var output by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    fun run() {
        error = ""
        output = ""
        val lines = input.lines().map { it.trim() }.let { if (ignoreEmpty) it.filter { l -> l.isNotEmpty() } else it }
        if (lines.isEmpty()) { error = "请输入文本"; return }
        output = when (mode) {
            "保留首次出现" -> lines.distinct()
            "保留末次出现" -> lines.reversed().distinct().reversed()
            else -> lines.groupingBy { it }.eachCount().filter { it.value > 1 }.keys.toList()
        }.joinToString("\n")
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "按行去重") {
            ChoiceChips(
                options = listOf("保留首次出现", "保留末次出现", "只显示重复项"),
                selected = mode,
                onSelect = { mode = it },
                label = { it }
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("忽略空行", modifier = Modifier.weight(1f))
                Switch(checked = ignoreEmpty, onCheckedChange = { ignoreEmpty = it })
            }
            Spacer(Modifier.height(8.dp))
            LabeledField(input, { input = it }, "每行一条文本", singleLine = false, minLines = 6)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { run() }, modifier = Modifier.fillMaxWidth()) { Text("去重") }
        }
        ErrorText(error)
        if (output.isNotBlank()) {
            SectionCard(title = "结果（${output.lines().size} 行）") {
                ResultText(output)
                Spacer(Modifier.height(10.dp))
                CopyButton(output)
            }
        }
    }
}

// ============================================================
// 36. 文本排序工具
// ============================================================
@Composable
fun TextSortTool() {
    var input by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("字典序升序") }
    var output by remember { mutableStateOf("") }

    fun run() {
        val lines = input.lines().map { it.trim() }.filter { it.isNotEmpty() }
        output = when (mode) {
            "字典序升序" -> lines.sorted()
            "字典序降序" -> lines.sortedDescending()
            "按长度短→长" -> lines.sortedWith(compareBy({ it.length }, { it }))
            else -> lines.sortedWith(compareByDescending<String> { it.length }.thenBy { it })
        }.joinToString("\n")
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "按行排序") {
            ChoiceChips(
                options = listOf("字典序升序", "字典序降序", "按长度短→长", "按长度长→短"),
                selected = mode,
                onSelect = { mode = it },
                label = { it }
            )
            Spacer(Modifier.height(10.dp))
            LabeledField(input, { input = it }, "每行一条文本", singleLine = false, minLines = 6)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { run() }, modifier = Modifier.fillMaxWidth()) { Text("排序") }
        }
        if (output.isNotBlank()) {
            SectionCard(title = "结果") {
                ResultText(output)
                Spacer(Modifier.height(10.dp))
                CopyButton(output)
            }
        }
    }
}

// ============================================================
// 37. 文本查找替换工具
// ============================================================
@Composable
fun FindReplaceTool() {
    var input by remember { mutableStateOf("") }
    var find by remember { mutableStateOf("") }
    var replace by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var count by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf("") }

    fun run() {
        error = ""
        output = ""
        if (find.isEmpty()) { error = "请输入要查找的内容"; return }
        count = input.split(find).size - 1
        output = input.replace(find, replace)
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "查找与替换（全部替换）") {
            LabeledField(input, { input = it }, "原文", singleLine = false, minLines = 4)
            Spacer(Modifier.height(8.dp))
            LabeledField(find, { find = it }, "查找内容")
            Spacer(Modifier.height(8.dp))
            LabeledField(replace, { replace = it }, "替换为")
            Spacer(Modifier.height(12.dp))
            Button(onClick = { run() }, modifier = Modifier.fillMaxWidth()) { Text("全部替换") }
        }
        ErrorText(error)
        if (output.isNotBlank() || count > 0) {
            SectionCard(title = "结果（替换 $count 处）") {
                ResultText(output)
                Spacer(Modifier.height(10.dp))
                CopyButton(output)
            }
        }
    }
}

// ============================================================
// 38. 空白清理工具
// ============================================================
@Composable
fun WhitespaceTool() {
    var input by remember { mutableStateOf("") }
    var trimLines by remember { mutableStateOf(true) }
    var removeEmpty by remember { mutableStateOf(true) }
    var collapseSpaces by remember { mutableStateOf(true) }
    var removeAllSpaces by remember { mutableStateOf(false) }
    var output by remember { mutableStateOf("") }

    fun run() {
        var lines = input.lines()
        if (trimLines) lines = lines.map { it.trim() }
        if (removeEmpty) lines = lines.filter { it.isNotEmpty() }
        var text = lines.joinToString("\n")
        if (collapseSpaces) text = text.replace(Regex("[ \\t]{2,}"), " ")
        if (removeAllSpaces) text = text.replace(" ", "")
        text = text.replace(Regex("\\n{3,}"), "\n\n")
        output = text
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "空白清理选项") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("每行首尾去空白", modifier = Modifier.weight(1f)); Switch(trimLines, { trimLines = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("删除空行", modifier = Modifier.weight(1f)); Switch(removeEmpty, { removeEmpty = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("合并连续空格", modifier = Modifier.weight(1f)); Switch(collapseSpaces, { collapseSpaces = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("删除所有空格", modifier = Modifier.weight(1f)); Switch(removeAllSpaces, { removeAllSpaces = it })
            }
            Spacer(Modifier.height(8.dp))
            LabeledField(input, { input = it }, "输入文本", singleLine = false, minLines = 6)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { run() }, modifier = Modifier.fillMaxWidth()) { Text("清理") }
        }
        if (output.isNotBlank()) {
            SectionCard(title = "结果") {
                ResultText(output)
                Spacer(Modifier.height(10.dp))
                CopyButton(output)
            }
        }
    }
}

// ============================================================
// 39. 文本分割 / 合并工具
// ============================================================
@Composable
fun SplitMergeTool() {
    var mode by remember { mutableStateOf("分割文本") }
    var input by remember { mutableStateOf("苹果,香蕉,橙子,西瓜") }
    var delimiter by remember { mutableStateOf(",") }
    var output by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    fun decode(s: String) = s.replace("\\t", "\t").replace("\\n", "\n").replace("\\r", "\r")

    fun run() {
        error = ""
        output = ""
        val sep = decode(delimiter)
        if (mode == "分割文本") {
            if (sep.isEmpty()) { error = "请输入分隔符"; return }
            output = input.split(sep).mapIndexed { i, s -> "${i + 1}. ${s.trim()}" }.joinToString("\n")
        } else {
            val lines = input.lines().map { it.trim() }.filter { it.isNotEmpty() }
            output = lines.joinToString(sep)
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "模式") {
            ChoiceChips(listOf("分割文本", "合并多行"), mode, { mode = it }, { it })
        }
        SectionCard(title = if (mode == "分割文本") "按分隔符拆分" else "多行合并") {
            LabeledField(input, { input = it }, if (mode == "分割文本") "输入文本" else "每行一条", singleLine = false, minLines = 5)
            Spacer(Modifier.height(8.dp))
            LabeledField(delimiter, { delimiter = it }, "分隔符（\\t 表示制表符，\\n 表示换行）")
            Spacer(Modifier.height(12.dp))
            Button(onClick = { run() }, modifier = Modifier.fillMaxWidth()) { Text(if (mode == "分割文本") "分割" else "合并") }
        }
        ErrorText(error)
        if (output.isNotBlank()) {
            SectionCard(title = "结果") {
                ResultText(output)
                Spacer(Modifier.height(10.dp))
                CopyButton(output)
            }
        }
    }
}
