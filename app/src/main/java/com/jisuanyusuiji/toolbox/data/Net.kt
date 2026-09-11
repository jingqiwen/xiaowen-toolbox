package com.jisuanyusuiji.toolbox.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

/**
 * 极简联网工具。
 *
 * 说明：App 的网络能力完全可选，只有在设置里打开“允许联网”后才发起请求，
 * 目前仅用于：① 背单词在线补充释义/例句；② 检查 GitHub 版本更新。
 * 不上传任何用户数据。
 */
object Net {

    suspend fun get(url: String, timeoutMs: Int = 9000): String? = withContext(Dispatchers.IO) {
        if (!Prefs.networkEnabled.value) return@withContext null
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "xiaowen-toolbox/1.0 (Android)")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Accept-Encoding", "gzip")
            }
            val code = conn.responseCode
            if (code !in 200..299) return@withContext null
            val stream = if (conn.contentEncoding?.contains("gzip", true) == true) {
                GZIPInputStream(conn.inputStream)
            } else {
                conn.inputStream
            }
            stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            try { conn?.disconnect() } catch (_: Exception) {}
        }
    }
}
