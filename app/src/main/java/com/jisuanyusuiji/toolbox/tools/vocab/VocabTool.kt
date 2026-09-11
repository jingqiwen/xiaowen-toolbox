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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.data.JsonStore
import com.jisuanyusuiji.toolbox.data.Net
import com.jisuanyusuiji.toolbox.data.Prefs
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

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

private val POS_PREFIX = Regex("^([A-Za-z]{1,6}\\.|\\[[^\\]]{1,16}\\])\\s*(.*)$")

/**
 * 把 ECDICT 释义排版成“每个词性一行”：
 *   vt. 放弃, 抛弃
 *   n. 放任
 * →
 *   【vt.】放弃, 抛弃
 *   【n.】放任
 * 同时兼容字面量 \n（旧数据）。
 */
internal fun formatMeanings(raw: String): String {
    val lines = raw.replace("\\n", "\n").replace("\\r", "\r")
        .split('\n')
        .flatMap { it.split('\r') }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    if (lines.isEmpty()) return ""
    return lines.joinToString("\n") { line ->
        val m = POS_PREFIX.find(line)
        if (m != null && m.groupValues[2].isNotBlank()) {
            val tag = m.groupValues[1].let { if (it.startsWith("[")) it.trim('[', ']') else it }
            "【$tag】${m.groupValues[2]}"
        } else line
    }
}

private fun firstMeaningLine(raw: String): String =
    formatMeanings(raw).lineSequence().firstOrNull()?.take(80) ?: raw.take(80)

private fun wordForms(w: VocabWord): List<Pair<String, String>> = buildList {
    if (w.thirdPerson.isNotBlank()) add("第三人称单数" to w.thirdPerson)
    if (w.ingForm.isNotBlank()) add("现在分词" to w.ingForm)
    if (w.pastTense.isNotBlank()) add("过去式" to w.pastTense)
    if (w.pastParticiple.isNotBlank()) add("过去分词" to w.pastParticiple)
    if (w.plural.isNotBlank()) add("复数" to w.plural)
    if (w.comparative.isNotBlank()) add("比较级" to w.comparative)
    if (w.superlative.isNotBlank()) add("最高级" to w.superlative)
}

private fun frequencyLabel(f: Int): String =
    if (f <= 0) "暂无数据" else "第 $f 名（数字越小越常用）"

// ---------- 在线词典 ----------

private data class OnlineEntry(
    val phonetic: String,
    val sections: List<Pair<String, String>>,
    val synonyms: List<String>
)

private fun parseOnlineJson(json: String): OnlineEntry? = try {
    val arr = JSONArray(json)
    if (arr.length() == 0) null else {
        val entry = arr.getJSONObject(0)
        var phonetic = entry.optString("phonetic")
        if (phonetic.isBlank()) {
            val ps = entry.optJSONArray("phonetics")
            if (ps != null) {
                for (i in 0 until ps.length()) {
                    val t = ps.optJSONObject(i)?.optString("text") ?: ""
                    if (t.isNotBlank()) { phonetic = t; break }
                }
            }
        }
        val sections = mutableListOf<Pair<String, String>>()
        val synonyms = linkedSetOf<String>()
        val meanings = entry.optJSONArray("meanings")
        if (meanings != null) {
            for (i in 0 until meanings.length()) {
                val m = meanings.optJSONObject(i) ?: continue
                val pos = m.optString("partOfSpeech")
                val defs = m.optJSONArray("definitions")
                val sb = StringBuilder()
                if (defs != null) {
                    for (j in 0 until minOf(defs.length(), 4)) {
                        val d = defs.optJSONObject(j) ?: continue
                        val def = d.optString("definition")
                        if (def.isNotBlank()) sb.append("• ").append(def).append('\n')
                        val ex = d.optString("example")
                        if (ex.isNotBlank()) sb.append("  例：").append(ex).append('\n')
                    }
                }
                m.optJSONArray("synonyms")?.let { s ->
                    for (k in 0 until minOf(s.length(), 8)) {
                        val v = s.optString(k)
                        if (v.isNotBlank()) synonyms.add(v)
                    }
                }
                if (sb.isNotBlank()) sections.add(pos to sb.toString().trim())
            }
        }
        OnlineEntry(phonetic, sections, synonyms.toList())
    }
} catch (_: Exception) {
    null
}

