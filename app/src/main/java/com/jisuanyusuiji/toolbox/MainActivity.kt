package com.jisuanyusuiji.toolbox

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.jisuanyusuiji.toolbox.data.Prefs
import com.jisuanyusuiji.toolbox.ui.AppRoot
import com.jisuanyusuiji.toolbox.ui.screens.SplashLoadingScreen
import com.jisuanyusuiji.toolbox.ui.screens.UserAgreementScreen
import com.jisuanyusuiji.toolbox.ui.theme.ToolboxTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_TOOL = "open_tool_id"
    }

    private val pendingToolId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        enableEdgeToEdge()
        pendingToolId.value = intent?.getStringExtra(EXTRA_OPEN_TOOL)
        setContent {
            val themeMode by Prefs.themeMode.collectAsState()
            val agreementAccepted by Prefs.agreementAccepted.collectAsState()
            ToolboxTheme(themeMode = themeMode) {
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(4000L) // 启动加载图显示 4 秒
                    showSplash = false
                }
                when {
                    showSplash -> SplashLoadingScreen()
                    !agreementAccepted -> UserAgreementScreen(
                        onAgree = { Prefs.acceptAgreement() },
                        onDecline = { finish() }
                    )
                    else -> AppRoot(
                        deepLinkToolId = pendingToolId.value,
                        onConsumeDeepLink = { pendingToolId.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingToolId.value = intent.getStringExtra(EXTRA_OPEN_TOOL)
    }
}
