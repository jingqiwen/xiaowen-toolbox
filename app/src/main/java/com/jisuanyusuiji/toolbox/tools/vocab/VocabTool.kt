package com.jisuanyusuiji.toolbox.tools.vocab

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jisuanyusuiji.toolbox.data.JsonStore
import com.jisuanyusuiji.toolbox.data.Net
import com.jisuanyusuiji.toolbox.data.Prefs
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

// ============================================================
// 艾宾浩斯复习计划
// ============================================================

/** 一个单词的复习状态：当前记忆级别、下次复习时间、上次复习时间、复习次数。 */
private data class ReviewState(val stage: Int, val dueAt: Long, val lastAt: Long, val reps: Int)

/** 艾宾浩斯复习间隔：10 分钟 → 12 小时 → 1/2/4/7/15/30/60 天，每通过一次复习进入下一级。 */
private val REVIEW_INTERVALS = listOf(
    10 * 60_000L,
    12 * 60 * 60_000L,
    24 * 60 * 60_000L,
    2 * 24 * 60 * 60_000L,
    4 * 24 * 60 * 60_000L,
    7 * 24 * 60 * 60_000L,
    15 * 24 * 60 * 60_000L,
    30 * 24 * 60 * 60_000L,
    60 * 24 * 60 * 60_000L
)
private val REVIEW_LABELS = listOf("10 分钟", "12 小时", "1 天", "2 天", "4 天", "7 天", "15 天", "30 天", "60 天")

private fun stageLabel(stage: Int): String =
    "第 ${stage.coerceIn(0, REVIEW_LABELS.size - 1) + 1} 级 · 间隔 ${REVIEW_LABELS[stage.coerceIn(0, REVIEW_LABELS.size - 1)]}"

private fun loadSchedule(store: JsonStore): MutableMap<String, ReviewState> {
    val obj = store.getObject("schedule")
    val map = mutableMapOf<String, ReviewState>()
    obj.keys().forEach { key ->
        try {
            val a = obj.getJSONArray(key)
            map[key] = ReviewState(a.getInt(0), a.getLong(1), a.getLong(2), a.getInt(3))
        } catch (_: Exception) {
        }
    }
    return map
}

private fun saveSchedule(store: JsonStore, map: Map<String, ReviewState>) {
    val obj = JSONObject()
    map.forEach { (key, s) ->
        try {
            obj.put(key, JSONArray().put(s.stage).put(s.dueAt).put(s.lastAt).put(s.reps))
        } catch (_: Exception) {
        }
    }
    store.putObject("schedule", obj)
}

/** 根据“完全不认识 / 不熟悉 / 掌握”推进或回退记忆级别。 */
private fun advance(schedule: Map<String, ReviewState>, word: String, zone: String?): MutableMap<String, ReviewState> {
    val now = System.currentTimeMillis()
    val next = schedule.toMutableMap()
    val current = schedule[word]
    when (zone) {
        null -> next.remove(word)
        ZONE_UNKNOWN -> next[word] = ReviewState(0, now + REVIEW_INTERVALS[0], now, (current?.reps ?: 0) + 1)
        ZONE_FUZZY -> {
            val stage = ((current?.stage ?: 1) - 1).coerceAtLeast(0)
            next[word] = ReviewState(stage, now + REVIEW_INTERVALS[stage], now, (current?.reps ?: 0) + 1)
        }
        else -> {
            val stage = ((current?.stage ?: -1) + 1).coerceAtMost(REVIEW_INTERVALS.size - 1)
            next[word] = ReviewState(stage, now + REVIEW_INTERVALS[stage], now, (current?.reps ?: 0) + 1)
        }
    }
    return next
}

private fun dueLabel(dueAt: Long, now: Long = System.currentTimeMillis()): String {
    val diff = dueAt - now
    return when {
        diff <= 0 -> "已到期"
        diff < 60 * 60_000L -> "${diff / 60_000L + 1} 分钟后"
        diff < 12 * 60 * 60_000L -> "今天 " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(dueAt))
        diff < 24 * 60 * 60_000L -> "明天前"
        diff < 2 * 24 * 60 * 60_000L -> "明天 " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(dueAt))
        diff < 7 * 24 * 60 * 60_000L -> "${diff / (24 * 60 * 60_000L)} 天后"
        else -> SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(dueAt))
    }
}