// ============================================================
// 背单词主界面
// ============================================================
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
    var shuffle by remember { mutableStateOf(true) }
    var reshuffle by remember { mutableStateOf(0) }
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
            "精选精讲" -> VocabData.builtIn
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
                (query.isBlank() || word.word.contains(query, true) ||
                    word.meanings.contains(query, true) || word.phrases.contains(query, true))
        }
    }
    // 背单词用的列表：与词库筛选/分区标记解耦，保证“自动下一个”稳定；乱序版每次洗牌
    val studyWords = remember(levelList, zoneFilter, shuffle, reshuffle) {
        val base = if (zoneFilter == "全部") levelList
        else levelList.filter { zones[it.word] == zoneFilter }.ifEmpty { levelList }
        if (shuffle) base.shuffled() else base
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
            "掌握 $knownCount · 不熟悉 $fuzzyCount · 完全不认识 $unknownCount（所有单词点开都是完整精讲）",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        ChoiceChips(
            listOf("精选精讲", "考研", "四级", "六级", "我的词库"),
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
        }
        Spacer(Modifier.height(6.dp))
        ChoiceChips(
            listOf("全部", ZONE_UNKNOWN, ZONE_FUZZY, ZONE_KNOWN),
            zoneFilter,
            { zoneFilter = it },
            { if (it == "全部") "全部分区" else zoneLabel(it) }
        )
        if (mode == "背单词") {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChoiceChips(
                    options = listOf(false, true),
                    selected = shuffle,
                    onSelect = { shuffle = it; reshuffle++ },
                    label = { if (it) "🔀 乱序版" else "顺序版" },
                    modifier = Modifier.weight(1f)
                )
                if (shuffle) {
                    TextButton(onClick = { reshuffle++ }) { Text("重新洗牌") }
                }
            }
        }
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
                            Text(firstMeaningLine(word.meanings), style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                        }
                    }
                }
                if (filtered.isEmpty()) {
                    item { Text("没有找到单词", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        } else {
            StudyMode(
                words = studyWords,
                zones = zones,
                onZone = { word, zone -> markZone(word, zone) },
                onClearZone = { clearZone(it) },
                onReshuffle = { if (shuffle) reshuffle++ }
            )
        }
    }

    detail?.let { word ->
        WordDetailDialog(
            store = store,
            word = word,
            zones = zones,
            onMark = { w, z -> markZone(w, z) },
            onClose = { detail = null }
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

// ============================================================
// 单词精讲详情（所有单词通用，含在线补充）
// ============================================================
@Composable
private fun WordDetailDialog(
    store: JsonStore,
    word: VocabWord,
    zones: Map<String, String>,
    onMark: (String, String?) -> Unit,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var online by remember(word.word) { mutableStateOf<OnlineEntry?>(null) }
    var onlineLoading by remember(word.word) { mutableStateOf(false) }
    var onlineError by remember(word.word) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Column {
                Text(word.word, fontWeight = FontWeight.Bold)
                Text(
                    word.phonetic.ifBlank { "（无音标）" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                DetailBlock("释义（按词性排版）", formatMeanings(word.meanings))
                val forms = wordForms(word)
                if (forms.isNotEmpty()) {
                    DetailBlock("词形变化", forms.joinToString("\n") { "${it.first}：${it.second}" })
                }
                if (word.usage.isNotBlank()) DetailBlock("用法", word.usage)
                if (word.phrases.isNotBlank()) DetailBlock("固定搭配 / 短语", word.phrases)
                if (word.example.isNotBlank()) DetailBlock("例句", word.example)
                if (word.derivatives.isNotBlank()) DetailBlock("派生词", word.derivatives)
                if (word.synonyms.isNotBlank()) DetailBlock("近义词", word.synonyms)
                if (word.antonyms.isNotBlank()) DetailBlock("反义词", word.antonyms)
                if (word.similar.isNotBlank()) DetailBlock("形近词", word.similar)
                if (word.definition.isNotBlank()) DetailBlock("英文释义", word.definition)
                DetailBlock("词频", frequencyLabel(word.frequency))
                if (word.collins.isNotBlank() && word.collins != "0") DetailBlock("柯林斯星级", word.collins)
                if (word.tags.isNotBlank()) DetailBlock("词库标签", word.tags.uppercase())

                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (!Prefs.networkEnabled.value) {
                                onlineError = "已在【设置 → 允许联网】中关闭网络"
                                return@OutlinedButton
                            }
                            val cached = store.getString("online_${word.word}")
                            if (cached != null) {
                                online = parseOnlineJson(cached) ?: run { onlineError = "缓存解析失败"; null }
                                return@OutlinedButton
                            }
                            onlineLoading = true
                            onlineError = ""
                            scope.launch {
                                val url = "https://api.dictionaryapi.dev/api/v2/entries/en/" +
                                    URLEncoder.encode(word.word, "UTF-8")
                                val json = Net.get(url)
                                onlineLoading = false
                                if (json == null) {
                                    onlineError = "联网失败：无网络或词典服务暂时不可用"
                                } else {
                                    val e = parseOnlineJson(json)
                                    if (e == null) onlineError = "在线词典暂未收录该词" else {
                                        online = e
                                        store.putString("online_${word.word}", json)
                                    }
                                }
                            }
                        },
                        enabled = !onlineLoading
                    ) { Text(if (online == null) "🌐 在线补充释义 / 例句" else "🌐 已获取在线内容") }
                    if (onlineLoading) CircularProgressIndicator(Modifier.height(20.dp))
                }
                if (onlineError.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(onlineError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                online?.let { o ->
                    if (o.phonetic.isNotBlank()) DetailBlock("在线音标", o.phonetic)
                    o.sections.forEach { (pos, text) -> DetailBlock("在线释义【${pos}】", text) }
                    if (o.synonyms.isNotEmpty()) DetailBlock("在线近义词", o.synonyms.joinToString(", "))
                }

                Spacer(Modifier.height(10.dp))
                Text("加入分区", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = { onMark(word.word, ZONE_KNOWN) }, modifier = Modifier.fillMaxWidth()) { Text("✅ 掌握") }
                    OutlinedButton(onClick = { onMark(word.word, ZONE_FUZZY) }, modifier = Modifier.fillMaxWidth()) { Text("🤔 不熟悉（模糊）") }
                    OutlinedButton(onClick = { onMark(word.word, ZONE_UNKNOWN) }, modifier = Modifier.fillMaxWidth()) { Text("😵 完全不认识") }
                    if (zones.containsKey(word.word)) {
                        TextButton(onClick = { onMark(word.word, null) }, modifier = Modifier.fillMaxWidth()) { Text("移出分区") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("关闭") } }
    )
}

@Composable
private fun DetailBlock(title: String, value: String) {
    if (value.isBlank()) return
    Spacer(Modifier.height(8.dp))
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(Modifier.height(2.dp))
    value.split('\n').filter { it.isNotBlank() }.forEach { line ->
        Text(line, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * 背单词模式：一次显示一个单词，显示释义后点“完全不认识 / 不熟悉 / 掌握”，
 * 自动进入下一个单词；下方是不认识区 / 不熟悉区 / 掌握区三个分区。
 */
@Composable
private fun StudyMode(
    words: List<VocabWord>,
    zones: Map<String, String>,
    onZone: (String, String?) -> Unit,
    onClearZone: (String) -> Unit,
    onReshuffle: () -> Unit
) {
    var index by remember { mutableStateOf(0) }
    var revealed by remember { mutableStateOf(false) }

    if (words.isEmpty()) {
        Text("词库为空", modifier = Modifier.padding(16.dp))
        return
    }
    LaunchedEffect(words) { index = 0; revealed = false }
    val safeIndex = ((index % words.size) + words.size) % words.size
    val word = words[safeIndex]

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${safeIndex + 1} / ${words.size}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                zones[word.word]?.let {
                    Text(
                        "当前标记：${zoneLabel(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                TextButton(onClick = {
                    index = (safeIndex + 1) % words.size
                    revealed = false
                }) { Text("跳过 ›") }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(word.word, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text(word.phonetic, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    if (revealed) {
                        formatMeanings(word.meanings).split('\n').filter { it.isNotBlank() }.forEach { line ->
                            Text(line, style = MaterialTheme.typography.bodyLarge)
                        }
                        if (word.phrases.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text("搭配：${word.phrases}", style = MaterialTheme.typography.bodyMedium)
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
                        index = (safeIndex + 1) % words.size
                        revealed = false
                    }, modifier = Modifier.fillMaxWidth()) { Text("😵 完全不认识") }
                    OutlinedButton(onClick = {
                        onZone(word.word, ZONE_FUZZY)
                        index = (safeIndex + 1) % words.size
                        revealed = false
                    }, modifier = Modifier.fillMaxWidth()) { Text("🤔 不熟悉（模糊）") }
                    Button(onClick = {
                        onZone(word.word, ZONE_KNOWN)
                        index = (safeIndex + 1) % words.size
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
                "📂 单词分区（共 ${zones.size} 词已标记，点击分区中的单词可跳转；乱序模式可点上方“重新洗牌”）",
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
                    if (i >= 0) { index = i; revealed = false }
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
                    if (i >= 0) { index = i; revealed = false }
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
                    if (i >= 0) { index = i; revealed = false }
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

// ============================================================
// 数据读取 / 保存
// ============================================================

private fun loadBulkWords(context: Context): List<VocabWord> = try {
    val curated = VocabData.builtIn.associateBy { it.word.lowercase() }
    context.assets.open("vocab_bulk.jsonl").bufferedReader().useLines { lines ->
        lines.mapNotNull { line ->
            try {
                val o = JSONObject(line)
                val exchange = o.optString("e")
                fun ex(key: String): String =
                    Regex("(?:^|/)" + Regex.escape(key) + ":([^/]+)").find(exchange)?.groupValues?.getOrNull(1) ?: ""
                val base = VocabWord(
                    word = o.optString("w"),
                    phonetic = o.optString("p"),
                    meanings = o.optString("t").replace("\\n", "\n"),
                    usage = o.optString("s"),
                    pastTense = ex("p"),
                    pastParticiple = ex("d"),
                    plural = ex("s"),
                    thirdPerson = ex("3"),
                    ingForm = ex("i"),
                    comparative = ex("r"),
                    superlative = ex("t"),
                    definition = o.optString("d"),
                    frequency = o.optInt("f", 0),
                    tags = o.optString("g"),
                    collins = o.optString("c")
                ).takeIf { it.word.isNotBlank() && it.meanings.isNotBlank() } ?: return@mapNotNull null
                // 若该词在“精选精讲”里有搭配/例句/近义等人工内容，合并进来
                val c = curated[base.word.lowercase()]
                if (c == null) base else base.copy(
                    usage = base.usage.ifBlank { c.usage },
                    phrases = c.phrases.ifBlank { base.phrases },
                    example = c.example.ifBlank { base.example },
                    derivatives = c.derivatives.ifBlank { base.derivatives },
                    synonyms = c.synonyms.ifBlank { base.synonyms },
                    antonyms = c.antonyms.ifBlank { base.antonyms },
                    similar = c.similar.ifBlank { base.similar }
                )
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
                similar = o.optString("similar"),
                definition = o.optString("definition"),
                plural = o.optString("plural"),
                thirdPerson = o.optString("thirdPerson"),
                ingForm = o.optString("ingForm"),
                comparative = o.optString("comparative"),
                superlative = o.optString("superlative"),
                frequency = o.optInt("frequency", 0)
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
                .put("definition", w.definition).put("plural", w.plural)
                .put("thirdPerson", w.thirdPerson).put("ingForm", w.ingForm)
                .put("comparative", w.comparative).put("superlative", w.superlative)
                .put("frequency", w.frequency)
        )
    }
    store.putArray("custom", arr)
}
