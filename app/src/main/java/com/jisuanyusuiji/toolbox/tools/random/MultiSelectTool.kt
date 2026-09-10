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
import com.jisuanyusuiji.toolbox.ui.components.Stepper

@Composable
fun MultiSelectTool() {
    var optionsText by remember {
        mutableStateOf("选项A\n选项B\n选项C\n选项D\n选项E\n选项F")
    }
    var count by remember { mutableStateOf(2) }
    var allowRepeat by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var result by remember { mutableStateOf(listOf<String>()) }

    fun pick() {
        val options = optionsText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        error = ""
        result = emptyList()
        if (options.isEmpty()) {
            error = "请先输入选项，每行一个"
            return
        }
        if (!allowRepeat && count > options.size) {
            error = "不重复模式下，选取数量（$count）不能超过选项总数（${options.size}）"
            return
        }
        Sfx.tick()
        result = if (allowRepeat) {
            List(count) { options.random() }
        } else {
            options.shuffled().take(count)
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "选项（每行一个）") {
            LabeledField(
                value = optionsText,
                onChange = { optionsText = it },
                label = "输入选项",
                singleLine = false,
                minLines = 5
            )
        }

        SectionCard {
            Stepper("一次选出 N 个", count, { count = it }, 1..100)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("允许同一选项重复出现", modifier = Modifier.weight(1f))
                Switch(checked = allowRepeat, onCheckedChange = { allowRepeat = it })
            }
        }

        ErrorText(error)
        Button(onClick = { pick() }, modifier = Modifier.fillMaxWidth()) {
            Text("🎲 开始选择", style = MaterialTheme.typography.titleMedium)
        }

        if (result.isNotEmpty()) {
            SectionCard(title = "选中结果（${result.size} 个）") {
                ResultText(
                    result.mapIndexed { index, item -> "${index + 1}. $item" }.joinToString("\n")
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CopyButton(result.joinToString(", "), modifier = Modifier.weight(1f))
                    CopyButton(result.joinToString("\n"), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
