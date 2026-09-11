package com.jisuanyusuiji.toolbox.tools.extra

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

// ============================================================
// .docx 文本提取（本地、离线）
// ============================================================

private fun unescapeXml(s: String): String = s
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace("&apos;", "'")
    .replace("&#39;", "'")
    .replace("&amp;", "&")
    .replace(Regex("&#x([0-9a-fA-F]+);")) { m ->
        m.groupValues[1].toIntOrNull(16)?.let { String(Character.toChars(it)) } ?: m.value
    }
    .replace(Regex("&#(\\d+);")) { m ->
        m.groupValues[1].toIntOrNull()?.let { String(Character.toChars(it)) } ?: m.value
    }

/** 读取 docx 压缩包中的 XML 文本。 */
private fun readDocxXml(context: Context, uri: Uri, entryNames: List<String>): String? {
    context.contentResolver.openInputStream(uri)?.use { input ->
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            var fallback: String? = null
            while (entry != null) {
                val name = entry.name.replace('\\', '/')
                val matched = entryNames.any { name.equals(it, true) } ||
                    (!entry.isDirectory && name.endsWith("document.xml"))
                if (matched && !entry.isDirectory) {
                    val bytes = zip.readBytes()
                    val text = String(bytes, Charsets.UTF_8).removePrefix("\uFEFF")
                    if (entryNames.any { name.equals(it, true) }) return text
                    if (fallback == null) fallback = text
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
            if (fallback != null) return fallback
        }
    }
    return null
}

/** 从 document.xml 里按段落提取文字，兼容表格、制表符、换行与 XML 实体。 */
private fun extractParagraphsFromXml(xml: String): List<String> {
    val paragraphs = mutableListOf<String>()
    val paraRegex = Regex("<w:p(?:\\s[^>]*)?>.*?</w:p>", RegexOption.DOT_MATCHES_ALL)
    val chunks = paraRegex.findAll(xml).map { it.value }.toList().ifEmpty { listOf(xml) }
    val tokenRegex = Regex(
        "<w:(t|tab|br|cr)(?:\\s[^>]*)?(?:/>|>(.*?)</w:t>)",
        RegexOption.DOT_MATCHES_ALL
    )
    for (chunk in chunks) {
        val sb = StringBuilder()
        tokenRegex.findAll(chunk).forEach { m ->
            when (m.groupValues[1]) {
                "t" -> sb.append(unescapeXml(m.groupValues[2]))
                "tab" -> sb.append('\t')
                else -> sb.append('\n')
            }
        }
        val text = sb.toString().trimEnd()
        if (text.isNotBlank()) paragraphs.add(text)
    }
    if (paragraphs.isEmpty()) {
        // 最后兜底：提取所有 XML 文本节点
        Regex(">([^<>]+)<").findAll(xml).forEach { m ->
            val t = unescapeXml(m.groupValues[1]).trim()
            if (t.isNotEmpty()) paragraphs.add(t)
        }
    }
    return paragraphs
}

private fun parseDocxParagraphs(context: Context, uri: Uri): List<String> {
    val xml = readDocxXml(context, uri, listOf("word/document.xml")) ?: return emptyList()
    return extractParagraphsFromXml(xml)
}

// ============================================================
// 文本排版成 PDF
// ============================================================

private fun buildPdfFromParagraphs(paragraphs: List<String>): ByteArray {
    val doc = PdfDocument()
    val pageW = 595
    val pageH = 842
    val margin = 48
    val paint = TextPaint().apply {
        color = Color.BLACK
        textSize = 13f
        isAntiAlias = true
    }
    var page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, 1).create())
    var canvas: Canvas = page.canvas
    var y = margin.toFloat()

    fun newPage() {
        doc.finishPage(page)
        page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, 1).create())
        canvas = page.canvas
        y = margin.toFloat()
    }

    paragraphs.forEach { text ->
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, pageW - margin * 2)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(true)
            .setLineSpacing(2f, 1.15f)
            .build()
        if (y + layout.height > pageH - margin) newPage()
        canvas.save()
        canvas.translate(margin.toFloat(), y)
        layout.draw(canvas)
        canvas.restore()
        y += layout.height + 6
    }
    doc.finishPage(page)
    val out = ByteArrayOutputStream()
    doc.writeTo(out)
    doc.close()
    return out.toByteArray()
}

