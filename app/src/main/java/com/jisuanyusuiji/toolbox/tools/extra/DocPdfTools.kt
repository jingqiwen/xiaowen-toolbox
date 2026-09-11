package com.jisuanyusuiji.toolbox.tools.extra

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Xml
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

/** 解析 .docx 文档中的文本段落（离线解析，不需要 Office）。 */
private fun parseDocxParagraphs(context: android.content.Context, uri: Uri): List<String> {
    val paragraphs = mutableListOf<String>()
    context.contentResolver.openInputStream(uri)?.use { input ->
        val zip = ZipInputStream(input)
        var entry = zip.nextEntry
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                val parser = Xml.newPullParser()
                parser.setInput(zip, "UTF-8")
                var event = parser.eventType
                var current = StringBuilder()
                var inText = false
                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> when (parser.name) {
                            "w:p" -> current = StringBuilder()
                            "w:t" -> {
                                inText = true
                                current.append(parser.nextText())
                            }
                            "w:tab" -> current.append("\t")
                            "w:br" -> current.append("\n")
                        }
                        XmlPullParser.END_TAG -> when (parser.name) {
                            "w:p" -> paragraphs.add(current.toString().trim())
                            "w:t" -> inText = false
                        }
                    }
                    event = parser.next()
                }
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
    }
    return paragraphs.filter { it.isNotEmpty() }
}

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

// ============================================================
// Word（.docx）转 PDF
// ============================================================
@Composable
fun WordToPdfTool() {
    val context = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf<SavedMedia?>(null) }
    var error by remember { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri = it }

    fun run() {
        val u = uri ?: run { error = "请先选择 .docx 文件"; return }
        error = ""
        message = ""
        try {
            val paragraphs = parseDocxParagraphs(context, u)
            if (paragraphs.isEmpty()) { error = "未能从文档中读取到文本"; return }
            val bytes = buildPdfFromParagraphs(paragraphs)
            val name = (u.lastPathSegment?.substringBeforeLast(".") ?: "document") + ".pdf"
            val savedMedia = saveMedia(context, bytes, "application/pdf", name)
            saved = savedMedia
            message = "已转换 ${paragraphs.size} 个段落"
        } catch (e: Exception) {
            error = "转换失败：${e.message ?: "文件格式不支持"}"
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
            Spacer(Modifier.height(12.dp))
            Button(onClick = { run() }, enabled = uri != null, modifier = Modifier.fillMaxWidth()) { Text("转换为 PDF") }
        }
        ErrorText(error)
        if (message.isNotBlank()) {
            SectionCard(title = "结果") {
                Text(message)
                saved?.let { MediaResultActions(it) }
            }
        }
        Text(
            "说明：当前版本为本地文本转换（段落文字），图片、表格与复杂排版暂不保留；仅支持 .docx，不支持旧版 .doc。",
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
    var saved by remember { mutableStateOf<SavedMedia?>(null) }
    var error by remember { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri = it }
    val bitmap = remember(uri) { uri?.let { decodeImage(context, it, 8000) } }

    fun run() {
        val bmp = bitmap ?: run { error = "请先选择长图"; return }
        error = ""
        message = ""
        val doc = PdfDocument()
        val pageW = 595
        var pageH = bmp.height * pageW / bmp.width
        if (pageH > 14000) pageH = 14000
        val page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, 1).create())
        val canvas = page.canvas
        val scale = pageW.toFloat() / bmp.width
        val drawW = bmp.width * scale
        val drawH = bmp.height * scale
        canvas.drawBitmap(bmp, null, android.graphics.RectF(0f, 0f, drawW, drawH), Paint().apply { isAntiAlias = true })
        doc.finishPage(page)
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        saved = saveMedia(context, out.toByteArray(), "application/pdf", "LONGIMG_${System.currentTimeMillis()}.pdf")
        message = "已生成长图 PDF"
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
            Button(onClick = { run() }, enabled = bitmap != null, modifier = Modifier.fillMaxWidth()) { Text("生成为 PDF") }
        }
        ErrorText(error)
        if (message.isNotBlank()) {
            SectionCard(title = "结果") {
                Text(message)
                saved?.let { MediaResultActions(it) }
            }
        }
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
    var error by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var saved by remember { mutableStateOf<SavedMedia?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri = it }

    fun run() {
        val u = uri ?: run { error = "请先选择 PDF"; return }
        error = ""
        message = ""
        try {
            val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(u, "r") ?: return
            pfd.use {
                val renderer = PdfRenderer(it)
                val pageCount = renderer.pageCount.coerceAtMost(40)
                val targetW = 1080
                val pages = mutableListOf<Bitmap>()
                var totalH = 0
                for (i in 0 until pageCount) {
                    val page = renderer.openPage(i)
                    val scale = targetW.toFloat() / page.width
                    val h = (page.height * scale).toInt()
                    val bmp = Bitmap.createBitmap(targetW, h, Bitmap.Config.ARGB_8888)
                    page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    pages += bmp
                    totalH += h
                    page.close()
                    if (totalH > 20000) break
                }
                renderer.close()
                if (pages.isEmpty()) { error = "PDF 无有效页面"; return }
                val out = Bitmap.createBitmap(targetW, totalH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(out)
                var y = 0
                pages.forEach { bmp ->
                    canvas.drawBitmap(bmp, 0f, y.toFloat(), null)
                    y += bmp.height
                }
                preview = out
                saved = saveBitmapMedia(context, out, "image/jpeg", Bitmap.CompressFormat.JPEG, 92, "PDFLONG_${System.currentTimeMillis()}.jpg")
                message = "已转换 ${pages.size} 页"
            }
        } catch (e: Exception) {
            error = "转换失败：${e.message ?: "文件格式不支持"}"
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
            Spacer(Modifier.height(12.dp))
            Button(onClick = { run() }, enabled = uri != null, modifier = Modifier.fillMaxWidth()) { Text("转换为长图") }
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
        Text("说明：最多渲染前 40 页，宽 1080px，按页竖向拼接。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}
