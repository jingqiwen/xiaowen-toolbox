package com.jisuanyusuiji.toolbox.tools.extra

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** 转换完成后统一显示：保存位置 + 打开 / 分享 / 打开所在位置。 */
@Composable
fun MediaResultActions(saved: SavedMedia, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier.fillMaxWidth()) {
        Text(saved.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { openMedia(context, saved) }, modifier = Modifier.weight(1f)) { Text("打开") }
            OutlinedButton(onClick = { shareMedia(context, saved) }, modifier = Modifier.weight(1f)) { Text("分享") }
            OutlinedButton(onClick = { openContainingFolder(context, saved) }, modifier = Modifier.weight(1f)) { Text("所在位置") }
        }
    }
}
