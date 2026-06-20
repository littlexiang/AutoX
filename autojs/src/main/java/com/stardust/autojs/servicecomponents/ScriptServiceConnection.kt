package com.stardust.autojs.servicecomponents

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Bundle
import android.os.Debug
import android.os.IBinder
import android.util.Log
import com.stardust.app.GlobalAppContext
import com.stardust.autojs.IndependentScriptService
import com.stardust.autojs.core.console.ConsoleImpl
import com.stardust.autojs.core.console.LogEntry
import com.stardust.autojs.execution.ExecutionConfig
import com.stardust.util.UiHandler
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class ScriptServiceConnection : ServiceConnection {
    enum class ServiceState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        BINDING_DIED,
        NULL_BINDING
    }

    val binderConsoleListener = BinderConsoleListener.ClientInterface()
    var binding: CompletableJob? = null
    var service: IBinder? = null
    var application: Context? = null
    private val connected = Job()
    private val _serviceState = MutableStateFlow(ServiceState.DISCONNECTED)
    val consoleImpl: ConsoleImpl =
        object : ConsoleImpl(UiHandler(GlobalAppContext.get())), BinderConsoleListener {
            override fun onPrintln(log: LogEntry) {
                println(log.level, log.content)
            }
        }.apply {
            binderConsoleListener.logPublish
                .observeOn(AndroidSchedulers.mainThread()).subscribe(::onPrintln)
        }
    val serviceState = _serviceState.asStateFlow()


    @Volatile
    var isConnected = false
        private set

    val isServiceHealthy: Boolean
        get() = _serviceState.value == ServiceState.CONNECTED

    @OptIn(DelicateCoroutinesApi::class)
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        this.service = service
        isConnected = true
        _serviceState.value = ServiceState.CONNECTED
        binding?.complete()
        connected.complete()
        binderConsoleListener.logPublish.onNext(
            LogEntry(
                level = Log.INFO,
                content = "Script service connected"
            )
        )
        GlobalScope.launch {
            registerGlobalConsoleListener(binderConsoleListener)
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        isConnected = false
        service = null
        binding = null
        _serviceState.value = ServiceState.DISCONNECTED
        binderConsoleListener.logPublish.onNext(
            LogEntry(
                level = Log.ERROR,
                content = "Script service disconnected"
            )
        )
    }

    override fun onBindingDied(name: ComponentName?) {
        isConnected = false
        service = null
        binding = null
        _serviceState.value = ServiceState.BINDING_DIED
        binderConsoleListener.logPublish.onNext(
            LogEntry(
                level = Log.ERROR,
                content = "Script service binding died"
            )
        )
        application?.let(::bind)
    }

    override fun onNullBinding(name: ComponentName?) {
        isConnected = false
        service = null
        binding = null
        _serviceState.value = ServiceState.NULL_BINDING
        binderConsoleListener.logPublish.onNext(
            LogEntry(
                level = Log.ERROR,
                content = "Script service returned null binding"
            )
        )
    }

    private suspend fun <T> sendBinder(n: suspend TanBinder.() -> T): T {
        awaitConnected()
        return ScriptBinder.connect(service!!, n)
    }

    suspend fun getAllScriptTasks(): MutableList<TaskInfo> = sendBinder {
        action = ScriptBinder.Action.GET_ALL_TASKS.id
        send()
        reply!!.readException()
        val bundle = reply.readBundle(ClassLoader.getSystemClassLoader())
        check(bundle != null) { "bundle is null" }
        val size = bundle.getInt("size")
        val tasks = mutableListOf<TaskInfo>()
        for (i in 1..size) {
            tasks.add(TaskInfo.fromBundle(bundle.getBundle((i - 1).toString())!!))
        }
        return@sendBinder tasks
    }

    suspend fun runScript(
        taskInfo: TaskInfo,
        listener: BinderScriptListener? = null,
        config: ExecutionConfig? = null
    ) = sendBinder {
        action = ScriptBinder.Action.RUN_SCRIPT.id
        data.writeBundle(Bundle().apply {
            putBundle(TaskInfo.TAG, taskInfo.toBundle())
            if (config != null) {
                putString(ExecutionConfig.tag, ExecutionConfig.toJson(config))
            }
            if (listener != null) {
                putBinder(BinderScriptListener.TAG, listener.toBinder())
            }
        })
        send()
    }

    suspend fun getMemoryInfo(): Debug.MemoryInfo = sendBinder {
        action = ScriptBinder.Action.GET_MEMORY_INFO.id
        send()
        reply!!.readException()
        val memoryInfo = Debug.MemoryInfo.CREATOR.createFromParcel(reply)
        return@sendBinder memoryInfo
    }

    suspend fun stopAllScript() = sendBinder {
        action = ScriptBinder.Action.STOP_ALL_SCRIPT.id
        send()
    }

    suspend fun stopScript(id: Int) = sendBinder {
        action = ScriptBinder.Action.STOP_SCRIPT.id
        data.writeInt(id)
        send()
    }

    suspend fun appExit() = sendBinder {
        action = ScriptBinder.Action.APP_EXIT.id
        send()
    }

    suspend fun registerGlobalScriptListener(listener: BinderScriptListener) = sendBinder {
        action = ScriptBinder.Action.REGISTER_GLOBAL_SCRIPT_LISTENER.id
        data.writeStrongBinder(listener.toBinder())
        send()
    }

    suspend fun registerGlobalConsoleListener(listener: Binder) = sendBinder {
        action = ScriptBinder.Action.REGISTER_GLOBAL_CONSOLE_LISTENER.id
        data.writeStrongBinder(listener)
        send()
    }

    suspend fun notificationListenerServiceStatus(): Boolean = sendBinder {
        action = ScriptBinder.Action.NOTIFICATION_LISTENER_SERVICE_STATUS.id
        send()
        reply!!.readException()
        reply.readInt() == 1
    }

    suspend fun bindShizukuUserService() = sendBinder {
        action = ScriptBinder.Action.BIND_SHIZUKU_SERVICE.id
        send()
    }

    suspend fun sendAck(seq: Int, event: String, taskNo: String) = sendBinder {
        action = ScriptBinder.Action.PUT_ACK.id
        data.writeInt(seq)
        data.writeString(event)
        data.writeString(taskNo)
        send()
    }

    suspend fun awaitConnected() = withTimeout(3000) {
        if (isConnected) return@withTimeout
        if (binding == null) {
            if (application != null) {
                bind(application!!)
            } else {
                throw IllegalStateException("ScriptServiceConnection not bind")
            }
        }
        Log.d(TAG, "awaitConnected")
        binding!!.join()
    }

    fun bind(context: Context) {
        if (isConnected || binding != null) return
        val appContext = context.applicationContext
        application = appContext
        binding = Job()
        _serviceState.value = ServiceState.CONNECTING
        val serviceIntent = Intent(appContext, IndependentScriptService::class.java)

        // Some Android 14/15 devices are more reliable if the remote-process service is
        // explicitly started before we bind to it.
        IndependentScriptService.ensureStarted(appContext)

        var isBound = appContext.bindService(
            serviceIntent,
            this,
            Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT
        )
        if (!isBound) {
            Log.w(TAG, "Initial bind failed, retrying IndependentScriptService bind")
            IndependentScriptService.ensureStarted(appContext)
            isBound = appContext.bindService(
                serviceIntent,
                this,
                Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT
            )
        }
        if (!isBound) {
            binding?.cancel()
            binding = null
            _serviceState.value = ServiceState.DISCONNECTED
            throw IllegalStateException(
                "Failed to bind IndependentScriptService for package=${appContext.packageName}"
            )
        }
    }

    fun unbind(context: Context) {
        if (!isConnected && binding == null) return
        try {
            context.applicationContext.unbindService(this)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Failed to unbind service: service not bound", e)
        }
        binding = null
        isConnected = false
        service = null
        _serviceState.value = ServiceState.DISCONNECTED
    }

    companion object {
        private const val TAG = "ScriptServiceConnection"
        val GlobalConnection by lazy { ScriptServiceConnection() }
    }
}
