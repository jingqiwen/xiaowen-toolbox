package com.jisuanyusuiji.toolbox.tools.knowledge

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips

// ============================================================
// 车标图鉴：只展示图标（文字徽标）与名字
// ============================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CarLogoTool() {
    var query by remember { mutableStateOf("") }
    val items = remember(query) {
        CarLogoData.items.filter {
            query.isBlank() || it.name.contains(query, true) || it.badge.contains(query, true)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "🚗 车标图鉴 · ${items.size} 个",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.size(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("搜索车标名称") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        Spacer(Modifier.size(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)
        ) {
            gridItems(items, key = { it.name }) { car ->
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier
                                .size(46.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                car.badge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1
                            )
                        }
                        Spacer(Modifier.size(6.dp))
                        Text(car.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    }
                }
            }
        }
    }
}

// ============================================================
// 电脑快捷键查询（Windows + macOS，常用 + 进阶）
// ============================================================
@Composable
fun ShortcutTool() {
    var query by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("全部") }
    var level by remember { mutableStateOf("全部") }

    val filtered = remember(query, platform, level) {
        ShortcutData.items.filter { item ->
            (platform == "全部" || item.platform == platform) &&
                (level == "全部" || item.level == level) &&
                (query.isBlank() ||
                    item.keys.contains(query, true) ||
                    item.action.contains(query, true) ||
                    item.category.contains(query, true) ||
                    item.platform.contains(query, true))
        }
    }
    val grouped = filtered.groupBy { it.category }

    Column(Modifier.fillMaxSize()) {
        Text(
            "⌨️ 电脑快捷键 · ${filtered.size} 条",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.size(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("搜索快捷键 / 功能") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        Spacer(Modifier.size(8.dp))
        ChoiceChips(
            options = listOf("全部", "Windows", "macOS"),
            selected = platform,
            onSelect = { platform = it },
            label = { it },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.size(8.dp))
        ChoiceChips(
            options = listOf("全部", "常用", "进阶"),
            selected = level,
            onSelect = { level = it },
            label = { it },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.size(8.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            grouped.forEach { (category, list) ->
                item {
                    Text(
                        category,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                items(list, key = { it.keys + it.action + it.platform }) { item ->
                    ShortcutCard(item)
                }
            }
            if (filtered.isEmpty()) {
                item { Text("没有找到相关快捷键", modifier = Modifier.padding(16.dp)) }
            }
            item { Spacer(Modifier.size(16.dp)) }
        }
    }
}

@Composable
private fun ShortcutCard(item: ShortcutItem) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    item.keys,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.action, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${item.platform} · ${item.level}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
