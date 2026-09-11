package com.jisuanyusuiji.toolbox.tools.extra

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.jisuanyusuiji.toolbox.ui.components.SectionCard
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ============================================================
// 手电筒
// ============================================================
@Composable
fun FlashlightTool() {
    val context = LocalContext.current
    val cameraManager = remember {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
    val flashCameraId = remember {
        try {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: Exception) {
            null
        }
    }
    var on by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    fun setTorch(next: Boolean) {
        error = ""
        val id = flashCameraId ?: run { error = "本机没有可用的闪光灯"; return }
        try {
            cameraManager.setTorchMode(id, next)
            on = next
        } catch (e: Exception) {
            error = "打开失败：${e.message}"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) setTorch(true) else error = "需要相机权限才能控制闪光灯"
    }

    DisposableEffect(Unit) {
        onDispose {
            if (on) {
                try { flashCameraId?.let { cameraManager.setTorchMode(it, false) } } catch (_: Exception) {}
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(170.dp)
                .background(if (on) Color(0xFFFFD54F) else Color(0xFF424242), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(if (on) "🔦" else "🔅", fontSize = 64.sp)
        }
        Spacer(Modifier.height(20.dp))
        Text(if (on) "手电筒已打开" else "手电筒已关闭", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = {
                if (!on && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                } else {
                    setTorch(!on)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (on) "关闭手电筒" else "打开手电筒") }
        if (error.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "提示：退出本页面会自动关闭手电筒，避免耗电。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================
// 指南针
// ============================================================
@Composable
fun CompassTool() {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    var azimuth by remember { mutableStateOf(0f) }
    var available by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val accel = FloatArray(3)
        val mag = FloatArray(3)
        var hasAccel = false
        var hasMag = false
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, accel, 0, 3)
                    hasAccel = true
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, mag, 0, 3)
                    hasMag = true
                }
                if (hasAccel && hasMag) {
                    val r = FloatArray(9)
                    val i = FloatArray(9)
                    if (SensorManager.getRotationMatrix(r, i, accel, mag)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(r, orientation)
                        azimuth = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        val a = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val m = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (a == null || m == null) {
            available = false
        } else {
            sensorManager.registerListener(listener, a, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(listener, m, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    val direction = when (((azimuth + 22.5f) % 360f / 45f).toInt()) {
        0 -> "北 N"
        1 -> "东北 NE"
        2 -> "东 E"
        3 -> "东南 SE"
        4 -> "南 S"
        5 -> "西南 SW"
        6 -> "西 W"
        else -> "西北 NW"
    }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!available) {
            Text("本机没有磁力计/加速度计，无法使用指南针")
            return@Column
        }
        Canvas(Modifier.size(300.dp)) {            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.minDimension / 2f - 10f
            drawCircle(Color(0xFFFAFAFA), radius = radius, center = Offset(cx, cy))
            drawCircle(Color(0xFF37474F), radius = radius, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f))
            drawCircle(Color(0xFFCFD8DC), radius = radius - 30f, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f))

            // 刻度盘：随方位角反向旋转，使 N 始终指向磁北
            rotate(degrees = -azimuth, pivot = Offset(cx, cy)) {
                for (deg in 0 until 360 step 2) {
                    val angle = Math.toRadians(deg.toDouble())
                    val isMajor = deg % 30 == 0
                    val isMedium = deg % 10 == 0
                    val isFive = deg % 5 == 0
                    val tickLen = when {
                        isMajor -> 22f
                        isMedium -> 15f
                        isFive -> 11f
                        else -> 6f
                    }
                    val startRadius = radius - tickLen
                    drawLine(
                        color = when {
                            isMajor -> Color(0xFF263238)
                            isMedium -> Color(0xFF546E7A)
                            else -> Color(0xFFB0BEC5)
                        },
                        start = Offset(
                            cx + (startRadius * sin(angle)).toFloat(),
                            cy - (startRadius * cos(angle)).toFloat()
                        ),
                        end = Offset(
                            cx + (radius * sin(angle)).toFloat(),
                            cy - (radius * cos(angle)).toFloat()
                        ),
                        strokeWidth = when {
                            isMajor -> 3.5f
                            isMedium -> 2.5f
                            else -> 1.2f
                        }
                    )
                }

                val numberPaint = android.graphics.Paint().apply {
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 13.dp.toPx()
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
                    color = android.graphics.Color.rgb(38, 50, 56)
                }
                val letterPaint = android.graphics.Paint().apply {
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 22.dp.toPx()
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
                }
                drawIntoCanvas { canvas ->
                    // 每 30° 的数字（N/E/S/W 处用字母）
                    for (deg in 30 until 360 step 30) {
                        if (deg % 90 == 0) continue
                        val angle = Math.toRadians(deg.toDouble())
                        val r = radius - 44f
                        canvas.save()
                        canvas.translate(
                            cx + (r * sin(angle)).toFloat(),
                            cy - (r * cos(angle)).toFloat()
                        )
                        canvas.rotate(azimuth)
                        canvas.nativeCanvas.drawText("$deg", 0f, numberPaint.textSize / 3f, numberPaint)
                        canvas.restore()
                    }
                    // N / E / S / W
                    listOf("N" to Color(0xFFD32F2F), "E" to Color(0xFF263238), "S" to Color(0xFF263238), "W" to Color(0xFF263238))
                        .forEachIndexed { index, (text, color) ->
                            val angle = index * 90.0 * PI / 180.0
                            val r = radius - 62f
                            canvas.save()
                            canvas.translate(
                                cx + (r * sin(angle)).toFloat(),
                                cy - (r * cos(angle)).toFloat()
                            )
                            canvas.rotate(azimuth)
                            letterPaint.color = color.toArgb()
                            canvas.nativeCanvas.drawText(text, 0f, letterPaint.textSize / 3f, letterPaint)
                            canvas.restore()
                        }
                }
            }

            // 顶部固定指针：指针正对的方向就是手机朝向
            val pointer = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx, cy - radius + 8f)
                lineTo(cx - 14f, cy - radius + 30f)
                lineTo(cx + 14f, cy - radius + 30f)
                close()
            }
            drawPath(pointer, Color(0xFFD32F2F))
            drawLine(Color(0xFFD32F2F), Offset(cx, cy), Offset(cx, cy - radius + 34f), strokeWidth = 4f)

            // 中心圆盘
            drawCircle(Color.White, radius = 26f, center = Offset(cx, cy))
            drawCircle(Color(0xFF37474F), radius = 26f, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
            drawCircle(Color(0xFFD32F2F), radius = 5f, center = Offset(cx, cy))
        }
        Spacer(Modifier.height(16.dp))
        Text("$direction  ${"%.0f".format(azimuth)}°", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "刻度：每 2° 一格、每 10° 一条长线、每 30° 标注角度；红色指针指向手机正前方。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text("请将手机平放，远离磁铁、金属和电子设备；走 8 字可校准磁力计。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ============================================================
// 镜子
// ============================================================
@Composable
fun MirrorTool() {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var front by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok -> granted = ok }

    if (!granted) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("镜子功能需要使用前置摄像头，仅在本机预览，不会拍照或上传。")
            Spacer(Modifier.height(16.dp))
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("授予相机权限") }
        }
        return
    }

    val lifecycleOwner = LocalContext.current as androidx.lifecycle.LifecycleOwner
    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            },
            update = { view ->
                val future = ProcessCameraProvider.getInstance(context)
                future.addListener({
                    try {
                        val provider = future.get()
                        val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                        val selector = CameraSelector.Builder()
                            .requireLensFacing(if (front) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK)
                            .build()
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, selector, preview)
                    } catch (e: Exception) {
                        error = "相机启动失败：${e.message}"
                    }
                }, ContextCompat.getMainExecutor(context))
            },
            modifier = Modifier
                .fillMaxSize()
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0x88000000))
                .padding(16.dp)
        ) {
            Text(
                "画面按真实方向显示，不做左右翻转；前置摄像头也保持与后置一致的方向。",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (front) "当前：前置摄像头" else "当前：后置摄像头", color = Color.White, modifier = Modifier.weight(1f))
                TextButton(onClick = { front = !front }) { Text("切换", color = Color.White) }
            }
            if (error.isNotBlank()) Text(error, color = Color(0xFFFF8A80))
        }
    }
}