/** 把文字版 PDF 的每一页渲染成整页图片，再重新打包为“一页一整图”的 PDF。 */
private fun pdfToImagePdf(context: Context, pdfBytes: ByteArray): ByteArray {
    val tmp = File.createTempFile("xw_source_", ".pdf", context.cacheDir)
    try {
        tmp.writeBytes(pdfBytes)
        val outDoc = PdfDocument()
        ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                val scale = 2f
                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val bmp = Bitmap.createBitmap(
                        (page.width * scale).toInt().coerceAtLeast(1),
                        (page.height * scale).toInt().coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888
                    )
                    // 先铺白底，避免透明区域在后续编码中变成黑块
                    Canvas(bmp).drawColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val outPage = outDoc.startPage(
                        PdfDocument.PageInfo.Builder(page.width, page.height, i + 1).create()
                    )
                    outPage.canvas.drawBitmap(
                        bmp, null,
                        RectF(0f, 0f, page.width.toFloat(), page.height.toFloat()),
                        Paint().apply { isAntiAlias = true; isFilterBitmap = true }
                    )
                    outDoc.finishPage(outPage)
                    bmp.recycle()
                    page.close()
                }
            }
        }
        val out = ByteArrayOutputStream()
        outDoc.writeTo(out)
        outDoc.close()
        return out.toByteArray()
    } finally {
        tmp.delete()
    }
}

