package com.jisuanyusuiji.toolbox.tools.random

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.ui.Sfx
import com.jisuanyusuiji.toolbox.ui.components.CopyButton
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.ResultText
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import com.jisuanyusuiji.toolbox.ui.components.ShareButton
import com.jisuanyusuiji.toolbox.ui.components.Stepper

@Composable
fun ListShuffleTool() {
    var inputText by remember {
        mutableStateOf("第1项\n第2项\n第3项\n第4项\n第5项\n第6项\n第7项\n第8项\n第9项\n第10项")
    }
    var groupEnabled by remember { mutableStateOf(false) }
    var groupSize by remember { mutableStateOf(2) }
    var outputLines by remember { mutableStateOf(listOf<String>()) }
    var error by remember { mutableStateOf("") }

    fun shuffle() {
        val lines = inputText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        error = ""
        outputLines = emptyList()
        if (lines.isEmpty()) {
            error = "请先粘贴多行文本"
            return
        }
        Sfx.tick()
        val shuffled = lines.shuffled()
        outputLines = if (groupEnabled && groupSize > 1) {
            shuffled.chunked(groupSize).mapIndexed { index, group ->
                "第${index + 1}组：" + group.joinToString("、")
            }
        } else {
            shuffled.mapIndexed { index, line -> "${index + 1}. $line" }
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "📄 原始列表（每行一项）") {
            LabeledField(
                value = inputText,
                onChange = { inputText = it },
                label = "粘贴多行文本",
                singleLine = false,
                minLines = 6
            )
        }

        SectionCard(title = "打乱设置") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("随机分组模式", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "关闭时仅打乱顺序；开启后按组输出",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = groupEnabled, onCheckedChange = { groupEnabled = it })
            }
            if (groupEnabled) {
                Spacer(Modifier.height(8.dp))
                Stepper("每组人数", groupSize, { groupSize = it }, 2..100)
            }
        }

        ErrorText(error)
        Button(onClick = { shuffle() }, modifier = Modifier.fillMaxWidth()) {
            Text("🔀 打乱", style = MaterialTheme.typography.titleMedium)
        }

        if (outputLines.isNotEmpty()) {
            SectionCard(title = "结果") {
                ResultText(outputLines.joinToString("\n"))
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CopyButton(outputLines.joinToString("\n"), modifier = Modifier.weight(1f))
                    ShareButton(outputLines.joinToString("\n"), title = "导出打乱结果", modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
