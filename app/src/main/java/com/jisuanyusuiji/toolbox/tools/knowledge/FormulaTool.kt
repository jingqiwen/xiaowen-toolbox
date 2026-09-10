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

@Composable
fun FormulaTool() {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("全部") }

    val categories = remember { listOf("全部") + FormulaData.all.map { it.category }.distinct() }
    val items = remember(query, category) {
        FormulaData.all.filter { item ->
            (category == "全部" || item.category == category) &&
                (query.isBlank() ||
                    item.name.contains(query, true) ||
                    item.expression.contains(query, true) ||
                    item.note.contains(query, true))
        }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "🧮 数学 / 物理公式 · ${items.size} / ${FormulaData.all.size} 条",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("搜索公式名称 / 表达式 / 说明") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
        ChoiceChips(
            options = categories,
            selected = category,
            onSelect = { category = it },
            label = { it },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items, key = { it.category + it.name }) { formula ->
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "${formula.name}  ·  ${formula.category}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            formula.expression,
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            formula.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        CopyButton(text = "${formula.name}\n${formula.expression}\n${formula.note}")
                    }
                }
            }
            if (items.isEmpty()) {
                item { Text("没有找到相关公式", modifier = Modifier.padding(16.dp)) }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