// ============================================================
// Word（.docx）转 PDF
// ============================================================
@Composable
fun WordToPdfTool() {
    val context = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf("") }
    var imagePages by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf<SavedMedia?>(null) }
    var error by remember { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri = it }

    fun run() {
        val u = uri ?: run { error = "请先选择 .docx 文件"; return }
        error = ""
        message = ""
        busy = true
        try {
            val paragraphs = parseDocxParagraphs(context, u)
            if (paragraphs.isEmpty()) {
                error = "未能从文档中读取到文本：请确认是 .docx 格式（旧版 .doc 请先用 Word 另存为 .docx），或文档里确实没有文字"
                return
            }
            val textPdf = buildPdfFromParagraphs(paragraphs)
            val bytes = if (imagePages) pdfToImagePdf(context, textPdf) else textPdf
            val name = (u.lastPathSegment?.substringBeforeLast(".") ?: "document") + ".pdf"
            val savedMedia = saveMedia(context, bytes, "application/pdf", name)
            saved = savedMedia
            message = "已转换 ${paragraphs.size} 个段落" + if (imagePages) "（每页整图输出）" else ""
        } catch (e: Exception) {
            error = "转换失败：${e.message ?: "文件格式不支持"}"
        } finally {
            busy = false
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "Word 转 PDF") {
            Button(onClick = { launcher.launch("application/vnd.openxmlformats-officedocument.wordprocessingml.document") }, modifier = Modifier.fillMaxWidth()) {
                Text(if (uri == null) "📂 选择 .docx 文件" else "✅ 已选择：${uri?.lastPathSegment ?: ""}")
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("整页图片输出", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "每页渲染成一张图片再打包 PDF，版式更稳定、不易出现乱码",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = imagePages, onCheckedChange = { imagePages = it })
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { run() }, enabled = uri != null && !busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (busy) "转换中…" else "转换为 PDF")
            }
        }
        ErrorText(error)
        if (message.isNotBlank()) {
            SectionCard(title = "结果") {
                Text(message)
                saved?.let { MediaResultActions(it) }
            }
        }
        Text(
            "说明：本地解析 .docx 的段落与表格文字并按 A4 重新排版；复杂公式、图片、分栏等版式不会原样保留。" +
                "打开“整页图片输出”后，最终 PDF 的每一页都是一整张图。仅支持 .docx，不支持旧版 .doc。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

// ============================================================
// 长图转 PDF
// ============================================================
@Composable
fun LongImageToPdfTool() {
    val context = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf<SavedMedia?>(null) }
    var error by remember { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri = it }
    val bitmap = remember(uri) { uri?.let { decodeImage(context, it, 12000) } }

    fun run() {
        val bmp = bitmap ?: run { error = "请先选择长图"; return }
        error = ""
        message = ""
        busy = true
        try {
            if (bmp.width <= 0 || bmp.height <= 0) {
                error = "图片尺寸无效"
                return
            }
            val doc = PdfDocument()
            val pageW = 595
            val scale = pageW.toFloat() / bmp.width
            // 单页最高 14000pt，超过则自动切成多页，避免超大页面导致的崩溃
            val maxSrcSliceH = (14000f / scale).toInt().coerceAtLeast(1)
            var srcY = 0
            var pageIndex = 1
            while (srcY < bmp.height) {
                val sliceH = minOf(maxSrcSliceH, bmp.height - srcY)
                val pageH = (sliceH * scale).toInt().coerceIn(1, 14000)
                val page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageIndex).create())
                val canvas = page.canvas
                val src = android.graphics.Rect(0, srcY, bmp.width, srcY + sliceH)
                val dst = RectF(0f, 0f, pageW.toFloat(), pageH.toFloat())
                canvas.drawBitmap(bmp, src, dst, Paint().apply { isAntiAlias = true; isFilterBitmap = true })
                doc.finishPage(page)
                srcY += sliceH
                pageIndex++
            }
            val out = ByteArrayOutputStream()
            doc.writeTo(out)
            doc.close()
            saved = saveMedia(context, out.toByteArray(), "application/pdf", "LONGIMG_${System.currentTimeMillis()}.pdf")
            message = "已生成 PDF，共 ${pageIndex - 1} 页"
        } catch (e: Exception) {
            error = "生成失败：${e.message ?: "图片过大或格式不支持"}"
        } finally {
            busy = false
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "长图转 PDF") {
            Button(onClick = { launcher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                Text(if (uri == null) "📂 选择长图" else "✅ 已选择")
            }
            bitmap?.let {
                Spacer(Modifier.height(8.dp))
                Image(it.asImageBitmap(), "长图", Modifier.fillMaxWidth().height(220.dp))
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { run() }, enabled = bitmap != null && !busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (busy) "生成中…" else "生成为 PDF")
            }
        }
        ErrorText(error)
        if (message.isNotBlank()) {
            SectionCard(title = "结果") {
                Text(message)
                saved?.let { MediaResultActions(it) }
            }
        }
        Text(
            "说明：长图会按宽度缩放到 A4 宽，超长时自动分成多页，避免单页过大导致闪退。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

// ============================================================
// PDF 转长图
// ============================================================
@Composable
fun PdfToLongImageTool() {
    val context = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var maxPages by remember { mutableStateOf(40) }
    var error by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var saved by remember { mutableStateOf<SavedMedia?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri = it }

    fun run() {
        val u = uri ?: run { error = "请先选择 PDF"; return }
        error = ""
        message = ""
        busy = true
        try {
            val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(u, "r")
                ?: run { error = "无法打开 PDF 文件"; return }
            pfd.use {
                val renderer = PdfRenderer(it)
                val pageCount = renderer.pageCount.coerceAtMost(maxPages)
                val targetW = 1440
                val pages = mutableListOf<Bitmap>()
                var totalH = 0
                for (i in 0 until pageCount) {
                    val page = renderer.openPage(i)
                    val scale = targetW.toFloat() / page.width
                    val h = (page.height * scale).toInt().coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(targetW, h, Bitmap.Config.ARGB_8888)
                    // 关键：先铺白底，避免透明区域在 JPEG 中变成黑块/黑方框
                    Canvas(bmp).drawColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    pages += bmp
                    totalH += h
                    page.close()
                    if (totalH > 24000) break
                }
                renderer.close()
                if (pages.isEmpty()) { error = "PDF 无有效页面"; return }
                val out = Bitmap.createBitmap(targetW, totalH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(out)
                canvas.drawColor(Color.WHITE)
                var y = 0
                pages.forEach { bmp ->
                    canvas.drawBitmap(bmp, 0f, y.toFloat(), null)
                    y += bmp.height
                    bmp.recycle()
                }
                preview = out
                saved = saveBitmapMedia(context, out, "image/png", Bitmap.CompressFormat.PNG, 100, "PDFLONG_${System.currentTimeMillis()}.png")
                message = "已转换 ${pages.size} 页（宽 ${targetW}px，PNG 无损输出）"
            }
        } catch (e: Exception) {
            error = "转换失败：${e.message ?: "文件格式不支持"}"
        } finally {
            busy = false
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "PDF 转长图") {
            Button(onClick = { launcher.launch("application/pdf") }, modifier = Modifier.fillMaxWidth()) {
                Text(if (uri == null) "📂 选择 PDF 文件" else "✅ 已选择：${uri?.lastPathSegment ?: ""}")
            }
            Spacer(Modifier.height(8.dp))
            Text("最多转换页数：$maxPages", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10, 20, 40, 80).forEach { n ->
                    Button(onClick = { maxPages = n }, modifier = Modifier.weight(1f)) { Text("$n") }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { run() }, enabled = uri != null && !busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (busy) "转换中…" else "转换为长图")
            }
        }
        ErrorText(error)
        if (message.isNotBlank()) {
            SectionCard(title = "结果") {
                Text(message)
                saved?.let { MediaResultActions(it) }
            }
        }
        preview?.let {
            SectionCard(title = "预览") {
                Image(it.asImageBitmap(), "PDF 长图", Modifier.fillMaxWidth().height(260.dp))
            }
        }
        Text(
            "说明：按 1440px 宽渲染并铺白底后用 PNG 无损拼接，尽量还原文字与原色；页数较多时请分批转换以免图片过大。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
