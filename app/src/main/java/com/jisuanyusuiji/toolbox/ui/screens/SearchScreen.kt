package com.jisuanyusuiji.toolbox.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.data.ToolRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onOpenTool: (String) -> Unit, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = remember(query) {
        if (query.isBlank()) ToolRegistry.all
        else ToolRegistry.all.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.desc.contains(query, ignoreCase = true) ||
                it.id.contains(query, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("全局搜索") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("‹ 返回") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("输入工具名称或关键词") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(results, key = { it.id }) { tool ->
                    Card(onClick = { onOpenTool(tool.id) }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp)) {
                            if (tool.iconRes != null) {
                                Image(
                                    painter = painterResource(tool.iconRes),
                                    contentDescription = tool.name,
                                    modifier = Modifier.size(28.dp)
                                )
                            } else {
                                Text(tool.icon, style = MaterialTheme.typography.headlineSmall)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(tool.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${tool.category.title} · ${tool.desc}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                if (results.isEmpty()) {
                    item { Text("没有找到相关工具", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}
