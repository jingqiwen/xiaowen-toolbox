package com.jisuanyusuiji.toolbox.ui

import android.media.AudioManager
import android.media.ToneGenerator
import com.jisuanyusuiji.toolbox.data.Prefs

/** 简单的本地提示音（不申请权限、无音频文件）。受“音效开关”控制。 */
object Sfx {

    private var generator: ToneGenerator? = null

    fun tick() {
        if (!Prefs.soundEnabled.value) return
        try {
            if (generator == null) {
                generator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
            }
            generator?.startTone(ToneGenerator.TONE_PROP_BEEP, 90)
        } catch (_: Exception) {
            // 个别设备不支持 ToneGenerator 时静默忽略
        }
    }

    fun release() {
        generator?.release()
        generator = null
    }
}
