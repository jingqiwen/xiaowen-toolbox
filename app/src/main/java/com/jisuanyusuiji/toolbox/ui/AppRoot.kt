package com.jisuanyusuiji.toolbox.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jisuanyusuiji.toolbox.data.Prefs
import com.jisuanyusuiji.toolbox.data.ToolRegistry
import com.jisuanyusuiji.toolbox.ui.components.ToolHost
import com.jisuanyusuiji.toolbox.ui.screens.HomeScreen
import com.jisuanyusuiji.toolbox.ui.screens.SearchScreen
import com.jisuanyusuiji.toolbox.ui.screens.SettingsScreen

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val TOOL = "tool/{toolId}"

    fun tool(id: String) = "tool/$id"
}

@Composable
fun AppRoot(
    deepLinkToolId: String? = null,
    onConsumeDeepLink: () -> Unit = {}
) {
    val navController = rememberNavController()

    // 桌面小组件等外部入口：直接跳转到指定工具
    LaunchedEffect(deepLinkToolId) {
        if (!deepLinkToolId.isNullOrBlank() && ToolRegistry.byId(deepLinkToolId) != null) {
            navController.navigate(Routes.tool(deepLinkToolId)) {
                popUpTo(Routes.HOME)
            }
            onConsumeDeepLink()
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenTool = { id -> navController.navigate(Routes.tool(id)) },
                onSearch = { navController.navigate(Routes.SEARCH) },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onOpenTool = { id -> navController.navigate(Routes.tool(id)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.TOOL,
            arguments = listOf(navArgument("toolId") { type = NavType.StringType })
        ) { entry ->
            val toolId = entry.arguments?.getString("toolId") ?: ""
            val tool = ToolRegistry.byId(toolId)
            if (tool == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("工具不存在：$toolId")
                }
            } else {
                LaunchedEffect(toolId) {
                    Prefs.addRecent(toolId)
                }
                ToolHost(tool = tool, onBack = { navController.popBackStack() })
            }
        }
    }
}
