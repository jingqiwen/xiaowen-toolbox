package com.jisuanyusuiji.toolbox.tools.extra

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import com.jisuanyusuiji.toolbox.ui.components.ErrorText
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer

@Composable
private fun PickVideoButton(uri: Uri?, onPicked: (Uri?) -> Unit, label: String) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { onPicked(it) }
    Button(onClick = { launcher.launch("video/*") }, modifier = Modifier.fillMaxWidth()) {
        Text(if (uri == null) "📂 $label" else "✅ 已选择：${uri.lastPathSegment ?: "视频"}")
    }
}

private fun extractAudio(context: Context, uri: Uri, outFile: File): String? {
    val extractor = MediaExtractor()
    val muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    try {
        extractor.setDataSource(context, uri, null)
        var audioIndex = -1
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                audioIndex = i
                break
            }
        }
        if (audioIndex < 0) return "该视频没有音频轨道"
        val format = extractor.getTrackFormat(audioIndex)
        val track = muxer.addTrack(format)
        muxer.start()
        extractor.selectTrack(audioIndex)
        val buffer = ByteBuffer.allocate(1 shl 20)
        val info = MediaCodec.BufferInfo()
        while (true) {
            val size = extractor.readSampleData(buffer, 0)
            if (size < 0) break
            info.offset = 0
            info.size = size
            info.presentationTimeUs = extractor.sampleTime
            info.flags = if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0)
                MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
            muxer.writeSampleData(track, buffer, info)
            extractor.advance()
        }
        muxer.stop()
        return null
    } catch (e: Exception) {
        return e.message ?: "提取失败"
    } finally {
        runCatching { muxer.release() }
        runCatching { extractor.release() }
    }
}

/** 简单的 GIF89a 编码器（全局 RGB332 调色板 + LZW 压缩）。 */
class GifEncoder(private val width: Int, private val height: Int) {

    private val out = ByteArrayOutputStream()
    private val palette = IntArray(256)
    private val delay: Int

    init {
        // RGB332 调色板
        for (r in 0..7) for (g in 0..7) for (b in 0..3) {
            val i = (r shl 5) or (g shl 2) or b
            palette[i] = ((r * 255 / 7) shl 16) or ((g * 255 / 7) shl 8) or (b * 255 / 3)
        }
        out.write(byteArrayOf('G'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), '8'.code.toByte(), '9'.code.toByte(), 'a'.code.toByte()))
        writeShort(width); writeShort(height)
        // 全局颜色表
        out.write(0xF7)
        out.write(0) // bg color index
        out.write(0) // aspect
        for (c in palette) {
            out.write((c shr 16) and 0xFF)
            out.write((c shr 8) and 0xFF)
            out.write(c and 0xFF)
        }
        delay = 0
    }

    fun addFrame(bitmap: Bitmap, delayCs: Int) {
        val indices = ByteArray(width * height)
        val px = IntArray(width * height)
        bitmap.getPixels(px, 0, width, 0, 0, width, height)
        for (i in px.indices) {
            val r = ((px[i] shr 16) and 0xFF) shr 5
            val g = ((px[i] shr 8) and 0xFF) shr 5
            val b = (px[i] and 0xFF) shr 6
            indices[i] = ((r shl 5) or (g shl 2) or b).toByte()
        }
        // GCE
        out.write(0x21); out.write(0xF9); out.write(4)
        out.write(0)
        writeShort(delayCs)
        out.write(0); out.write(0)
        // Image descriptor
        out.write(0x2C)
        writeShort(0); writeShort(0); writeShort(width); writeShort(height)
        out.write(0)
        lzw(indices)
    }

    fun finish(): ByteArray {
        out.write(0x3B)
        return out.toByteArray()
    }

    private fun lzw(indices: ByteArray) {
        val dict = HashMap<String, Int>(4096)
        for (i in 0..255) dict[i.toString()] = i
        var nextCode = 258
        var codeWidth = 9
        var bitBuf = 0
        var bitCount = 0

        fun writeCode(code: Int) {
            bitBuf = bitBuf or (code shl bitCount)
            bitCount += codeWidth
            while (bitCount >= 8) {
                out.write(bitBuf and 0xFF)
                bitBuf = bitBuf shr 8
                bitCount -= 8
            }
        }

        out.write(8) // min code size
        writeCode(256) // clear
        var w = ""
        for (i in indices.indices) {
            val k = (indices[i].toInt() and 0xFF).toString()
            val wk = if (w.isEmpty()) k else "$w,$k"
            if (dict.containsKey(wk)) {
                w = wk
            } else {
                writeCode(dict[w]!!)
                dict[wk] = nextCode++
                if (nextCode == (1 shl codeWidth) && codeWidth < 12) codeWidth++
                if (nextCode >= 4096) {
                    writeCode(256)
                    dict.clear()
                    for (j in 0..255) dict[j.toString()] = j
                    nextCode = 258
                    codeWidth = 9
                }
                w = k
            }
        }
        if (w.isNotEmpty()) writeCode(dict[w]!!)
        writeCode(257)
        if (bitCount > 0) out.write(bitBuf and 0xFF)
    }

    private fun writeShort(v: Int) {
        out.write(v and 0xFF)
        out.write((v shr 8) and 0xFF)
    }
}

