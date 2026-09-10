package com.jisuanyusuiji.toolbox.tools.calc

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.ui.components.CopyButton
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.InfoRow
import com.jisuanyusuiji.toolbox.ui.components.LabeledField
import com.jisuanyusuiji.toolbox.ui.components.ResultText
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import java.security.MessageDigest

// ============================================================
// 27. 哈希计算器
// ============================================================
@Composable
fun HashTool() {
    var input by remember { mutableStateOf("hello world") }
    var result by remember { mutableStateOf(listOf<Pair<String, String>>()) }

    fun hash(algorithm: String): String {
        val bytes = MessageDigest.getInstance(algorithm).digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun calc() {
        if (input.isEmpty()) return
        result = listOf(
            "MD5" to hash("MD5"),
            "SHA1" to hash("SHA-1"),
            "SHA256" to hash("SHA-256")
        )
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "输入文本（本地计算，不上传）") {
            LabeledField(input, { input = it }, "文本", singleLine = false, minLines = 4)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { calc() }, modifier = Modifier.fillMaxWidth()) { Text("计算哈希") }
        }
        if (result.isNotEmpty()) {
            SectionCard(title = "结果") {
                result.forEach { (name, value) ->
                    InfoRow("$name：", value.uppercase())
                }
                Spacer(Modifier.height(10.dp))
                CopyButton(result.joinToString("\n") { "${it.first}: ${it.second.uppercase()}" })
            }
        }
    }
}

// ============================================================
// 28. ASCII 码互查工具
// ============================================================
@Composable
fun AsciiTool() {
    var charInput by remember { mutableStateOf("A") }
    var numberInput by remember { mutableStateOf("65") }
    var result by remember { mutableStateOf(listOf<String>()) }
    var error by remember { mutableStateOf("") }

    fun charToCode() {
        error = ""
        if (charInput.isEmpty()) { error = "请输入字符"; return }
        val c = charInput[0]
        val code = c.code
        result = listOf(
            "字符“$c”",
            "十进制：$code",
            "十六进制：0x${code.toString(16).uppercase()}",
            "八进制：0${code.toString(8)}",
            "二进制：${code.toString(2)}"
        )
    }

    fun codeToChar() {
        error = ""
        val n = numberInput.trim().toIntOrNull() ?: run { error = "请输入有效数字"; return }
        if (n !in 0..0x10FFFF) { error = "数字超出 Unicode 范围"; return }
        val c = n.toChar()
        result = listOf(
            "十进制 $n 对应字符：$c",
            "十六进制 0x${n.toString(16).uppercase()} 对应字符：$c"
        )
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "字符 → ASCII 码") {
            LabeledField(charInput, { charInput = it }, "输入一个字符")
            Spacer(Modifier.height(10.dp))
            Button(onClick = { charToCode() }, modifier = Modifier.fillMaxWidth()) { Text("查询编码") }
        }
        SectionCard(title = "ASCII 码 → 字符") {
            LabeledField(numberInput, { numberInput = it }, "输入十进制数字", keyboardType = KeyboardType.Number)
            Spacer(Modifier.height(10.dp))
            Button(onClick = { codeToChar() }, modifier = Modifier.fillMaxWidth()) { Text("查询字符") }
        }
        ErrorText(error)
        if (result.isNotEmpty()) {
            SectionCard(title = "结果") {
                result.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
            }
        }
        SectionCard(title = "常用 ASCII 表（32~126）") {
            val table = (32..126).chunked(10).joinToString("\n") { row ->
                row.joinToString("  ") { code ->
                    val ch = code.toChar().toString().replace(" ", "␠")
                    "${code.toString().padStart(3)}:${ch}"
                }
            }
            ResultText(table)
        }
    }
}
