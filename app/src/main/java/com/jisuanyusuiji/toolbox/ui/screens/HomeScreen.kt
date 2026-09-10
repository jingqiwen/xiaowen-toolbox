package com.jisuanyusuiji.toolbox.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.data.Prefs
import com.jisuanyusuiji.toolbox.data.ToolCategory
import com.jisuanyusuiji.toolbox.data.ToolDef
import com.jisuanyusuiji.toolbox.data.ToolRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenTool: (String) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit
) {
    // 每次回到首页时刷新收藏和最近使用
    var refresh by remember { mutableStateOf(0) }
    val favorites = remember(refresh) { Prefs.favorites().mapNotNull(ToolRegistry::byId) }
    val recents = remember(refresh) { Prefs.recents().mapNotNull(ToolRegistry::byId) }
    fun reload() { refresh++ }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("小温工具箱") },
                actions = {
                    TextButton(onClick = onSettings) { Text("⚙ 设置") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                // 全局搜索入口
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSearch)
                ) {
                    Text(
                        "🔍  搜索工具名称…",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (favorites.isNotEmpty()) {
                item { SectionTitle("⭐ 收藏夹") }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(favorites, key = { it.id }) { tool ->
                            MiniToolCard(tool, onClick = { onOpenTool(tool.id) })
                        }
                    }
                }
            }

            if (recents.isNotEmpty()) {
                item { SectionTitle("🕘 最近使用") }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(recents, key = { it.id }) { tool ->
                            MiniToolCard(tool, onClick = { onOpenTool(tool.id) })
                        }
                    }
                }
            }

            ToolCategory.entries.forEach { category ->
                val tools = ToolRegistry.byCategory(category)
                if (tools.isNotEmpty()) {
                    item { SectionTitle(category.title) }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            tools.chunked(2).forEach { rowTools ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowTools.forEach { tool ->
                                        ToolCard(
                                            tool = tool,
                                            onClick = { onOpenTool(tool.id) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (rowTools.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "所有数据仅保存在本机 · 无任何网络权限",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
    // 返回首页时自动刷新一次
    androidx.compose.runtime.LaunchedEffect(Unit) { reload() }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ToolCard(tool: ToolDef, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 104.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(tool.icon, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(tool.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(
                tool.desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MiniToolCard(tool: ToolDef, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.width(128.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(tool.icon)
            Spacer(Modifier.width(8.dp))
            Text(tool.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
