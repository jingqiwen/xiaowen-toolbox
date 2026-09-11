package com.jisuanyusuiji.toolbox.tools.vocab

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.data.JsonStore
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import org.json.JSONArray
import org.json.JSONObject

private const val ZONE_UNKNOWN = "不认识"
private const val ZONE_FUZZY = "不熟悉"
private const val ZONE_KNOWN = "掌握"

private fun VocabWord.hasTag(tag: String): Boolean =
    tags.split(' ', '\t').any { it.equals(tag, true) }

private fun zoneLabel(zone: String): String = when (zone) {
    ZONE_KNOWN -> "✅ 掌握"
    ZONE_FUZZY -> "🤔 不熟悉（模糊）"
    else -> "😵 完全不认识"
}

@Composable
fun VocabTool() {
    val context = LocalContext.current
    val store = remember { JsonStore(context, "vocab") }
    var custom by remember { mutableStateOf(loadCustomWords(store)) }
    var zones by remember { mutableStateOf(loadZoneMap(store)) }
    var zoneFilter by remember { mutableStateOf("全部") }
    var mode by remember { mutableStateOf("词库") }
    var level by remember { mutableStateOf("考研") }
    var includeBasic by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<VocabWord?>(null) }
    var showImport by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    fun markZone(word: String, zone: String?) {
        val next = zones.toMutableMap()
        if (zone == null) next.remove(word) else next[word] = zone
        zones = next
        saveZoneMap(store, zones)
    }

    fun clearZone(zone: String) {
        val next = zones.toMutableMap()
        next.entries.removeAll { it.value == zone }
        zones = next
        saveZoneMap(store, zones)
    }

    val bulk = remember { loadBulkWords(context) }
    // 每个级别的“核心标签”（官方大纲收录）与“基础标签”（含基础词开关时并入）
    val coreTags = remember(level) {
        when (level) {
            "考研" -> listOf("ky")
            "四级" -> listOf("cet4")
            "六级" -> listOf("cet6")
            else -> emptyList()
        }
    }
    val basicTags = remember(level) {
        when (level) {
            "考研" -> listOf("cet6", "cet4", "gk", "zk")
            "四级" -> listOf("gk", "zk")
            "六级" -> listOf("cet4", "gk", "zk")
            else -> emptyList()
        }
    }
    val levelList = remember(level, custom, bulk, includeBasic) {
        when (level) {
            "内置精讲" -> VocabData.builtIn
            "我的词库" -> custom
            else -> bulk.filter { word ->
                coreTags.any { word.hasTag(it) } || (includeBasic && basicTags.any { word.hasTag(it) })
            }
        }
    }
    val levelCounts = remember(bulk, includeBasic) {
        fun count(core: List<String>, basic: List<String>) =
            bulk.count { word -> core.any { word.hasTag(it) } || (includeBasic && basic.any { word.hasTag(it) }) }
        mapOf(
            "考研" to count(listOf("ky"), listOf("cet6", "cet4", "gk", "zk")),
            "四级" to count(listOf("cet4"), listOf("gk", "zk")),
            "六级" to count(listOf("cet6"), listOf("cet4", "gk", "zk"))
        )
    }
    val filtered = remember(query, levelList, zoneFilter, zones) {
        levelList.filter { word ->
            (zoneFilter == "全部" || zones[word.word] == zoneFilter) &&
                (query.isBlank() || word.word.contains(query, true) || word.meanings.contains(query, true) || word.phrases.contains(query, true))
        }
    }
    val knownCount = zones.values.count { it == ZONE_KNOWN }
    val fuzzyCount = zones.values.count { it == ZONE_FUZZY }
    val unknownCount = zones.values.count { it == ZONE_UNKNOWN }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "📚 背单词 · ${level} · ${levelList.size} 词",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "掌握 $knownCount · 不熟悉 $fuzzyCount · 完全不认识 $unknownCount",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        ChoiceChips(
            listOf("内置精讲", "考研", "四级", "六级", "我的词库"),
            level,
            { level = it },
            {
                when (it) {
                    "考研", "四级", "六级" -> "$it ${levelCounts[it] ?: 0}"
                    else -> it
                }
            }
        )
        if (level == "考研" || level == "四级" || level == "六级") {
            Spacer(Modifier.height(6.dp))
            ChoiceChips(
                listOf(false, true),
                includeBasic,
                { includeBasic = it },
                { if (it) "含基础词（补全）" else "官方大纲规模" }
            )
            Text(
                if (includeBasic) "已并入中学 / 四级基础词，词表更全，适合零基础或查漏补缺"
                else "按考纲词数整理；打开“含基础词”可并入中学 / 四级基础词",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        ChoiceChips(
            listOf("全部", ZONE_UNKNOWN, ZONE_FUZZY, ZONE_KNOWN),
            zoneFilter,
            { zoneFilter = it },
            { if (it == "全部") "全部分区" else zoneLabel(it) }
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
                                val zone = zones[word.word]
                                if (zone != null) {
                                    Spacer(Modifier.padding(4.dp))
                                    Text(
                                        when (zone) {
                                            ZONE_KNOWN -> "✅"
                                            ZONE_FUZZY -> "🤔"
                                            else -> "😵"
                                        },
                                        style = MaterialTheme.typography.bodySmall
                                    )
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
                words = filtered.ifEmpty { levelList },
                zones = zones,
                onZone = { word, zone -> markZone(word, zone) },
                onClearZone = { clearZone(it) }
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
                    if (word.tags.isNotBlank()) InfoLine("词库标签", word.tags.uppercase())
                    if (word.collins.isNotBlank() && word.collins != "0") InfoLine("柯林斯星级", word.collins)
                    Spacer(Modifier.height(10.dp))
                    Text("加入分区", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { markZone(word.word, ZONE_UNKNOWN) }) { Text("😵 不认识") }
                        OutlinedButton(onClick = { markZone(word.word, ZONE_FUZZY) }) { Text("🤔 不熟悉") }
                        OutlinedButton(onClick = { markZone(word.word, ZONE_KNOWN) }) { Text("✅ 掌握") }
                    }
                    if (zones.containsKey(word.word)) {
                        TextButton(onClick = { markZone(word.word, null) }) { Text("移出分区") }
                    }
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

/**
 * 背单词模式：上方是单词卡片，下方是不认识区 / 不熟悉区 / 掌握区三个分区。
 * 点击分区里的单词可以直接跳到该单词，便于重点复习。
 */
@Composable
private fun StudyMode(
    words: List<VocabWord>,
    zones: Map<String, String>,
    onZone: (String, String?) -> Unit,
    onClearZone: (String) -> Unit
) {
    var index by remember { mutableStateOf(0) }
    var revealed by remember { mutableStateOf(false) }

    if (words.isEmpty()) {
        Text("词库为空", modifier = Modifier.padding(16.dp))
        return
    }
    LaunchedEffect(words.size) { if (index >= words.size) index = 0 }
    val word = words[index % words.size]

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                Text("${index + 1} / ${words.size}", style = MaterialTheme.typography.bodySmall)
                zones[word.word]?.let {
                    Text(
                        "当前标记：${zoneLabel(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        item {
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
        }
        item {
            if (!revealed) {
                Button(onClick = { revealed = true }, modifier = Modifier.fillMaxWidth()) { Text("显示释义") }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        onZone(word.word, ZONE_UNKNOWN)
                        index++
                        revealed = false
                    }, modifier = Modifier.fillMaxWidth()) { Text("😵 完全不认识") }
                    OutlinedButton(onClick = {
                        onZone(word.word, ZONE_FUZZY)
                        index++
                        revealed = false
                    }, modifier = Modifier.fillMaxWidth()) { Text("🤔 不熟悉（模糊）") }
                    Button(onClick = {
                        onZone(word.word, ZONE_KNOWN)
                        index++
                        revealed = false
                    }, modifier = Modifier.fillMaxWidth()) { Text("✅ 掌握") }
                    if (zones.containsKey(word.word)) {
                        TextButton(onClick = { onZone(word.word, null) }, modifier = Modifier.fillMaxWidth()) {
                            Text("清除该词的标记")
                        }
                    }
                }
            }
        }
        item {
            Text(
                "📂 单词分区（共 ${zones.size} 词已标记，点击分区中的单词可跳转）",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        item {
            ZoneSection(
                title = "😵 不认识区（完全不认识）",
                words = words.filter { zones[it.word] == ZONE_UNKNOWN },
                onJump = { w ->
                    val i = words.indexOf(w)
                    if (i >= 0) {
                        index = i
                        revealed = false
                    }
                },
                onClear = { onClearZone(ZONE_UNKNOWN) }
            )
        }
        item {
            ZoneSection(
                title = "🤔 不熟悉区（模糊）",
                words = words.filter { zones[it.word] == ZONE_FUZZY },
                onJump = { w ->
                    val i = words.indexOf(w)
                    if (i >= 0) {
                        index = i
                        revealed = false
                    }
                },
                onClear = { onClearZone(ZONE_FUZZY) }
            )
        }
        item {
            ZoneSection(
                title = "✅ 掌握区",
                words = words.filter { zones[it.word] == ZONE_KNOWN },
                onJump = { w ->
                    val i = words.indexOf(w)
                    if (i >= 0) {
                        index = i
                        revealed = false
                    }
                },
                onClear = { onClearZone(ZONE_KNOWN) }
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ZoneSection(
    title: String,
    words: List<VocabWord>,
    onJump: (VocabWord) -> Unit,
    onClear: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text("${words.size} 词", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (words.isNotEmpty()) {
                    TextButton(onClick = onClear, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("清空") }
                }
            }
            if (words.isEmpty()) {
                Text("暂无单词", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                words.chunked(3).forEach { rowWords ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowWords.forEach { w ->
                            OutlinedButton(
                                onClick = { onJump(w) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    w.word,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                        repeat(3 - rowWords.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Text("$label：$value", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp))
}

private fun loadBulkWords(context: Context): List<VocabWord> = try {
    context.assets.open("vocab_bulk.jsonl").bufferedReader().useLines { lines ->
        lines.mapNotNull { line ->
            try {
                val o = JSONObject(line)
                val exchange = o.optString("e")
                fun ex(key: String): String =
                    Regex("(?:^|/)" + key + ":([^/]+)").find(exchange)?.groupValues?.getOrNull(1) ?: ""
                VocabWord(
                    word = o.optString("w"),
                    phonetic = o.optString("p"),
                    meanings = o.optString("t"),
                    usage = o.optString("s"),
                    pastTense = ex("p"),
                    pastParticiple = ex("d"),
                    tags = o.optString("g"),
                    collins = o.optString("c")
                ).takeIf { it.word.isNotBlank() && it.meanings.isNotBlank() }
            } catch (_: Exception) {
                null
            }
        }.toList()
    }
} catch (_: Exception) {
    emptyList()
}

private fun loadZoneMap(store: JsonStore): MutableMap<String, String> {
    val obj = store.getObject("zones")
    val map = mutableMapOf<String, String>()
    obj.keys().forEach { key ->
        try { map[key] = obj.getString(key) } catch (_: Exception) {}
    }
    return map
}

private fun saveZoneMap(store: JsonStore, map: Map<String, String>) {
    store.putObject("zones", JSONObject(map))
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
