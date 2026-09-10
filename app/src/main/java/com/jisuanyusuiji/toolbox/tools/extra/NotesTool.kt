package com.jisuanyusuiji.toolbox.tools.extra

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.data.JsonStore
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class NoteItem(
    val id: Long,
    var title: String,
    var content: String,
    var updated: String
)

private fun loadNotes(store: JsonStore): MutableList<NoteItem> {
    val arr = store.getArray("notes")
    return (0 until arr.length()).mapNotNull { i ->
        try {
            val o = arr.getJSONObject(i)
            NoteItem(o.getLong("id"), o.getString("title"), o.getString("content"), o.optString("updated"))
        } catch (_: Exception) {
            null
        }
    }.toMutableList()
}

private fun saveNotes(store: JsonStore, notes: List<NoteItem>) {
    val arr = JSONArray()
    notes.forEach { n ->
        arr.put(
            JSONObject()
                .put("id", n.id)
                .put("title", n.title)
                .put("content", n.content)
                .put("updated", n.updated)
        )
    }
    store.putArray("notes", arr)
}

@Composable
fun NotesTool() {
    val context = LocalContext.current
    val store = remember { JsonStore(context, "备忘录") }
    var notes by remember { mutableStateOf(loadNotes(store)) }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<NoteItem?>(null) }
    var isNew by remember { mutableStateOf(false) }

    val filtered = notes.filter {
        query.isBlank() || it.title.contains(query, true) || it.content.contains(query, true)
    }.sortedByDescending { it.updated }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("搜索备忘录") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.padding(4.dp))
            Button(onClick = {
                isNew = true
                editing = NoteItem(
                    id = System.currentTimeMillis(),
                    title = "",
                    content = "",
                    updated = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                )
            }) { Text("＋ 新建") }
        }
        Spacer(Modifier.height(10.dp))
        if (filtered.isEmpty()) {
            Text("暂无备忘录", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filtered, key = { it.id }) { note ->
                Card(
                    onClick = { isNew = false; editing = note },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            note.title.ifBlank { "（无标题）" },
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (note.content.isNotBlank()) {
                            Text(
                                note.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3
                            )
                        }
                        Text(
                            note.updated,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }

    editing?.let { note ->
        var title by remember(note.id) { mutableStateOf(note.title) }
        var content by remember(note.id) { mutableStateOf(note.content) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (isNew) "新建备忘录" else "编辑备忘录") },
            text = {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("标题") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("内容") },
                        minLines = 6,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                    if (isNew) {
                        notes = (notes + NoteItem(note.id, title, content, now)).toMutableList()
                    } else {
                        note.title = title
                        note.content = content
                        note.updated = now
                        notes = notes.toMutableList()
                    }
                    saveNotes(store, notes)
                    editing = null
                }) { Text("保存") }
            },
            dismissButton = {
                Row {
                    if (!isNew) {
                        TextButton(onClick = {
                            notes = notes.filterNot { it.id == note.id }.toMutableList()
                            saveNotes(store, notes)
                            editing = null
                        }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                    }
                    TextButton(onClick = { editing = null }) { Text("取消") }
                }
            }
        )
    }
}