// ============================================================
// 视频转音频
// ============================================================
@Composable
fun VideoToAudioTool() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uri by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    fun run() {
        val u = uri ?: run { error = "请先选择视频"; return }
        error = ""
        message = ""
        busy = true
        scope.launch {
            val outFile = File(context.cacheDir, "audio_${System.currentTimeMillis()}.m4a")
            val err = withContext(Dispatchers.IO) { extractAudio(context, u, outFile) }
            busy = false
            if (err != null) {
                error = err
            } else {
                val bytes = outFile.readBytes()
                message = saveBytesToGallery(context, bytes, "audio/mp4", "AUDIO_${System.currentTimeMillis()}.m4a")
            }
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "视频转音频（提取原声）") {
            PickVideoButton(uri, { uri = it }, "选择视频")
            Spacer(Modifier.height(12.dp))
            Button(onClick = { run() }, enabled = uri != null && !busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (busy) "提取中…" else "提取为 M4A 音频")
            }
        }
        ErrorText(error)
        if (message.isNotBlank()) SectionCard(title = "结果") { Text(message) }
        Text("说明：从视频中直接抽取音频轨道，不重新编码；仅当视频带音频时可用。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}

// ============================================================
// 视频转 GIF
// ============================================================
@Composable
fun VideoToGifTool() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uri by remember { mutableStateOf<Uri?>(null) }
    var fps by remember { mutableStateOf(10) }
    var frames by remember { mutableStateOf(20) }
    var width by remember { mutableStateOf(320) }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    fun run() {
        val u = uri ?: run { error = "请先选择视频"; return }
        error = ""
        message = ""
        busy = true
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, u)
                    val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    if (durationMs <= 0) { error = "无法读取视频时长"; return@withContext }
                    val encoder = GifEncoder(width, width)
                    val delay = (100 / fps).coerceAtLeast(1)
                    for (i in 0 until frames) {
                        val t = durationMs * (i + 1) / (frames + 1)
                        val frame = retriever.getFrameAtTime(t * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                            ?: continue
                        val scale = width.toFloat() / frame.width
                        val scaled = Bitmap.createScaledBitmap(frame, width, (frame.height * scale).toInt().coerceAtLeast(1), true)
                        encoder.addFrame(scaled, delay)
                    }
                    retriever.release()
                    val bytes = encoder.finish()
                    message = saveBytesToGallery(context, bytes, "image/gif", "GIF_${System.currentTimeMillis()}.gif")
                } catch (e: Exception) {
                    error = "转换失败：${e.message}"
                }
                busy = false
            }
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "视频转 GIF") {
            PickVideoButton(uri, { uri = it }, "选择视频")
            Spacer(Modifier.height(10.dp))
            ChoiceChips(listOf(5, 10, 15), fps, { fps = it }, { "${it} fps" })
            Spacer(Modifier.height(8.dp))
            ChoiceChips(listOf(10, 20, 30), frames, { frames = it }, { "$it 帧" })
            Spacer(Modifier.height(8.dp))
            ChoiceChips(listOf(240, 320, 480), width, { width = it }, { "宽 ${it}px" })
            Spacer(Modifier.height(12.dp))
            Button(onClick = { run() }, enabled = uri != null && !busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (busy) "转换中…" else "转换为 GIF")
            }
        }
        ErrorText(error)
        if (message.isNotBlank()) SectionCard(title = "结果") { Text(message) }
        Text("说明：按时间均匀抽取帧；画面为方形（按宽度等比缩放）。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}

// ============================================================
// 视频格式转换 / 变速 / 重新编码压缩（Media3 Transformer）
// ============================================================
@Composable
fun VideoEditTool() {
    val context = LocalContext.current
    var uri by remember { mutableStateOf<Uri?>(null) }
    var speed by remember { mutableStateOf(1.0f) }
    var mode by remember { mutableStateOf("转 MP4 / 压缩") }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var transformer by remember { mutableStateOf<Transformer?>(null) }

    fun run() {
        val u = uri ?: run { error = "请先选择视频"; return }
        error = ""
        message = ""
        busy = true
        val outFile = File(context.cacheDir, "edit_${System.currentTimeMillis()}.mp4")
        val t = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    busy = false
                    val bytes = outFile.readBytes()
                    message = saveBytesToGallery(context, bytes, "video/mp4", "VIDEO_${System.currentTimeMillis()}.mp4") +
                        "\n输出：${outFile.absolutePath}"
                    transformer = null
                }

                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    busy = false
                    error = "处理失败：${exportException.message ?: "不支持的格式或编码"}"
                    transformer = null
                }
            })
            .build()
        transformer = t
        val effects = if (speed == 1.0f) Effects.EMPTY
        else Effects(listOf(), listOf(SpeedChangeEffect(speed)))
        val item = EditedMediaItem.Builder(MediaItem.fromUri(u)).setEffects(effects).build()
        t.start(item, outFile.absolutePath)
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "视频处理（本地）") {
            PickVideoButton(uri, { uri = it }, "选择视频")
            Spacer(Modifier.height(10.dp))
            ChoiceChips(
                listOf("转 MP4 / 压缩", "变速处理"),
                mode,
                { mode = it },
                { it }
            )
            if (mode == "变速处理") {
                Spacer(Modifier.height(8.dp))
                ChoiceChips(
                    listOf(0.5f, 0.75f, 1.25f, 1.5f, 2.0f),
                    speed,
                    { speed = it },
                    { "${it}x" }
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { run() }, enabled = uri != null && !busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (busy) "处理中…" else "开始处理")
            }
        }
        ErrorText(error)
        if (message.isNotBlank()) SectionCard(title = "结果") { Text(message) }
        SectionCard(title = "支持范围说明") {
            Text(
                "输出：MP4（H.264 + AAC）。\n输入：MP4、MOV、TS、MKV、FLV 等常见格式；AVI、特殊编码的 MKV/FLV 可能不支持。\n" +
                    "“压缩”为重新编码，体积通常减小；变速会保持音调。\n倒放目前系统 API 不支持，需要 FFmpeg 才能实现。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
