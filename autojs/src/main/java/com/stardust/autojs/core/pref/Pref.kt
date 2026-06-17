package com.stardust.autojs.core.pref

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.stardust.app.GlobalAppContext.get
import com.stardust.autojs.core.pref.PrefKey.KEY_FOREGROUND_SERVICE
import com.stardust.autojs.core.pref.PrefKey.KEY_REMOTE_CONTROL_KEEP_ALIVE
import com.stardust.autojs.core.pref.PrefKey.KEY_REMOTE_CONTROL_KEEP_ALIVE_REASONS

object Pref {
    private var inr: SharedPreferences? = null
    private val preferences: SharedPreferences
        get() = inr ?: PreferenceManager.getDefaultSharedPreferences(get())

    private val keepAliveLock = Any()


    val isStableModeEnabled: Boolean
        get() {
            return preferences.getBoolean("key_stable_mode", false)
        }

    val isGestureObservingEnabled: Boolean
        get() {
            return preferences.getBoolean("key_gesture_observing", false)
        }

    val isForegroundServiceEnabled: Boolean
        get() {
            return preferences.getBoolean(KEY_FOREGROUND_SERVICE, false)
        }

    val isRemoteControlKeepAliveEnabled: Boolean
        get() {
            return synchronized(keepAliveLock) {
                preferences.getBoolean(KEY_REMOTE_CONTROL_KEEP_ALIVE, false) ||
                    preferences.getStringSet(KEY_REMOTE_CONTROL_KEEP_ALIVE_REASONS, emptySet())
                        .orEmpty()
                        .isNotEmpty()
            }
        }

    val shouldKeepScriptProcessAlive: Boolean
        get() {
            return isForegroundServiceEnabled || isRemoteControlKeepAliveEnabled
        }

    fun setRemoteControlKeepAliveEnabled(enabled: Boolean) {
        synchronized(keepAliveLock) {
            val reasons = mutableSetOf<String>()
            if (enabled) {
                reasons += REMOTE_CONTROL_KEEP_ALIVE_REASON_LEGACY
            }
            preferences.edit()
                .putBoolean(KEY_REMOTE_CONTROL_KEEP_ALIVE, enabled)
                .putStringSet(KEY_REMOTE_CONTROL_KEEP_ALIVE_REASONS, reasons)
                .apply()
        }
    }

    fun acquireRemoteControlKeepAlive(reason: String) {
        synchronized(keepAliveLock) {
            val reasons = getRemoteControlKeepAliveReasonsLocked().toMutableSet()
            reasons += reason
            writeRemoteControlKeepAliveReasonsLocked(reasons)
        }
    }

    fun releaseRemoteControlKeepAlive(reason: String) {
        synchronized(keepAliveLock) {
            val reasons = getRemoteControlKeepAliveReasonsLocked().toMutableSet()
            reasons -= reason
            writeRemoteControlKeepAliveReasonsLocked(reasons)
        }
    }

    fun clearRemoteControlKeepAliveReasons() {
        synchronized(keepAliveLock) {
            writeRemoteControlKeepAliveReasonsLocked(emptySet())
        }
    }

    fun getRemoteControlKeepAliveReasons(): Set<String> {
        return synchronized(keepAliveLock) {
            getRemoteControlKeepAliveReasonsLocked()
        }
    }

    private fun getRemoteControlKeepAliveReasonsLocked(): Set<String> {
        val reasons =
            preferences.getStringSet(KEY_REMOTE_CONTROL_KEEP_ALIVE_REASONS, null)?.toMutableSet()
                ?: mutableSetOf()
        if (preferences.getBoolean(KEY_REMOTE_CONTROL_KEEP_ALIVE, false)) {
            reasons += REMOTE_CONTROL_KEEP_ALIVE_REASON_LEGACY
        }
        return reasons
    }

    private fun writeRemoteControlKeepAliveReasonsLocked(reasons: Set<String>) {
        preferences.edit()
            .putBoolean(KEY_REMOTE_CONTROL_KEEP_ALIVE, reasons.isNotEmpty())
            .putStringSet(KEY_REMOTE_CONTROL_KEEP_ALIVE_REASONS, reasons.toSet())
            .apply()
    }

    fun getDefault(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun init(context: Context) {
        inr = getDefault(context)
    }
}

object PrefKey {
    const val KEY_STABLE_MODE = "key_stable_mode"
    const val KEY_GESTURE_OBSERVING = "key_gesture_observing"
    const val KEY_AUTO_BACKUP = "key_auto_backup"
    const val KEY_FOREGROUND_SERVICE = "key_foreground_service"
    const val KEY_REMOTE_CONTROL_KEEP_ALIVE = "key_remote_control_keep_alive"
    const val KEY_REMOTE_CONTROL_KEEP_ALIVE_REASONS = "key_remote_control_keep_alive_reasons"
    const val KEY_USB_DEBUG = "key_usb_debug"
    const val KEY_USE_VOLUME_CONTROL_RECORD = "key_use_volume_control_record"
    const val KEY_LANGUAGE = "key_language"
}

const val REMOTE_CONTROL_KEEP_ALIVE_REASON_LEGACY = "legacy"
