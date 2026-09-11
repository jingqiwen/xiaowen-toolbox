package com.jisuanyusuiji.toolbox.tools.extra

import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// ============================================================
// 压缩文件（多选文件 → zip）
// ============================================================
@Composable
fun ZipCompressTool() {
    val context = LocalContext.current
    var uris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf<SavedMedia?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris = it.take(100) }

    fun run() {
        error = ""; message = ""; busy = true; saved = null
        try {
            if (uris.isEmpty()) throw IllegalArgumentException("请先选择要压缩的文件")
            val out = ByteArrayOutputStream()
            val zip = ZipOutputStream(out)
            var count = 0
            var used = mutableSetOf<String>()
            uris.forEachIndexed { index, uri ->
                val name = uri.lastPathSegment?.substringAfterLast('/') ?: "file_$index"
                var entryName = name
                var suffix = 1
                while (used.contains(entryName)) {
                    entryName = "${name.substringBeforeLast('.', name)}_${suffix++}.${name.substringAfterLast('.', "bin")}"
                }
                used.add(entryName)
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@forEachIndexed
                zip.putNextEntry(ZipEntry(entryName))
                zip.write(bytes)
                zip.closeEntry()
                count++
            }
            zip.close()
            saved = saveMedia(context, out.toByteArray(), "application/zip", "ARCHIVE_${System.currentTimeMillis()}.zip")
            message = "已压缩 $count 个文件"
        } catch (e: Exception) {
            error = "压缩失败：${e.message}"
        }
        busy = false
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "压缩文件（ZIP）") {
            Button(onClick = { launcher.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
                Text(if (uris.isEmpty()) "📂 选择要压缩的文件（可多选）" else "✅ 已选择 ${uris.size} 个文件")
            }
            Spacer(Modifier.height(10.dp))
            Button(onClick = { run() }, enabled = uris.isNotEmpty() && !busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (busy) "压缩中…" else "压缩为 ZIP")
            }
        }
        ErrorText(error)
        if (message.isNotBlank()) {
            SectionCard(title = "结果") {
                Text(message)
                saved?.let { MediaResultActions(it) }
            }
        }
        Text("说明：仅本地压缩，不上传；暂不支持加密 ZIP。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}

// ============================================================
// 解压文件（ZIP → 应用文件夹）
// ============================================================
@Composable
fun ZipExtractTool() {
    val context = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var extracted by remember { mutableStateOf(listOf<String>()) }
    var saved by remember { mutableStateOf<SavedMedia?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri = it }

    fun run() {
        error = ""; message = ""; busy = true; extracted = emptyList(); saved = null
        try {
            val u = uri ?: throw IllegalArgumentException("请先选择 ZIP 文件")
            val baseDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "unzip_${System.currentTimeMillis()}").apply { mkdirs() }
            val list = mutableListOf<String>()
            context.contentResolver.openInputStream(u)?.use { input ->
                val zip = ZipInputStream(input)
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name.replace('\\', '/')
                    if (name.contains("..")) { zip.closeEntry(); entry = zip.nextEntry; continue }
                    val target = File(baseDir, name)
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { out -> zip.copyTo(out) }
                        list.add("${entry.name}（${target.length() / 1024} KB）")
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            extracted = list.take(200)
            message = "已解压 ${list.size} 个文件到：\n${baseDir.absolutePath}"
            saved = SavedMedia(
                message = "解压目录：${baseDir.absolutePath}",
                uri = null,
                file = File(baseDir, extractedFirstFileName(extracted) ?: ""),
                mimeType = "*/*"
            )
        } catch (e: Exception) {
            error = "解压失败：${e.message}"
        }
        busy = false
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "解压文件（ZIP）") {
            Button(onClick = { launcher.launch("application/zip") }, modifier = Modifier.fillMaxWidth()) {
                Text(if (uri == null) "📂 选择 ZIP 文件" else "✅ 已选择：${uri?.lastPathSegment ?: ""}")
            }
            Spacer(Modifier.height(10.dp))
            Button(onClick = { run() }, enabled = uri != null && !busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (busy) "解压中…" else "解压到应用文件夹")
            }
        }
        ErrorText(error)
        if (message.isNotBlank()) {
            SectionCard(title = "结果") {
                Text(message, style = MaterialTheme.typography.bodySmall)
                extracted.take(30).forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                saved?.let { MediaResultActions(it) }
            }
        }
        Text("说明：解压位置为 App 专属外部文件夹（无需存储权限）；暂不支持加密 ZIP。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}

private fun extractedFirstFileName(list: List<String>): String? =
    list.firstOrNull()?.substringBefore("（")?.substringAfterLast('/')