// ============================================================
// 录音机
// ============================================================
private fun recordingDir(context: Context): File =
    File(context.filesDir, "recordings").apply { mkdirs() }

@Composable
fun RecorderTool() {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var recording by remember { mutableStateOf(false) }
    var elapsed by remember { mutableStateOf(0) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var currentFile by remember { mutableStateOf<File?>(null) }
    var files by remember { mutableStateOf(recordingDir(context).listFiles()?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()) }
    var playing by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingFile by remember { mutableStateOf<File?>(null) }
    var error by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok -> granted = ok }

    LaunchedEffect(recording) {
        while (recording) {
            delay(1000)
            elapsed++
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try { recorder?.stop(); recorder?.release() } catch (_: Exception) {}
            try { playing?.stop(); playing?.release() } catch (_: Exception) {}
        }
    }

    fun startRecording() {
        error = ""
        try {
            val file = File(recordingDir(context), "REC_${System.currentTimeMillis()}.m4a")
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(128000)
            r.setAudioSamplingRate(44100)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            currentFile = file
            elapsed = 0
            recording = true
        } catch (e: Exception) {
            error = "录音启动失败：${e.message}"
        }
    }

    fun stopRecording() {
        try {
            recorder?.stop()
            recorder?.release()
        } catch (e: Exception) {
            error = "停止录音失败：${e.message}"
        }
        recorder = null
        recording = false
        files = recordingDir(context).listFiles()?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
    }

    fun play(file: File) {
        try {
            playing?.stop(); playing?.release()
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
            }
            playing = player
            playingFile = file
            player.setOnCompletionListener {
                playingFile = null
                it.release()
                playing = null
            }
        } catch (e: Exception) {
            error = "播放失败：${e.message}"
        }
    }

    if (!granted) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("录音机需要麦克风权限，录音文件只保存在本机。")
            Spacer(Modifier.height(16.dp))
            Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) { Text("授予麦克风权限") }
        }
        return
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionCard(title = "🎙️ 录音机") {
            val seconds = elapsed
            Text(
                "%02d:%02d".format(seconds / 60, seconds % 60),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { if (recording) stopRecording() else startRecording() },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (recording) "⏹ 停止录音" else "🔴 开始录音") }
            if (error.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        }

        SectionCard(title = "录音文件（本机）") {
            if (files.isEmpty()) {
                Text("暂无录音", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                files.forEach { file ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(file.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(file.lastModified())) +
                                    " · ${file.length() / 1024} KB",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = {
                                    if (playingFile == file) {
                                        playing?.stop(); playing?.release(); playing = null; playingFile = null
                                    } else play(file)
                                }) { Text(if (playingFile == file) "停止" else "播放") }
                                TextButton(onClick = { shareFile(context, file, "audio/mp4") }) { Text("分享") }
                                TextButton(onClick = {
                                    if (playingFile == file) {
                                        playing?.stop(); playing?.release(); playing = null; playingFile = null
                                    }
                                    file.delete()
                                    files = recordingDir(context).listFiles()?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
                                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
    }
}
