package com.jisuanyusuiji.toolbox.tools.extra

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Xml
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
import androidx.compose.material3.OutlinedButton
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
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.max

// ============================================================
// PPT（.pptx）转图片：解析每一页文字并渲染为 PNG
// ============================================================
@Composable
fun PptToImagesTool() {
    val context = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var saved by remember { mutableStateOf<SavedMedia?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri = it }

    fun convert() {
        val u = uri ?: run { error = "请先选择 .pptx 文件"; return }
        error = ""; message = ""; busy = true; saved = null
        try {
            val slides = parsePptxSlides(context, u)
            if (slides.isEmpty()) { error = "未读取到幻灯片内容"; busy = false; return }
            var lastSaved: SavedMedia? = null
            slides.forEachIndexed { index, lines ->
                val bmp = renderSlide(lines, 1280, 720)
                if (index == 0) preview = bmp
                lastSaved = saveBitmapMedia(context, bmp, "image/png", Bitmap.CompressFormat.PNG, 100, "PPT_${index + 1}.png")
            }
            saved = lastSaved
            message = "已转换 ${slides.size} 页，每页保存为一张图片（最后一张的保存位置见下方）"
        } catch (e: Exception) {
            error = "转换失败：${e.message}"
        }
        busy = false
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "PPT 转图片") {
            Button(onClick = { launcher.launch("application/vnd.openxmlformats-officedocument.presentationml.presentation") }, modifier = Modifier.fillMaxWidth()) {
                Text(if (uri == null) "📂 选择 .pptx 文件" else "✅ 已选择：${uri?.lastPathSegment ?: ""}")
            }
            Spacer(Modifier.height(10.dp))
            Button(onClick = { convert() }, enabled = uri != null && !busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (busy) "转换中…" else "转换为图片")
            }
            preview?.let {
                Spacer(Modifier.height(10.dp))
                Image(it.asImageBitmap(), "第一页预览", Modifier.fillMaxWidth().height(220.dp))
            }
        }
        ErrorText(error)
        if (message.isNotBlank()) {
            SectionCard(title = "结果") {
                Text(message)
                saved?.let { MediaResultActions(it) }
            }
        }
        Text("说明：当前为本地文字版式转换（保留每页文字，图片/图表/动画不保留）。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}

private fun parsePptxSlides(context: android.content.Context, uri: Uri): List<List<String>> {
    val slides = sortedMapOf<Int, List<String>>()
    context.contentResolver.openInputStream(uri)?.use { input ->
        val zip = ZipInputStream(input)
        var entry = zip.nextEntry
        while (entry != null) {
            val name = entry.name
            val match = Regex("ppt/slides/slide(\\d+)\\.xml").find(name)
            if (match != null) {
                val parser = Xml.newPullParser()
                parser.setInput(zip, "UTF-8")
                var event = parser.eventType
                val lines = mutableListOf<String>()
                val current = StringBuilder()
                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> if (parser.name == "t") current.append(parser.nextText())
                        XmlPullParser.END_TAG -> if (parser.name == "p") {
                            if (current.isNotBlank()) lines.add(current.toString().trim())
                            current.clear()
                        }
                    }
                    event = parser.next()
                }
                slides[match.groupValues[1].toInt()] = lines
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
    }
    return slides.values.toList()
}

private fun renderSlide(lines: List<String>, width: Int, height: Int): Bitmap {
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(android.graphics.Color.WHITE)
    var y = 60f
    lines.forEachIndexed { index, line ->
        val paint = TextPaint().apply {
            isAntiAlias = true
            color = android.graphics.Color.rgb(30, 30, 30)
            textSize = if (index == 0) 54f else 38f
            isFakeBoldText = index == 0
        }
        val layout = StaticLayout.Builder.obtain(line, 0, line.length, paint, width - 120)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(true)
            .build()
        if (y + layout.height > height - 50) {
            canvas.drawText("…", 60f, y + 40f, Paint().apply { textSize = 40f; color = android.graphics.Color.GRAY })
            return@forEachIndexed
        }
        canvas.save()
        canvas.translate(60f, y)
        layout.draw(canvas)
        canvas.restore()
        y += layout.height + 26f
    }
    return bmp
}

// ============================================================
// 图片转 PPT：每张图片一页，生成 .pptx
// ============================================================
@Composable
fun ImagesToPptTool() {
    val context = LocalContext.current
    var uris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf<SavedMedia?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris = it.take(30) }

    fun convert() {
        error = ""; message = ""; busy = true; saved = null
        try {
            if (uris.size < 1) throw IllegalArgumentException("请至少选择 1 张图片")
            val images = uris.mapNotNull { decodeImage(context, it, 1920) }
            if (images.isEmpty()) throw IllegalArgumentException("图片解码失败")
            val bytes = buildPptx(images)
            saved = saveMedia(context, bytes, "application/vnd.openxmlformats-officedocument.presentationml.presentation", "IMAGES_${System.currentTimeMillis()}.pptx")
            message = "已生成 ${images.size} 页 PPT，每页一张图片"
        } catch (e: Exception) {
            error = "生成失败：${e.message}"
        }
        busy = false
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "图片转 PPT") {
            Button(onClick = { launcher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                Text(if (uris.isEmpty()) "📂 选择多张图片（按选择顺序，最多 30 张）" else "✅ 已选择 ${uris.size} 张")
            }
            Spacer(Modifier.height(10.dp))
            Button(onClick = { convert() }, enabled = uris.isNotEmpty() && !busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (busy) "生成中…" else "生成 PPT")
            }
        }
        ErrorText(error)
        if (message.isNotBlank()) {
            SectionCard(title = "结果") {
                Text(message)
                saved?.let { MediaResultActions(it) }
            }
        }
        Text("说明：图片会按选择顺序放到每一页，保持 16:9 页面比例，图片比例不合适时可能拉伸。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}

private fun buildPptx(images: List<Bitmap>): ByteArray {
    val out = ByteArrayOutputStream()
    val zip = ZipOutputStream(out)
    fun put(name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }
    fun putBytes(name: String, bytes: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(bytes)
        zip.closeEntry()
    }
    val slideCount = images.size
    val overrides = (1..slideCount).joinToString("\n") {
        "<Override PartName=\"/ppt/slides/slide$it.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slide+xml\"/>"
    }
    put("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Default Extension="png" ContentType="image/png"/>
<Default Extension="jpeg" ContentType="image/jpeg"/>
<Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
<Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/>
<Override PartName="/ppt/slideLayouts/slideLayout1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"/>
<Override PartName="/ppt/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>
$overrides
</Types>""")
    put("_rels/.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
</Relationships>""")
    val slideIds = (1..slideCount).joinToString("") { "<p:sldId id=\"${255 + it}\" r:id=\"rId${it + 1}\"/>" }
    put("ppt/presentation.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
<p:sldMasterIdLst><p:sldMasterId id="2147483648" r:id="rId1"/></p:sldMasterIdLst>
<p:sldIdLst>$slideIds</p:sldIdLst>
<p:sldSz cx="12192000" cy="6858000"/><p:notesSz cx="6858000" cy="9144000"/>
</p:presentation>""")
    val presRels = StringBuilder()
    presRels.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="slideMasters/slideMaster1.xml"/>""")
    (1..slideCount).forEach { presRels.append("<Relationship Id=\"rId${it + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide\" Target=\"slides/slide$it.xml\"/>") }
    presRels.append("</Relationships>")
    put("ppt/_rels/presentation.xml.rels", presRels.toString())
    put("ppt/slideMasters/slideMaster1.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldMaster xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
<p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr></p:spTree></p:cSld>
<p:clrMap bg1="lt1" tx1="dk1" bg2="lt2" tx2="dk2" accent1="accent1" accent2="accent2" accent3="accent3" accent4="accent4" accent5="accent5" accent6="accent6" hlink="hlink" folHlink="folHlink"/>
<p:sldLayoutIdLst><p:sldLayoutId id="2147483649" r:id="rId1"/></p:sldLayoutIdLst>
</p:sldMaster>""")
    put("ppt/slideMasters/_rels/slideMaster1.xml.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="../theme/theme1.xml"/>
</Relationships>""")
    put("ppt/slideLayouts/slideLayout1.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldLayout xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" type="blank" preserve="1">
<p:cSld name="Blank"><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr></p:spTree></p:cSld>
<p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sldLayout>""")
    put("ppt/slideLayouts/_rels/slideLayout1.xml.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="../slideMasters/slideMaster1.xml"/>
</Relationships>""")
    put("ppt/theme/theme1.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="Office">
<a:themeElements>
<a:clrScheme name="Office"><a:dk1><a:sysClr val="windowText" lastClr="000000"/></a:dk1><a:lt1><a:sysClr val="window" lastClr="FFFFFF"/></a:lt1><a:dk2><a:srgbClr val="44546A"/></a:dk2><a:lt2><a:srgbClr val="E7E6E6"/></a:lt2><a:accent1><a:srgbClr val="4472C4"/></a:accent1><a:accent2><a:srgbClr val="ED7D31"/></a:accent2><a:accent3><a:srgbClr val="A5A5A5"/></a:accent3><a:accent4><a:srgbClr val="FFC000"/></a:accent4><a:accent5><a:srgbClr val="5B9BD5"/></a:accent5><a:accent6><a:srgbClr val="70AD47"/></a:accent6><a:hlink><a:srgbClr val="0563C1"/></a:hlink><a:folHlink><a:srgbClr val="954F72"/></a:folHlink></a:clrScheme>
<a:fontScheme name="Office"><a:majorFont><a:latin typeface="Calibri Light"/><a:ea typeface=""/><a:cs typeface=""/></a:majorFont><a:minorFont><a:latin typeface="Calibri"/><a:ea typeface=""/><a:cs typeface=""/></a:minorFont></a:fontScheme>
<a:fmtScheme name="Office"><a:fillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:fillStyleLst><a:lnStyleLst><a:ln w="6350" cap="flat" cmpd="sng" algn="ctr"><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:ln></a:lnStyleLst><a:effectStyleLst><a:effectStyle><a:effectLst/></a:effectStyle></a:effectStyleLst><a:bgFillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:bgFillStyleLst></a:fmtScheme>
</a:themeElements></a:theme>""")

    images.forEachIndexed { index, bmp ->
        val n = index + 1
        val mediaName = "image$n.jpeg"
        val bytes = ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }.toByteArray()
        putBytes("ppt/media/$mediaName", bytes)
        put("ppt/slides/slide$n.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
<p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr>
<p:pic><p:nvPicPr><p:cNvPr id="2" name="Picture $n"/><p:cNvPicPr/><p:nvPr/></p:nvPicPr><p:blipFill><a:blip r:embed="rId1"/><a:stretch><a:fillRect/></a:stretch></p:blipFill><p:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="12192000" cy="6858000"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></p:spPr></p:pic>
</p:spTree></p:cSld></p:sld>""")
        put("ppt/slides/_rels/slide$n.xml.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="../media/$mediaName"/>
</Relationships>""")
    }
    zip.close()
    return out.toByteArray()
}
