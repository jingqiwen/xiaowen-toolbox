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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.ui.Sfx
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.CopyButton
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.ResultText
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import com.jisuanyusuiji.toolbox.ui.components.Stepper
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random

@Composable
fun RandomNumberTool() {
    var minText by remember { mutableStateOf("1") }
    var maxText by remember { mutableStateOf("100") }
    var decimal by remember { mutableStateOf(false) }
    var allowRepeat by remember { mutableStateOf(true) }
    var count by remember { mutableStateOf(5) }
    var error by remember { mutableStateOf("") }
    var result by remember { mutableStateOf(listOf<String>()) }

    fun generate() {
        error = ""
        result = emptyList()
        Sfx.tick()

        if (decimal) {
            val lo = minText.toDoubleOrNull()
            val hi = maxText.toDoubleOrNull()
            if (lo == null || hi == null) {
                error = "请输入有效的最小值和最大值"
                return
            }
            if (lo > hi) {
                error = "最小值不能大于最大值"
                return
            }
            val values = List(count) {
                val v = lo + Random.nextDouble() * (hi - lo)
                BigDecimal(v).setScale(4, RoundingMode.HALF_UP)
                    .stripTrailingZeros().toPlainString()
            }
            result = values
        } else {
            val lo = minText.toLongOrNull()
            val hi = maxText.toLongOrNull()
            if (lo == null || hi == null) {
                error = "整数模式下请输入整数（例如 -100、0、1000）"
                return
            }
            if (lo > hi) {
                error = "最小值不能大于最大值"
                return
            }
            val rangeSize = hi - lo + 1
            if (rangeSize <= 0) {
                error = "整数范围过大，请缩小范围"
                return
            }
            if (!allowRepeat && count > rangeSize) {
                error = "不重复模式下，生成数量（$count）不能超过范围内整数个数（$rangeSize）"
                return
            }
            val values = if (allowRepeat) {
                List(count) { Random.nextLong(lo, hi + 1) }
            } else {
                val set = mutableSetOf<Long>()
                while (set.size < count) {
                    set.add(Random.nextLong(lo, hi + 1))
                }
                set.toList()
            }
            result = values.map { it.toString() }
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "参数设置") {
            ChoiceChips(
                options = listOf(false, true),
                selected = decimal,
                onSelect = { decimal = it },
                label = { if (it) "小数" else "整数" }
            )
            Spacer(Modifier.height(10.dp))
            LabeledField(
                value = minText,
                onChange = { minText = it },
                label = "最小值",
                keyboardType = KeyboardType.Number
            )
            Spacer(Modifier.height(8.dp))
            LabeledField(
                value = maxText,
                onChange = { maxText = it },
                label = "最大值",
                keyboardType = KeyboardType.Number
            )
            Spacer(Modifier.height(10.dp))
            Stepper("生成数量", count, { count = it }, 1..100)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("是否允许重复", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "小数模式下重复值概率极低，无需关闭",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = allowRepeat, onCheckedChange = { allowRepeat = it })
            }
        }

        ErrorText(error)
        Button(onClick = { generate() }, modifier = Modifier.fillMaxWidth()) {
            Text("🎲 生成随机数", style = MaterialTheme.typography.titleMedium)
        }

        if (result.isNotEmpty()) {
            SectionCard(title = "结果（${result.size} 个）") {
                ResultText(result.joinToString(", "))
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CopyButton(result.joinToString(", "), modifier = Modifier.weight(1f))
                    CopyButton(result.joinToString("\n"), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
