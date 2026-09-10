package com.jisuanyusuiji.toolbox.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.R

/**
 * 启动加载页：默认读取 assets/splash_loading.png（用户提供的加载图），
 * 如果还没有放入图片，则显示内置图标作为占位。
 */
@Composable
fun SplashLoadingScreen() {
    val context = LocalContext.current
    val splashBitmap = remember {
        try {
            context.assets.open("splash_loading.png").use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) {
            null
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1020)),
        contentAlignment = Alignment.Center
    ) {
        if (splashBitmap != null) {
            Image(
                bitmap = splashBitmap.asImageBitmap(),
                contentDescription = "启动加载图",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = "应用图标",
                    modifier = Modifier.size(140.dp)
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    "小温工具箱",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "正在加载…",
                    color = Color(0xFFB9C3FF),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
