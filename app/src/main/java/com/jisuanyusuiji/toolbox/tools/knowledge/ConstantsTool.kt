package com.jisuanyusuiji.toolbox.tools.knowledge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.CopyButton

/** 独立的科学常数表工具（与 fx-999 内的常量表共用同一份数据）。 */
@Composable
fun ConstantsTool() {
    var query by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("全部") }

    val groups = remember { listOf("全部") + ScienceConstants.groups }
    val items = remember(query, group) {
        ScienceConstants.all.filter { c ->
            (group == "全部" || c.group == group) &&
                (query.isBlank() ||
                    c.name.contains(query, true) ||
                    c.symbol.contains(query, true) ||
                    c.value.contains(query, true) ||
                    c.unit.contains(query, true) ||
                    c.note.contains(query, true))
        }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "🔬 科学常量表 · ${items.size} / ${ScienceConstants.all.size} 条",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("搜索名称 / 符号 / 数值 / 单位") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
        ChoiceChips(
            options = groups,
            selected = group,
            onSelect = { group = it },
            label = { it },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items, key = { it.group + it.name + it.symbol }) { c ->
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "${c.name}（${c.symbol}）",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            c.group,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (c.unit.isBlank()) c.value else "${c.value} ${c.unit}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (c.note.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                c.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        CopyButton(text = "${c.name}（${c.symbol}）= ${c.value} ${c.unit}".trim())
                    }
                }
            }
            if (items.isEmpty()) {
                item { Text("没有找到相关常量", modifier = Modifier.padding(16.dp)) }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
