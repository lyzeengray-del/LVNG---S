package com.lvngx.app

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Shizuku-backed macro dispatcher.
 *
 * Uses the user-authorized Shizuku shell identity to invoke Android's input command.
 * It does not use AccessibilityService. Compatibility is device/ROM/game dependent.
 */
object MacroConfig {
    const val PREFS = "lvngx_macro"
    const val KEY_TARGET_X = "target_x"
    const val KEY_TARGET_Y = "target_y"
    const val KEY_INTERVAL = "interval_ms"
    const val KEY_TRIGGER_SIZE = "trigger_size"
    const val KEY_TRIGGER_ALPHA = "trigger_alpha"
    const val KEY_PROFILE = "active_profile"
}

object MacroShellEngine {
    private val running = AtomicBoolean(false)
    private val activeProcess = AtomicReference<Process?>(null)
    @Volatile var lastError: String? = null
        private set

    fun isRunning(): Boolean = running.get()

    fun isShizukuReady(): Boolean = try {
        Shizuku.pingBinder() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
        false
    }

    /** Starts only after a real hold event and only with Shizuku binder + permission. */
    @Synchronized
    fun start(context: Context): Boolean {
        if (running.get()) return true
        if (!isShizukuReady()) {
            lastError = "Shizuku server atau izin LVNG -X belum siap"
            return false
        }

        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(MacroConfig.PREFS, Context.MODE_PRIVATE)
        val nx = prefs.getFloat(MacroConfig.KEY_TARGET_X, 0.5f).coerceIn(0f, 1f)
        val ny = prefs.getFloat(MacroConfig.KEY_TARGET_Y, 0.5f).coerceIn(0f, 1f)
        val dm = appContext.resources.displayMetrics
        val x = (nx * dm.widthPixels).toInt().coerceIn(0, dm.widthPixels - 1)
        val y = (ny * dm.heightPixels).toInt().coerceIn(0, dm.heightPixels - 1)
        val interval = prefs.getInt(MacroConfig.KEY_INTERVAL, 250).coerceIn(150, 2000)

        lastError = null
        running.set(true)
        Thread({
            try {
                // Ignore accidental taps: macro begins only after a deliberate short hold.
                try { Thread.sleep(180L) } catch (_: InterruptedException) { return@Thread }
                while (running.get()) {
                    if (!isShizukuReady()) {
                        lastError = "Koneksi atau izin Shizuku terputus"
                        break
                    }
                    var process: Process? = null
                    try {
                        process = Shizuku.newProcess(
                            arrayOf("input", "tap", x.toString(), y.toString()),
                            null,
                            null
                        )
                        activeProcess.set(process)
                        val completed = process.waitFor(2, TimeUnit.SECONDS)
                        if (!completed) {
                            process.destroy()
                            lastError = "Perintah input melewati batas waktu"
                            break
                        }
                        if (process.exitValue() != 0) {
                            lastError = "Perintah input ditolak oleh sistem"
                            break
                        }
                    } catch (t: Throwable) {
                        lastError = t.message ?: "Gagal mengirim input melalui Shizuku"
                        break
                    } finally {
                        activeProcess.compareAndSet(process, null)
                        try { process?.inputStream?.close() } catch (_: Throwable) {}
                        try { process?.errorStream?.close() } catch (_: Throwable) {}
                        try { process?.outputStream?.close() } catch (_: Throwable) {}
                    }
                    if (!running.get()) break
                    try { Thread.sleep(interval.toLong()) } catch (_: InterruptedException) { break }
                }
            } finally {
                running.set(false)
                activeProcess.getAndSet(null)?.let { try { it.destroy() } catch (_: Throwable) {} }
            }
        }, "LVNG-X-Shizuku-Macro").apply { isDaemon = true }.start()
        return true
    }

    /** Release, emergency stop, screen-off, binder loss, or service teardown all call this. */
    @Synchronized
    fun stop() {
        running.set(false)
        activeProcess.getAndSet(null)?.let { process ->
            try { process.destroy() } catch (_: Throwable) {}
        }
    }
}
