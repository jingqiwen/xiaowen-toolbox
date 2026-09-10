package com.jisuanyusuiji.toolbox.tools.calc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

private fun ipToLong(ip: String): Long? {
    val parts = ip.trim().split(".")
    if (parts.size != 4) return null
    var v = 0L
    for (p in parts) {
        val n = p.toIntOrNull() ?: return null
        if (n !in 0..255) return null
        v = (v shl 8) or n.toLong()
    }
    return v
}

private fun longToIp(v: Long): String =
    "${(v shr 24) and 0xFF}.${(v shr 16) and 0xFF}.${(v shr 8) and 0xFF}.${v and 0xFF}"

@Composable
fun IpSubnetTool() {
    var ip by remember { mutableStateOf("192.168.1.100") }
    var prefix by remember { mutableStateOf("24") }
    var result by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var error by remember { mutableStateOf("") }

    fun calc() {
        error = ""
        result = emptyList()
        val ipLong = ipToLong(ip) ?: run { error = "IP 地址格式错误（如 192.168.1.100）"; return }
        val p = prefix.trim().toIntOrNull() ?: run { error = "前缀长度必须是 0~32 的整数"; return }
        if (p !in 0..32) { error = "前缀长度必须在 0~32 之间"; return }
        val mask = if (p == 0) 0L else (0xFFFFFFFFL shl (32 - p)) and 0xFFFFFFFFL
        val invMask = mask.inv() and 0xFFFFFFFFL
        val network = ipLong and mask
        val broadcast = network or invMask
        val hostCount = if (p >= 31) (if (p == 32) 1 else 2) else (1L shl (32 - p)) - 2
        val first = if (p == 32) network else if (p == 31) network else network + 1
        val last = if (p == 32) network else if (p == 31) broadcast else broadcast - 1
        result = listOf(
            "子网掩码" to longToIp(mask),
            "网络地址" to longToIp(network),
            "广播地址" to longToIp(broadcast),
            "可用主机范围" to "${longToIp(first)} ~ ${longToIp(last)}",
            "可用主机数量" to hostCount.toString(),
            "总地址数" to (1L shl (32 - p)).toString()
        )
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "输入（全部本地计算）") {
            LabeledField(ip, { ip = it }, "IP 地址", keyboardType = KeyboardType.Number)
            Spacer(Modifier.height(8.dp))
            LabeledField(prefix, { prefix = it }, "前缀长度 /24 或直接填 24", keyboardType = KeyboardType.Number)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { calc() }, modifier = Modifier.fillMaxWidth()) { Text("计算子网") }
        }
        ErrorText(error)
        if (result.isNotEmpty()) {
            SectionCard(title = "结果") {
                result.forEach { (label, value) ->
                    InfoRow("$label：", value)
                }
                Spacer(Modifier.height(8.dp))
                val text = result.joinToString("\n") { "${it.first}：${it.second}" }
                CopyButton(text)
            }
        }
        SectionCard(title = "说明") {
            ResultText("示例：192.168.1.100/24 表示前 24 位为网络位，后 8 位为主机位。\n本工具不联网，仅做数学计算。")
        }
    }
}
