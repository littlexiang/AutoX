package com.stardust.autojs

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.github.aiselp.autox.api.TermuxApi
import com.stardust.app.service.AbstractAutoService
import com.stardust.autojs.core.pref.Pref
import com.stardust.autojs.servicecomponents.ScriptBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

class IndependentScriptService : AbstractAutoService() {
    val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    @Volatile
    private var stoppedExplicitly = false

    override fun onCreate() {
        super.onCreate()
        stoppedExplicitly = false
        Log.i(TAG, "onCreate")
        Log.i(TAG, "Pid: ${Process.myPid()}")
        if (Pref.shouldKeepScriptProcessAlive) {
            startForeground()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }

    private fun startForeground() {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            }
        )
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val name: CharSequence = "AutoJS Service"
        val description = "script foreground service"
        val channel = NotificationChannel(
            CHANEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = description
        channel.enableLights(false)
        manager.createNotificationChannel(channel)

        // 设置启动意图
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        val contentIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANEL_ID)
            .setContentTitle(getString(R.string.foreground_notification_title))
            .setContentText(getString(R.string.foreground_notification_text))
            .setSmallIcon(R.drawable.autojs_logo)
            .setWhen(System.currentTimeMillis())
            .setContentIntent(contentIntent)
            .setChannelId(CHANEL_ID)
            .setVibrate(LongArray(0))
            .setOngoing(true)

        return builder.build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            TermuxApi.onHandleIntent(intent)
        }
        val action = intent?.action
        when (action) {
            ACTION_START_FOREGROUND -> {
                stoppedExplicitly = false
                startForeground()
            }

            ACTION_ENABLE_REMOTE_CONTROL_KEEP_ALIVE -> {
                stoppedExplicitly = false
                Pref.setRemoteControlKeepAliveEnabled(true)
                startForeground()
            }

            ACTION_STOP_FOREGROUND -> {
                if (Pref.shouldKeepScriptProcessAlive) {
                    stoppedExplicitly = false
                    startForeground()
                } else {
                    stoppedExplicitly = true
                    stopServiceInternal()
                    return START_NOT_STICKY
                }
            }

            ACTION_DISABLE_REMOTE_CONTROL_KEEP_ALIVE -> {
                Pref.setRemoteControlKeepAliveEnabled(false)
                if (Pref.shouldKeepScriptProcessAlive) {
                    stoppedExplicitly = false
                    startForeground()
                } else {
                    stoppedExplicitly = true
                    stopServiceInternal()
                    return START_NOT_STICKY
                }
            }
        }
        return if (Pref.shouldKeepScriptProcessAlive) START_STICKY else START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (Pref.shouldKeepScriptProcessAlive && !stoppedExplicitly) {
            Log.w(TAG, "Task removed, restarting keep-alive script service")
            startForeground(applicationContext)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        Log.i(TAG, "IndependentScriptService Service destroyed")
        super.onDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
    }

    override fun onBind(intent: Intent?): IBinder {
        return ScriptBinder(this, scope)
    }

    companion object {
        private const val TAG = "ScriptService"
        private const val NOTIFICATION_ID = 25

        private val CHANEL_ID = IndependentScriptService::class.java.name + "_foreground"
        const val ACTION_START_FOREGROUND = "action_start_foreground"
        const val ACTION_STOP_FOREGROUND = "action_stop_foreground"
        const val ACTION_ENABLE_REMOTE_CONTROL_KEEP_ALIVE =
            "action_enable_remote_control_keep_alive"
        const val ACTION_DISABLE_REMOTE_CONTROL_KEEP_ALIVE =
            "action_disable_remote_control_keep_alive"

        fun startForeground(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, IndependentScriptService::class.java).apply {
                    action = ACTION_START_FOREGROUND
                }
            )
        }

        fun stopForeground(context: Context) {
            ContextCompat.startForegroundService(Intent(context, IndependentScriptService::class.java).apply {
                action = ACTION_STOP_FOREGROUND
            })
        }

        fun enableRemoteControlKeepAlive(context: Context) {
            Pref.setRemoteControlKeepAliveEnabled(true)
            ContextCompat.startForegroundService(
                context,
                Intent(context, IndependentScriptService::class.java).apply {
                    action = ACTION_ENABLE_REMOTE_CONTROL_KEEP_ALIVE
                }
            )
        }

        fun disableRemoteControlKeepAlive(context: Context) {
            Pref.setRemoteControlKeepAliveEnabled(false)
            ContextCompat.startForegroundService(
                context,
                Intent(context, IndependentScriptService::class.java).apply {
                    action = ACTION_DISABLE_REMOTE_CONTROL_KEEP_ALIVE
                }
            )
        }
    }
}