private fun bucketLabel(dueAt: Long, now: Long = System.currentTimeMillis()): String {
    val diff = dueAt - now
    return when {
        diff <= 0 -> "⏰ 已到期（现在复习）"
        diff < 24 * 60 * 60_000L -> "📅 今天"
        diff < 2 * 24 * 60 * 60_000L -> "📅 明天"
        diff < 3 * 24 * 60 * 60_000L -> "📅 3 天内"
        diff < 7 * 24 * 60 * 60_000L -> "📅 7 天内"
        else -> "📅 以后"
    }
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
    var schedule by remember { mutableStateOf(loadSchedule(store)) }
    var reviewOnly by remember { mutableStateOf(false) }
    var reviewSession by remember { mutableStateOf<List<VocabWord>>(emptyList()) }
    var scheduleTick by remember { mutableStateOf(0) }

    // 背单词页：向下滑时顶部分类/设置区收起，向上滑或滑回顶部时再展开
    var headerVisible by remember { mutableStateOf(true) }
    val studyListState = rememberLazyListState()
    val headerNestedScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -1.2f) headerVisible = false
                if (available.y > 1.2f) headerVisible = true
                return Offset.Zero
            }
        }
    }
    // 每次进入“背单词”先把工具栏展开，避免刚从别的页面切回来时是收起状态
    LaunchedEffect(mode) {
        if (mode == "背单词") headerVisible = true
    }

    fun markZone(word: String, zone: String?) {
        val next = zones.toMutableMap()
        if (zone == null) next.remove(word) else next[word] = zone
        zones = next
        saveZoneMap(store, zones)
        // 同步推进艾宾浩斯复习计划
        schedule = advance(schedule, word, zone)
        saveSchedule(store, schedule)
        scheduleTick++
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
    // 到期待复习的单词
    val dueWords = remember(levelList, schedule, scheduleTick) {
        val nowMs = System.currentTimeMillis()
        levelList.filter { w -> schedule[w.word]?.let { it.dueAt <= nowMs } == true }
    }
    // 背单词用的列表：与词库筛选/分区标记解耦，保证“自动下一个”稳定；乱序版每次洗牌
    val studyWords = remember(levelList, zoneFilter, shuffle, reshuffle, reviewOnly, reviewSession) {
        if (reviewOnly) {
            reviewSession.ifEmpty { levelList }
        } else {
            val base = if (zoneFilter == "全部") levelList
            else levelList.filter { zones[it.word] == zoneFilter }.ifEmpty { levelList }
            if (shuffle) base.shuffled() else base
        }
    }
    val knownCount = zones.values.count { it == ZONE_KNOWN }
    val fuzzyCount = zones.values.count { it == ZONE_FUZZY }
    val unknownCount = zones.values.count { it == ZONE_UNKNOWN }

    val screenScrollModifier = if (mode == "背单词") Modifier.nestedScroll(headerNestedScroll) else Modifier
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .then(screenScrollModifier)
    ) {
        // 背单词时整块“分类 / 级别 / 分区 / 模式”工具栏：下划收起，上划展开
        AnimatedVisibility(
            visible = headerVisible || mode != "背单词",
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                Text(
                    "📚 背单词 · ${level} · ${levelList.size} 词",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "掌握 $knownCount · 不熟悉 $fuzzyCount · 完全不认识 $unknownCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChoiceChips(
                        listOf("精选精讲", "考研", "四级", "六级", "我的词库"),
                        level,
                        { level = it },
                        {
                            when (it) {
                                "考研", "四级", "六级" -> "$it ${levelCounts[it] ?: 0}"
                                else -> it
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    if (level == "考研" || level == "四级" || level == "六级") {
                        TextButton(onClick = { includeBasic = !includeBasic }) {
                            Text(if (includeBasic) "含基础词 ✓" else "补全基础词", maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                ChoiceChips(
                    listOf("全部", ZONE_UNKNOWN, ZONE_FUZZY, ZONE_KNOWN),
                    zoneFilter,
                    { zoneFilter = it },
                    { if (it == "全部") "全部分区" else zoneLabel(it) }
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChoiceChips(
                        options = listOf("词库", "背单词", "复习计划"),
                        selected = mode,
                        onSelect = { selected ->
                            if (selected != mode) {
                                mode = selected
                                if (selected != "复习计划") reviewOnly = false
                            }
                        },
                        label = { it },
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showImport = true }) { Text("导入", maxLines = 1) }
                }
            }
        }
        Spacer(Modifier.height(6.dp))

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
        } else if (mode == "复习计划") {
            ReviewPlanPane(
                modifier = Modifier.weight(1f),
                dueWords = dueWords,
                allWords = levelList,
                schedule = schedule,
                onStart = {
                    reviewSession = dueWords
                    reviewOnly = true
                    mode = "背单词"
                },
                onOpenWord = { detail = it },
                onReset = {
                    schedule = mutableMapOf()
                    saveSchedule(store, schedule)
                    scheduleTick++
                }
            )
        } else {
            if (reviewOnly) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "🎯 今日复习模式 · ${studyWords.size} 词（按艾宾浩斯计划）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { reviewOnly = false }) { Text("退出复习") }
                }
                Spacer(Modifier.height(4.dp))
            }
            StudyMode(
                words = studyWords,
                zones = zones,
                schedule = schedule,
                listState = studyListState,
                shuffle = shuffle,
                onToggleShuffle = { shuffle = !shuffle; reshuffle++ },
                onReshuffle = { reshuffle++ },
                onZone = { word, zone -> markZone(word, zone) },
                onClearZone = { clearZone(it) }
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

// ============================================================
// 复习计划面板（艾宾浩斯曲线 + 到期/后续安排）
// ============================================================
@Composable
private fun ReviewPlanPane(
    modifier: Modifier,
    dueWords: List<VocabWord>,
    allWords: List<VocabWord>,
    schedule: Map<String, ReviewState>,
    onStart: () -> Unit,
    onOpenWord: (VocabWord) -> Unit,
    onReset: () -> Unit
) {
    val wordMap = remember(allWords) { allWords.associateBy { it.word } }
    val entries = remember(schedule, wordMap) {
        schedule.entries.sortedBy { it.value.dueAt }
            .mapNotNull { (w, st) -> wordMap[w]?.let { it to st } }
    }
    val groups = entries.groupBy { bucketLabel(it.second.dueAt) }

    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("🧠 艾宾浩斯复习计划", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "已安排 ${schedule.size} 词 · 现在到期 ${dueWords.size} 词 · 累计复习 ${schedule.values.sumOf { it.reps }} 次",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("📉 遗忘曲线与复习节点", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "每次复习都会把记忆拉回接近 100%，遗忘速度随之变慢。本 App 按 10 分钟 → 12 小时 → 1/2/4/7/15/30/60 天自动安排下一次复习。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    EbbinghausCurve()
                }
            }
        }
        item {
            Button(
                onClick = onStart,
                enabled = dueWords.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (dueWords.isEmpty()) "今天没有到期单词 🎉" else "▶ 开始今日复习（${dueWords.size} 词）")
            }
        }
        if (schedule.isEmpty()) {
            item {
                Text(
                    "还没有复习计划：去“背单词”给单词标记 😵 完全不认识 / 🤔 不熟悉 / ✅ 掌握，App 会自动按遗忘曲线安排复习。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            groups.forEach { (label, list) ->
                item {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                items(list, key = { it.first.word }) { pair ->
                    val w = pair.first
                    val st = pair.second
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(w.word, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "${stageLabel(st.stage)} · 下次：${dueLabel(st.dueAt)} · 已复习 ${st.reps} 次",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { onOpenWord(w) }) { Text("详情") }
                        }
                    }
                }
            }
            item {
                TextButton(onClick = onReset) { Text("清空复习计划（不影响单词分区）") }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

/** 示意性的艾宾浩斯遗忘曲线：多段指数衰减，复习点把保持率拉回 100%。 */
@Composable
private fun EbbinghausCurve() {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.fillMaxWidth().height(190.dp)) {
        val left = 34.dp.toPx()
        val bottom = size.height - 30.dp.toPx()
        val top = 12.dp.toPx()
        val right = size.width - 10.dp.toPx()
        drawLine(gridColor, Offset(left, top), Offset(left, bottom), strokeWidth = 2f)
        drawLine(gridColor, Offset(left, bottom), Offset(right, bottom), strokeWidth = 2f)

        val logMax = kotlin.math.ln(61f)
        fun xOf(days: Float): Float = left + (right - left) * (kotlin.math.ln(1f + days) / logMax)

        val nodes = listOf(0f, 0.5f, 1f, 2f, 4f, 7f, 15f, 30f, 60f)
        var px = xOf(0f)
        var py = top
        for (i in nodes.indices) {
            val start = nodes[i]
            val end = if (i + 1 < nodes.size) nodes[i + 1] else 60f
            // 每次复习后记忆强度翻倍：τ 指数增长
            val tau = 0.5f * Math.pow(2.0, i.toDouble()).toFloat()
            val steps = 26
            for (s in 1..steps) {
                val d = start + (end - start) * s / steps
                val retention = kotlin.math.exp(-(d - start) / tau)
                val x = xOf(d)
                val y = bottom - (bottom - top) * retention
                drawLine(lineColor, Offset(px, py), Offset(x, y), strokeWidth = 2.5f)
                px = x
                py = y
            }
            drawLine(
                lineColor.copy(alpha = 0.3f),
                Offset(xOf(start), top),
                Offset(xOf(start), bottom),
                strokeWidth = 1.5f
            )
            drawCircle(lineColor, radius = 3.5f, center = Offset(xOf(start), top))
        }

        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                color = textColor.toArgb()
                textSize = 11.sp.toPx()
                isAntiAlias = true
            }
            val axisPaint = android.graphics.Paint().apply {
                color = lineColor.toArgb()
                textSize = 11.sp.toPx()
                isAntiAlias = true
            }
            canvas.nativeCanvas.drawText("保持率", 2.dp.toPx(), top + 4.dp.toPx(), axisPaint)
            canvas.nativeCanvas.drawText("100%", 2.dp.toPx(), top + 18.dp.toPx(), paint)
            canvas.nativeCanvas.drawText("0", 2.dp.toPx(), bottom + 4.dp.toPx(), paint)
            listOf(2 to "1天", 4 to "4天", 6 to "15天", 8 to "60天").forEach { (index, text) ->
                canvas.nativeCanvas.drawText(
                    text,
                    xOf(nodes[index]) - 10.dp.toPx(),
                    bottom + 18.dp.toPx(),
                    paint
                )
            }
        }
    }
}

/**
 * 背单词模式：一次显示一个单词，显示释义后点“完全不认识 / 不熟悉 / 掌握”，
 * 自动进入下一个单词；单词大卡片固定在屏幕主要位置，下方是不认识区 / 不熟悉区 / 掌握区。
 */
@Composable
private fun StudyMode(
    words: List<VocabWord>,
    zones: Map<String, String>,
    schedule: Map<String, ReviewState>,
    listState: LazyListState,
    shuffle: Boolean,
    onToggleShuffle: () -> Unit,
    onReshuffle: () -> Unit,
    onZone: (String, String?) -> Unit,
    onClearZone: (String) -> Unit
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

    // 换词或换词库后回到顶部：刚标记完就能看到下一张单词卡
    LaunchedEffect(safeIndex, words) {
        if (listState.firstVisibleItemIndex != 0 || listState.firstVisibleItemScrollOffset != 0) {
            listState.animateScrollToItem(0)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${safeIndex + 1} / ${words.size}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onToggleShuffle,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) { Text(if (shuffle) "🔀 乱序版" else "顺序版", maxLines = 1) }
                if (shuffle) {
                    TextButton(
                        onClick = onReshuffle,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) { Text("洗牌", maxLines = 1) }
                }
                TextButton(
                    onClick = {
                        index = (safeIndex + 1) % words.size
                        revealed = false
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) { Text("跳过 ›", maxLines = 1) }
            }
        }
        // 主单词卡：固定占屏幕大半高度，内容太长时在卡片内部滚动
        item {
            Card(
                modifier = Modifier
                    .fillParentMaxWidth()
                    .fillParentMaxHeight(0.60f)
            ) {
                if (!revealed) {
                    Column(
                        Modifier.fillMaxSize().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(word.word, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text(word.phonetic, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        zones[word.word]?.let {
                            Spacer(Modifier.height(4.dp))
                            Text("当前标记：${zoneLabel(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        schedule[word.word]?.let { st ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "🧠 ${stageLabel(st.stage)} · 下次复习：${dueLabel(st.dueAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                        Text("点击下方“显示释义”查看答案", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(word.word, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text(word.phonetic, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        zones[word.word]?.let {
                            Spacer(Modifier.height(4.dp))
                            Text("当前标记：${zoneLabel(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        schedule[word.word]?.let { st ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "🧠 ${stageLabel(st.stage)} · 下次复习：${dueLabel(st.dueAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        formatMeanings(word.meanings).split('\n').filter { it.isNotBlank() }.forEach { line ->
                            Text(line, style = MaterialTheme.typography.bodyLarge)
                        }
                        if (word.phrases.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text("搭配：${word.phrases}", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(8.dp))
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
