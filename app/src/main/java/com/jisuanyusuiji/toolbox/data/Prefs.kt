package com.jisuanyusuiji.toolbox.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray

/**
 * 全局设置 / 收藏 / 使用记录。
 * 全部保存在本地 SharedPreferences，不上传任何数据。
 */
object Prefs {

    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"

    /** 用户协议版本号：以后修改协议内容时 +1，会要求用户重新同意。 */
    const val AGREEMENT_VERSION = 1

    private const val FILE = "toolbox_settings"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_SOUND = "sound_enabled"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_RECENT = "recent_tools"
    private const val KEY_AGREEMENT_VERSION = "agreement_version"

    private var appContext: Context? = null

    private val _themeMode = MutableStateFlow(THEME_SYSTEM)
    val themeMode: StateFlow<String> = _themeMode

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled

    private val _agreementAccepted = MutableStateFlow(false)
    val agreementAccepted: StateFlow<Boolean> = _agreementAccepted

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        _themeMode.value = sp().getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
        _soundEnabled.value = sp().getBoolean(KEY_SOUND, true)
        _agreementAccepted.value =
            sp().getInt(KEY_AGREEMENT_VERSION, 0) >= AGREEMENT_VERSION
    }

    fun acceptAgreement() {
        sp().edit().putInt(KEY_AGREEMENT_VERSION, AGREEMENT_VERSION).apply()
        _agreementAccepted.value = true
    }

    private fun sp(): SharedPreferences =
        checkNotNull(appContext) { "Prefs 尚未初始化，请先在 MainActivity 调用 Prefs.init()" }
            .getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        sp().edit().putString(KEY_THEME, mode).apply()
    }

    fun setSoundEnabled(enabled: Boolean) {
        _soundEnabled.value = enabled
        sp().edit().putBoolean(KEY_SOUND, enabled).apply()
    }

    // ---------- 收藏 ----------

    fun isFavorite(toolId: String): Boolean = list(KEY_FAVORITES).contains(toolId)

    fun setFavorite(toolId: String, favorite: Boolean) {
        val items = list(KEY_FAVORITES)
        if (favorite) {
            if (!items.contains(toolId)) items.add(toolId)
        } else {
            items.remove(toolId)
        }
        saveList(KEY_FAVORITES, items)
    }

    fun toggleFavorite(toolId: String): Boolean {
        val now = !isFavorite(toolId)
        setFavorite(toolId, now)
        return now
    }

    fun favorites(): List<String> = list(KEY_FAVORITES)

    // ---------- 最近使用 ----------

    fun addRecent(toolId: String) {
        val items = list(KEY_RECENT)
        items.remove(toolId)
        items.add(0, toolId)
        while (items.size > 20) items.removeAt(items.size - 1)
        saveList(KEY_RECENT, items)
    }

    fun recents(): List<String> = list(KEY_RECENT)

    // ---------- 清空 ----------

    fun clearAll() {
        sp().edit().clear().apply()
        _themeMode.value = THEME_SYSTEM
        _soundEnabled.value = true
        _agreementAccepted.value = false
    }

    // ---------- 内部 ----------

    private fun list(key: String): MutableList<String> {
        val raw = sp().getString(key, null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }.toMutableList()
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    private fun saveList(key: String, items: List<String>) {
        val arr = JSONArray()
        items.forEach { arr.put(it) }
        sp().edit().putString(key, arr.toString()).apply()
    }
}
