package com.jisuanyusuiji.toolbox.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.jisuanyusuiji.toolbox.MainActivity
import com.jisuanyusuiji.toolbox.R

class QuickToolsWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context))
        }
    }

    companion object {
        fun buildViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_tools)
            views.setOnClickPendingIntent(
                R.id.btn_widget_dice,
                openToolIntent(context, "dice", 100)
            )
            views.setOnClickPendingIntent(
                R.id.btn_widget_calc,
                openToolIntent(context, "basic_calc", 200)
            )
            views.setOnClickPendingIntent(
                R.id.widget_root,
                openToolIntent(context, "home", 300)
            )
            return views
        }

        private fun openToolIntent(context: Context, toolId: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_TOOL, toolId)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
