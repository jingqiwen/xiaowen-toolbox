package com.jisuanyusuiji.toolbox.ui.screens

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.data.Prefs
import com.jisuanyusuiji.toolbox.data.StoreCleaner
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val themeMode by Prefs.themeMode.collectAsState()
    val soundEnabled by Prefs.soundEnabled.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    var showAgreement by remember { mutableStateOf(false) }

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

            SectionCard(title = "💾 本地数据") {
                Text(
                    "所有模板、名单、历史记录都只保存在这台手机上。",
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
                Text("小温工具箱 v0.5.0")
                Spacer(Modifier.height(6.dp))
                Text(
                    "已包含 69 个工具：随机 10、影音图像 12、计算转换 21、文本编码 10、图鉴查询 4、实用 12。\n全部本地离线运行，不上传任何数据。",
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
