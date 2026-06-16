package org.autojs.autojs.devplugin

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.stardust.app.service.AbstractAutoService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.autojs.autojs.Pref
import org.autojs.autojs.ui.main.MainActivity
import org.autojs.autoxjs.R

class UsbDebugService : AbstractAutoService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var stoppedByUser = false

    override fun onCreate() {
        super.onCreate()
        stoppedByUser = false
        startForegroundInternal()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stoppedByUser = true
                stopServiceInternal()
                return START_NOT_STICKY
            }

            ACTION_START, null -> {
                if (!Pref.isUsbDebugEnabled()) {
                    stoppedByUser = true
                    stopServiceInternal()
                    return START_NOT_STICKY
                }
            }
        }
        scope.launch {
            kotlin.runCatching {
                DevPlugin.startUSBDebug()
            }.onFailure {
                Log.e(TAG, "Failed to start USB debug server", it)
                Pref.setUsbDebugEnabled(false)
                stoppedByUser = true
                stopServiceInternal()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (Pref.isUsbDebugEnabled() && !stoppedByUser) {
            Log.w(TAG, "Task removed, scheduling USB debug service restart")
            start(applicationContext)
        }
    }

    override fun onDestroy() {
        runBlocking(Dispatchers.IO) {
            kotlin.runCatching { DevPlugin.stopUSBDebug() }
        }
        scope.cancel()
        if (Pref.isUsbDebugEnabled() && !stoppedByUser) {
            Log.w(TAG, "UsbDebugService destroyed unexpectedly, restarting")
            start(applicationContext)
        }
        super.onDestroy()
    }

    private fun startForegroundInternal() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.usb_debug_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.usb_debug_notification_text)
            enableLights(false)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
        )
    }

    private fun buildNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.usb_debug_notification_title))
            .setContentText(getString(R.string.usb_debug_notification_text))
            .setSmallIcon(R.drawable.autojs_logo)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    companion object {
        private const val TAG = "UsbDebugService"
        private const val ACTION_START = "org.autojs.autojs.devplugin.action.START_USB_DEBUG"
        private const val ACTION_STOP = "org.autojs.autojs.devplugin.action.STOP_USB_DEBUG"
        private const val NOTIFICATION_ID = 26
        private const val CHANNEL_ID = "org.autojs.autojs.devplugin.usb_debug"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, UsbDebugService::class.java).apply {
                    action = ACTION_START
                }
            )
        }

        fun stop(context: Context) {
            context.startService(Intent(context, UsbDebugService::class.java).apply {
                action = ACTION_STOP
            })
        }
    }
}
