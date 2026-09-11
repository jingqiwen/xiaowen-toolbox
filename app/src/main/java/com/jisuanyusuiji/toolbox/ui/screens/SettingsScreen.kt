package com.jisuanyusuiji.toolbox.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.BuildConfig
import com.jisuanyusuiji.toolbox.data.Net
import com.jisuanyusuiji.toolbox.data.Prefs
import com.jisuanyusuiji.toolbox.data.StoreCleaner
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import kotlinx.coroutines.launch
import org.json.JSONObject

private fun compareVersion(a: String, b: String): Int {
    val pa = a.split('.').mapNotNull { it.toIntOrNull() }
    val pb = b.split('.').mapNotNull { it.toIntOrNull() }
    for (i in 0 until maxOf(pa.size, pb.size)) {
        val x = pa.getOrElse(i) { 0 }
        val y = pb.getOrElse(i) { 0 }
        if (x != y) return x - y
    }
    return 0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val themeMode by Prefs.themeMode.collectAsState()
    val soundEnabled by Prefs.soundEnabled.collectAsState()
    val networkEnabled by Prefs.networkEnabled.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    var showAgreement by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var updateMsg by remember { mutableStateOf("") }
    var updateUrl by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun checkUpdate() {
        if (!networkEnabled) {
            updateMsg = "请先打开“允许联网”"
            return
        }
        checking = true
        updateMsg = ""
        updateUrl = ""
        scope.launch {
            val json = Net.get("https://api.github.com/repos/jingqiwen/xiaowen-toolbox/releases/latest")
            checking = false
            if (json == null) {
                updateMsg = "检查失败：无网络或服务暂时不可用"
                return@launch
            }
            try {
                val o = JSONObject(json)
                val tag = o.optString("tag_name")
                val url = o.optString("html_url")
                val latest = tag.trimStart('v', 'V')
                val current = BuildConfig.VERSION_NAME
                if (compareVersion(latest, current) > 0) {
                    updateMsg = "发现新版本 $tag（当前 v$current）"
                    updateUrl = url
                } else {
                    updateMsg = "已是最新版本 v$current"
                }
            } catch (_: Exception) {
                updateMsg = "更新信息解析失败"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("‹ 返回") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionCard(title = "🎨 外观主题") {
                ChoiceChips(
                    options = listOf(Prefs.THEME_SYSTEM, Prefs.THEME_LIGHT, Prefs.THEME_DARK),
                    selected = themeMode,
                    onSelect = { Prefs.setThemeMode(it) },
                    label = { mode ->
                        when (mode) {
                            Prefs.THEME_LIGHT -> "浅色"
                            Prefs.THEME_DARK -> "深色"
                            else -> "跟随系统"
                        }
                    }
                )
            }

            SectionCard(title = "🔊 音效") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("操作提示音", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "掷骰、抽签等随机操作时播放提示音",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { Prefs.setSoundEnabled(it) }
                    )
                }
            }

            SectionCard(title = "🌐 联网功能（可选）") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("允许联网", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "用于：背单词在线补充释义/例句、检查更新。\n不会上传任何个人数据；关闭后所有功能仍可离线使用。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = networkEnabled,
                        onCheckedChange = { Prefs.setNetworkEnabled(it) }
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { checkUpdate() }, enabled = !checking) {
                        Text(if (checking) "检查中…" else "🔄 检查更新")
                    }
                    if (checking) CircularProgressIndicator(Modifier.height(20.dp))
                    Text("当前版本 v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (updateMsg.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(updateMsg, style = MaterialTheme.typography.bodyMedium)
                }
                if (updateUrl.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl)))
                        } catch (_: Exception) {
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("打开下载页（GitHub）") }
                }
            }

            SectionCard(title = "💾 本地数据") {
                Text(
                    "所有模板、名单、历史记录、背单词分区都只保存在这台手机上。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🗑 一键清空全部本地数据") }
            }

            SectionCard(title = "📜 用户协议") {
                Text(
                    "您已同意《小温工具箱用户服务协议》。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showAgreement = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("查看用户协议") }
            }

            SectionCard(title = "ℹ️ 关于") {
                Text("小温工具箱 v${BuildConfig.VERSION_NAME}（Android 版）")
                Spacer(Modifier.height(6.dp))
                Text(
                    "共 78 个工具：随机 10、计算与转换 21、文本与编码 11、影音与图像 14、函数绘图 3、学习查询 5、实用工具 14。\n" +
                        "本 App 仅支持 Android，不提供 iOS / 网页版。\n" +
                        "默认完全离线可用；联网为可选功能（在线词典、检查更新），可在上方关闭。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("确认清空？") },
            text = { Text("将删除全部收藏、使用记录、名单、模板、历史和设置，此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    Prefs.clearAll()
                    StoreCleaner.clearAll(context)
                    showClearDialog = false
                }) { Text("确认清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }

    if (showAgreement) {
        AgreementDialog(onDismiss = { showAgreement = false })
    }
}
