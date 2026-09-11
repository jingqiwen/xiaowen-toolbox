package com.jisuanyusuiji.toolbox.tools.knowledge

import android.content.Context
import com.jisuanyusuiji.toolbox.data.JsonStore
import org.json.JSONArray
import org.json.JSONObject

/** 公式收藏与个人笔记（本地保存，不上传）。 */
class FormulaBook(context: Context) {

    private val store = JsonStore(context, "formula_book")

    /** 用“名称 + 表达式”作为公式的唯一标识。 */
    fun keyOf(item: FormulaItem): String = item.name + "||" + item.expression

    fun favorites(): Set<String> {
        val arr = store.getArray("favorites")
        return (0 until arr.length()).mapNotNull { i ->
            try { arr.getString(i) } catch (_: Exception) { null }
        }.toSet()
    }

    fun isFavorite(key: String): Boolean = favorites().contains(key)

    /** 切换收藏，返回切换后是否已收藏。 */
    fun toggle(key: String): Boolean {
        val set = favorites().toMutableSet()
        val nowFavorite = if (set.contains(key)) {
            set.remove(key); false
        } else {
            set.add(key); true
        }
        val arr = JSONArray()
        set.forEach { arr.put(it) }
        store.putArray("favorites", arr)
        return nowFavorite
    }

    fun note(key: String): String = store.getObject("notes").optString(key, "")

    fun setNote(key: String, note: String) {
        val obj: JSONObject = store.getObject("notes")
        if (note.isBlank()) obj.remove(key) else obj.put(key, note)
        store.putObject("notes", obj)
    }
}
