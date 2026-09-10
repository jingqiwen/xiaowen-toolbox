package com.jisuanyusuiji.toolbox.tools.text

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
import com.jisuanyusuiji.toolbox.ui.components.CopyButton
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import com.jisuanyusuiji.toolbox.ui.components.Stepper
import java.security.SecureRandom

@Composable
fun PasswordTool() {
    var length by remember { mutableStateOf(16) }
    var upper by remember { mutableStateOf(true) }
    var lower by remember { mutableStateOf(true) }
    var digits by remember { mutableStateOf(true) }
    var symbols by remember { mutableStateOf(true) }
    var excludeAmbiguous by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    fun strength(pwd: String): String {
        val sets = listOf(
            Regex("[a-z]").containsMatchIn(pwd),
            Regex("[A-Z]").containsMatchIn(pwd),
            Regex("[0-9]").containsMatchIn(pwd),
            Regex("[^A-Za-z0-9]").containsMatchIn(pwd)
        ).count { it }
        val score = pwd.length + sets * 8
        return when {
            pwd.length >= 16 && sets >= 4 -> "极强"
            pwd.length >= 12 && sets >= 3 -> "强"
            pwd.length >= 8 && sets >= 2 -> "中等"
            else -> "弱"
        }
    }

    fun generate() {
        error = ""
        result = ""
        if (!upper && !lower && !digits && !symbols) {
            error = "请至少选择一种字符类型"
            return
        }
        val pools = mutableListOf<CharArray>()
        if (upper) pools += "ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray().let {
            if (excludeAmbiguous) it else "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()
        }
        if (lower) pools += if (excludeAmbiguous) "abcdefghijkmnpqrstuvwxyz".toCharArray() else "abcdefghijklmnopqrstuvwxyz".toCharArray()
        if (digits) pools += if (excludeAmbiguous) "23456789".toCharArray() else "0123456789".toCharArray()
        if (symbols) pools += "!@#$%^&*()-_=+[]{};:,.<>?".toCharArray()

        val random = SecureRandom()
        val all = pools.flatMap { it.toList() }.toCharArray()
        val sb = StringBuilder()
        // 保证每种已选类型至少出现一次
        pools.forEach { sb.append(it[random.nextInt(it.size)]) }
        while (sb.length < length) {
            sb.append(all[random.nextInt(all.size)])
        }
        result = sb.toString().toList().shuffled(random).joinToString("")
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "密码选项") {
            Stepper("密码长度", length, { length = it }, 4..128)
            Spacer(Modifier.height(8.dp))
            SwitchRow("大写字母 A-Z", upper) { upper = it }
            SwitchRow("小写字母 a-z", lower) { lower = it }
            SwitchRow("数字 0-9", digits) { digits = it }
            SwitchRow("特殊符号 !@#…", symbols) { symbols = it }
            SwitchRow("排除易混淆字符（0O1lI 等）", excludeAmbiguous) { excludeAmbiguous = it }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { generate() }, modifier = Modifier.fillMaxWidth()) { Text("生成密码") }
        }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        if (result.isNotBlank()) {
            SectionCard(title = "生成的密码（本地随机生成）") {
                Text(result, style = MaterialTheme.typography.headlineSmall)
                Text("强度评估：${strength(result)}", color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(10.dp))
                CopyButton(result)
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
