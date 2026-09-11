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
        // Netscape 2.0 循环扩展：无限循环播放
        out.write(0x21); out.write(0xFF); out.write(11)
        out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
        out.write(3); out.write(1)
        writeShort(0)
        out.write(0)
        delay = 0
    }

    fun addFrame(bitmap: Bitmap, delayCs: Int) {
        // 统一缩放到编码器尺寸，避免尺寸不一致导致的黑边/黑块或越界
        val src = if (bitmap.width == width && bitmap.height == height) bitmap
        else Bitmap.createScaledBitmap(bitmap, width, height, true)
        val indices = ByteArray(width * height)
        val px = IntArray(width * height)
        src.getPixels(px, 0, width, 0, 0, width, height)
        for (i in px.indices) {
            val r = ((px[i] shr 16) and 0xFF) shr 5
            val g = ((px[i] shr 8) and 0xFF) shr 5
            val b = (px[i] and 0xFF) shr 6
            indices[i] = ((r shl 5) or (g shl 2) or b).toByte()
        }
        if (src !== bitmap) src.recycle()
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
    var saved by remember { mutableStateOf<SavedMedia?>(null) }
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
                saved = saveMedia(context, bytes, "audio/mp4", "AUDIO_${System.currentTimeMillis()}.m4a")
                message = "音频已提取"
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
        if (message.isNotBlank()) {
            SectionCard(title = "结果") {
                Text(message)
                saved?.let { MediaResultActions(it) }
            }
        }
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
    var saved by remember { mutableStateOf<SavedMedia?>(null) }
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
                    var srcW = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                    var srcH = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                    val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                    if (rotation == 90 || rotation == 270) { val tmp = srcW; srcW = srcH; srcH = tmp }
                    if (srcW <= 0 || srcH <= 0) {
                        retriever.getFrameAtTime(0)?.let { srcW = it.width; srcH = it.height }
                    }
                    val targetH = if (srcW > 0 && srcH > 0) {
                        (width.toLong() * srcH / srcW).toInt().coerceIn(16, 2400)
                    } else width
                    val encoder = GifEncoder(width, targetH)
                    val delay = (100 / fps).coerceAtLeast(2) // GIF 最小延时约 2/100 秒，避免部分播放器把 0 当作 10
                    for (i in 0 until frames) {
                        val t = durationMs * (i + 1) / (frames + 1)
                        val frame = retriever.getFrameAtTime(t * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                            ?: continue
                        val scaled = Bitmap.createScaledBitmap(frame, width, targetH, true)
                        encoder.addFrame(scaled, delay)
                        if (scaled !== frame) scaled.recycle()
                    }
                    retriever.release()
                    val bytes = encoder.finish()
                    saved = saveMedia(context, bytes, "image/gif", "GIF_${System.currentTimeMillis()}.gif")
                    message = "GIF 已生成（${width}×${targetH}，${frames} 帧，${fps} fps，循环播放）"
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
        if (message.isNotBlank()) {
            SectionCard(title = "结果") {
                Text(message)
                saved?.let { MediaResultActions(it) }
            }
        }
        Text("说明：按时间均匀抽取帧，保持原视频宽高比，输出动图会无限循环播放。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
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
    var codec by remember { mutableStateOf("H.264（兼容性最好）") }
    var quality by remember { mutableStateOf("原始分辨率") }
    var frameRate by remember { mutableStateOf(0) }
    var mute by remember { mutableStateOf(false) }
    var audioOnly by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf<SavedMedia?>(null) }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var transformer by remember { mutableStateOf<Transformer?>(null) }

    fun run() {
        val u = uri ?: run { error = "请先选择视频"; return }
        error = ""
        message = ""
        busy = true
        val extension = if (audioOnly) "m4a" else "mp4"
        val mime = if (audioOnly) "audio/mp4" else "video/mp4"
        val outFile = File(context.cacheDir, "edit_${System.currentTimeMillis()}.$extension")
        val t = Transformer.Builder(context)
            .setVideoMimeType(if (codec.startsWith("H.265")) MimeTypes.VIDEO_H265 else MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    busy = false
                    val bytes = outFile.readBytes()
                    saved = saveMedia(context, bytes, mime, "VIDEO_${System.currentTimeMillis()}.$extension")
                    message = if (audioOnly) "已导出为音频（M4A / AAC）" else "视频处理完成"
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
        val videoEffects = mutableListOf<androidx.media3.common.Effect>()
        if (speed != 1.0f) videoEffects += SpeedChangeEffect(speed)
        if (!audioOnly && quality != "原始分辨率") {
            val targetH = when (quality) {
                "1080p" -> 1080
                "720p" -> 720
                else -> 480
            }
            val mmr = MediaMetadataRetriever()
            try {
                mmr.setDataSource(context, u)
                val srcH = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                if (srcH > targetH) {
                    val scale = targetH.toFloat() / srcH
                    videoEffects += androidx.media3.effect.ScaleAndRotateTransformation.Builder()
                        .setScale(scale, scale)
                        .build()
                }
            } catch (_: Exception) {
            } finally {
                try { mmr.release() } catch (_: Exception) {}
            }
        }
        val item = EditedMediaItem.Builder(MediaItem.fromUri(u)).apply {
            setEffects(Effects(listOf(), videoEffects))
            if (audioOnly) setRemoveVideo(true)
            if (mute && !audioOnly) setRemoveAudio(true)
            if (!audioOnly && frameRate > 0) setFrameRate(frameRate)
        }.build()
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
            Spacer(Modifier.height(10.dp))
            Text("输出类型", style = MaterialTheme.typography.bodyMedium)
            ChoiceChips(
                listOf(false, true),
                audioOnly,
                { audioOnly = it },
                { if (it) "仅音频 M4A" else "视频 MP4" }
            )
            if (!audioOnly) {
                Spacer(Modifier.height(8.dp))
                Text("编码", style = MaterialTheme.typography.bodyMedium)
                ChoiceChips(
                    listOf("H.264（兼容性最好）", "H.265（体积更小）"),
                    codec,
                    { codec = it },
                    { it }
                )
                Spacer(Modifier.height(8.dp))
                Text("分辨率", style = MaterialTheme.typography.bodyMedium)
                ChoiceChips(
                    listOf("原始分辨率", "1080p", "720p", "480p"),
                    quality,
                    { quality = it },
                    { it }
                )
                Spacer(Modifier.height(8.dp))
                Text("帧率", style = MaterialTheme.typography.bodyMedium)
                ChoiceChips(
                    listOf(0, 24, 30, 60),
                    frameRate,
                    { frameRate = it },
                    { if (it == 0) "保持原始" else "$it fps" }
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("静音（不要声音）", modifier = Modifier.weight(1f))
                androidx.compose.material3.Switch(checked = mute, onCheckedChange = { mute = it }, enabled = !audioOnly)
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { run() }, enabled = uri != null && !busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (busy) "处理中…" else "开始处理")
            }
        }
        ErrorText(error)
        if (message.isNotBlank()) {
            SectionCard(title = "结果") {
                Text(message)
                saved?.let { MediaResultActions(it) }
            }
        }
        SectionCard(title = "支持范围说明") {
            Text(
                "输出：MP4（H.264/H.265 + AAC）或仅音频 M4A（AAC）。\n" +
                    "输入：MP4、MOV、TS、MKV、FLV 等常见格式；AVI、特殊编码的 MKV/FLV 可能不支持。\n" +
                    "“压缩”为重新编码，体积通常减小；变速会保持音调；H.265 需要手机硬件支持，不支持时自动回退 H.264。\n" +
                    "倒放目前系统 API 不支持，需要 FFmpeg 才能实现。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
