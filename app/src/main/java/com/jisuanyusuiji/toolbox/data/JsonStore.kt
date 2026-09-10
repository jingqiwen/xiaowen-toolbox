package com.jisuanyusuiji.toolbox.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 简单的 JSON 键值存储：用于保存骰子历史、名单、转盘模板、卡池等。
 * 每个工具使用独立的 SharedPreferences 文件（store_xxx）。
 */
class JsonStore(context: Context, name: String) {

    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("store_$name", Context.MODE_PRIVATE)

    fun putString(key: String, value: String) {
        sp.edit().putString(key, value).apply()
    }

    fun getString(key: String): String? = sp.getString(key, null)

    fun putArray(key: String, array: JSONArray) = putString(key, array.toString())

    fun getArray(key: String): JSONArray = try {
        JSONArray(getString(key) ?: "[]")
    } catch (_: Exception) {
        JSONArray()
    }

    fun putObject(key: String, obj: JSONObject) = putString(key, obj.toString())

    fun getObject(key: String): JSONObject = try {
        JSONObject(getString(key) ?: "{}")
    } catch (_: Exception) {
        JSONObject()
    }

    fun remove(key: String) {
        sp.edit().remove(key).apply()
    }

    fun keys(): List<String> = sp.all.keys.sorted()

    fun clear() {
        sp.edit().clear().apply()
    }
}

/** 清空所有工具级本地数据（保留应用设置文件本身，由 Prefs.clearAll 处理）。 */
object StoreCleaner {
    fun clearAll(context: Context) {
        val dir = File(context.applicationInfo.dataDir, "shared_prefs")
        dir.listFiles()?.forEach { f ->
            val name = f.name.removeSuffix(".xml")
            if (name.startsWith("store_")) {
                context.getSharedPreferences(name, Context.MODE_PRIVATE)
                    .edit().clear().commit()
            }
        }
    }
}
