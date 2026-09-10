package com.jisuanyusuiji.toolbox.tools.vocab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.data.JsonStore
import org.json.JSONArray

private fun loadStringSet(store: JsonStore, key: String): MutableSet<String> {
    val arr = store.getArray(key)
    return (0 until arr.length()).mapNotNull { i ->
        try { arr.getString(i) } catch (_: Exception) { null }
    }.toMutableSet()
}

private fun saveStringSet(store: JsonStore, key: String, set: Set<String>) {
    val arr = JSONArray()
    set.forEach { arr.put(it) }
    store.putArray(key, arr)
}

@Composable
fun VocabTool() {
    val context = LocalContext.current
    val store = remember { JsonStore(context, "vocab") }
    var custom by remember { mutableStateOf(loadCustomWords(store)) }
    var known by remember { mutableStateOf(loadStringSet(store, "known")) }
    var mode by remember { mutableStateOf("词库") }
    var query by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<VocabWord?>(null) }
    var showImport by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    val all = remember(custom) { VocabData.builtIn + custom }
    val filtered = remember(query, all) {
        if (query.isBlank()) all
        else all.filter {
            it.word.contains(query, true) || it.meanings.contains(query, true) || it.phrases.contains(query, true)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "📚 考研背单词 · 共 ${all.size} 词（已掌握 ${known.size}）",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { mode = "词库" },
                modifier = Modifier.weight(1f),
                enabled = mode != "词库"
            ) { Text("词库") }
            Button(
                onClick = { mode = "背单词" },
                modifier = Modifier.weight(1f),
                enabled = mode != "背单词"
            ) { Text("背单词") }
            OutlinedButton(onClick = { showImport = true }, modifier = Modifier.weight(1f)) { Text("导入") }
        }
        Spacer(Modifier.height(8.dp))

        if (mode == "词库") {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("搜索单词 / 释义 / 搭配") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.word }) { word ->
                    Card(
                        onClick = { detail = word },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(word.word, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.padding(4.dp))
                                Text(word.phonetic, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (known.contains(word.word)) {
                                    Spacer(Modifier.padding(4.dp))
                                    Text("✅", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Text(word.meanings, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                        }
                    }
                }
                if (filtered.isEmpty()) {
                    item { Text("没有找到单词", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        } else {
            StudyMode(
                words = filtered.ifEmpty { all },
                known = known,
                onKnown = { word, isKnown ->
                    known = if (isKnown) (known + word).toMutableSet() else (known - word).toMutableSet()
                    saveStringSet(store, "known", known)
                }
            )
        }
    }

    detail?.let { word ->
        AlertDialog(
            onDismissRequest = { detail = null },
            title = { Text(word.word, fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                    Text(word.phonetic, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    InfoLine("全部释义", word.meanings)
                    if (word.usage.isNotBlank()) InfoLine("用法", word.usage)
                    if (word.phrases.isNotBlank()) InfoLine("固定搭配/短语", word.phrases)
                    if (word.pastTense.isNotBlank()) InfoLine("动词变形", "过去式：${word.pastTense}；过去分词：${word.pastParticiple}")
                    if (word.example.isNotBlank()) InfoLine("例句", word.example)
                    if (word.derivatives.isNotBlank()) InfoLine("派生词", word.derivatives)
                    if (word.synonyms.isNotBlank()) InfoLine("近义词", word.synonyms)
                    if (word.antonyms.isNotBlank()) InfoLine("反义词", word.antonyms)
                    if (word.similar.isNotBlank()) InfoLine("形近词", word.similar)
                }
            },
            confirmButton = { TextButton(onClick = { detail = null }) { Text("关闭") } }
        )
    }

    if (showImport) {
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text("导入自定义词库") },
            text = {
                Column {
                    Text(
                        "每行一个单词，用 | 分隔：\n单词|音标|释义|用法|搭配|过去式|过去分词|例句|派生|近义|反义|形近\n（后几项可为空）",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        label = { Text("粘贴词库内容") },
                        minLines = 8,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsed = importText.lines().mapNotNull { line ->
                        val parts = line.split("|").map { it.trim() }
                        if (parts.isEmpty() || parts[0].isEmpty()) null
                        else VocabWord(
                            word = parts.getOrElse(0) { "" },
                            phonetic = parts.getOrElse(1) { "" },
                            meanings = parts.getOrElse(2) { "" },
                            usage = parts.getOrElse(3) { "" },
                            phrases = parts.getOrElse(4) { "" },
                            pastTense = parts.getOrElse(5) { "" },
                            pastParticiple = parts.getOrElse(6) { "" },
                            example = parts.getOrElse(7) { "" },
                            derivatives = parts.getOrElse(8) { "" },
                            synonyms = parts.getOrElse(9) { "" },
                            antonyms = parts.getOrElse(10) { "" },
                            similar = parts.getOrElse(11) { "" }
                        )
                    }
                    if (parsed.isNotEmpty()) {
                        custom = (custom + parsed).distinctBy { it.word }.toMutableList()
                        saveCustomWords(store, custom)
                        importText = ""
                        showImport = false
                    }
                }) { Text("导入") }
            },
            dismissButton = { TextButton(onClick = { showImport = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun StudyMode(words: List<VocabWord>, known: Set<String>, onKnown: (String, Boolean) -> Unit) {
    var index by remember { mutableStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    val word = words.getOrNull(index % words.size.coerceAtLeast(1))

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (word == null) {
            Text("词库为空")
            return@Column
        }
        Text("${index + 1} / ${words.size}", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(word.word, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(word.phonetic, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                if (revealed) {
                    Text(word.meanings, style = MaterialTheme.typography.bodyLarge)
                    if (word.phrases.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text("搭配：${word.phrases}", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (word.example.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text("例句：${word.example}", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    Text("点击下方“显示释义”查看答案", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (!revealed) {
            Button(onClick = { revealed = true }, modifier = Modifier.fillMaxWidth()) { Text("显示释义") }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = {
                    onKnown(word.word, false)
                    index++
                    revealed = false
                }, modifier = Modifier.weight(1f)) { Text("不认识") }
                Button(onClick = {
                    onKnown(word.word, true)
                    index++
                    revealed = false
                }, modifier = Modifier.weight(1f)) { Text("认识 ✅") }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Text("$label：$value", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp))
}

private fun loadCustomWords(store: JsonStore): MutableList<VocabWord> {
    val arr = store.getArray("custom")
    return (0 until arr.length()).mapNotNull { i ->
        try {
            val o = arr.getJSONObject(i)
            VocabWord(
                word = o.optString("word"),
                phonetic = o.optString("phonetic"),
                meanings = o.optString("meanings"),
                usage = o.optString("usage"),
                phrases = o.optString("phrases"),
                pastTense = o.optString("pastTense"),
                pastParticiple = o.optString("pastParticiple"),
                example = o.optString("example"),
                derivatives = o.optString("derivatives"),
                synonyms = o.optString("synonyms"),
                antonyms = o.optString("antonyms"),
                similar = o.optString("similar")
            )
        } catch (_: Exception) { null }
    }.toMutableList()
}

private fun saveCustomWords(store: JsonStore, words: List<VocabWord>) {
    val arr = JSONArray()
    words.forEach { w ->
        arr.put(
            org.json.JSONObject()
                .put("word", w.word).put("phonetic", w.phonetic).put("meanings", w.meanings)
                .put("usage", w.usage).put("phrases", w.phrases)
                .put("pastTense", w.pastTense).put("pastParticiple", w.pastParticiple)
                .put("example", w.example).put("derivatives", w.derivatives)
                .put("synonyms", w.synonyms).put("antonyms", w.antonyms).put("similar", w.similar)
        )
    }
    store.putArray("custom", arr)
}
