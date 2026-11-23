package com.pulse.player.data

import android.content.Context
import android.os.Build
import android.util.Log
import com.pulse.player.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CrashRecord(
    val signature: String,
    val exceptionClass: String,
    val message: String,
    val threadName: String,
    val stackTrace: String,
    val versionName: String,
    val versionCode: Int,
    val deviceInfo: String,
    val firstSeenMs: Long,
    val lastSeenMs: Long,
    val count: Int = 1,
    val unacknowledged: Boolean = true
)

/**
 * Persistent crash & diagnostic log store.
 * Installs an uncaught exception handler that records deduplicated crash signatures,
 * occurrence counts, device/app version metadata, and full stack traces to internal storage
 * so users and developers can inspect recurring crashes on next launch.
 */
object CrashLogStore {
    private const val CRASH_FILE = "pulse_crash_logs.json"
    private const val ERROR_FILE = "pulse_error_logs.json"
    private const val MAX_CRASH_RECORDS = 30
    private const val MAX_ERROR_RECORDS = 80

    @Volatile
    private var appFilesDir: File? = null
    @Volatile
    private var installed = false
    private val lock = Any()

    private val _crashes = MutableStateFlow<List<CrashRecord>>(emptyList())
    val crashes = _crashes.asStateFlow()

    private val _pendingRecoveryCrash = MutableStateFlow<CrashRecord?>(null)
    val pendingRecoveryCrash = _pendingRecoveryCrash.asStateFlow()

    fun install(context: Context) {
        synchronized(lock) {
            appFilesDir = context.applicationContext.filesDir
            loadCrashesLocked()
            loadPersistedErrorsLocked()
            if (installed) return
            installed = true

            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    recordUncaughtException(
                        threadName = thread.name.ifBlank { "unknown" },
                        throwable = throwable,
                        filesDir = appFilesDir
                    )
                } catch (ignored: Throwable) {
                    Log.e("CrashLogStore", "Failed to persist uncaught crash", ignored)
                } finally {
                    previous?.uncaughtException(thread, throwable)
                }
            }
        }
    }

    fun recordUncaughtException(
        threadName: String,
        throwable: Throwable,
        filesDir: File? = appFilesDir,
        nowMs: Long = System.currentTimeMillis()
    ): CrashRecord {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stack = sw.toString().take(24_000)
        val record = buildCrashRecord(
            exceptionClass = throwable::class.java.name,
            message = throwable.message ?: throwable.localizedMessage ?: "Uncaught exception",
            threadName = threadName,
            stackTrace = stack,
            nowMs = nowMs
        )
        synchronized(lock) {
            val targetDir = filesDir ?: appFilesDir
            val existing = if (targetDir != null) readCrashesFromFile(File(targetDir, CRASH_FILE)) else _crashes.value
            val merged = mergeCrashRecord(existing, record)
            _crashes.value = merged
            _pendingRecoveryCrash.value = merged.firstOrNull { it.unacknowledged }
            if (targetDir != null) {
                writeCrashesToFile(File(targetDir, CRASH_FILE), merged)
            }
        }
        return record
    }

    fun buildCrashRecord(
        exceptionClass: String,
        message: String,
        threadName: String,
        stackTrace: String,
        nowMs: Long = System.currentTimeMillis(),
        versionName: String = BuildConfig.VERSION_NAME,
        versionCode: Int = BuildConfig.VERSION_CODE,
        deviceInfo: String = "${Build.MANUFACTURER} ${Build.MODEL} (SDK ${Build.VERSION.SDK_INT})"
    ): CrashRecord {
        val signature = computeSignature(exceptionClass, stackTrace)
        return CrashRecord(
            signature = signature,
            exceptionClass = exceptionClass,
            message = message.take(500),
            threadName = threadName,
            stackTrace = stackTrace,
            versionName = versionName,
            versionCode = versionCode,
            deviceInfo = deviceInfo,
            firstSeenMs = nowMs,
            lastSeenMs = nowMs,
            count = 1,
            unacknowledged = true
        )
    }

    fun computeSignature(exceptionClass: String, stackTrace: String): String {
        val lines = stackTrace.lineSequence().map { it.trim() }.toList()
        val appFrame = lines.firstOrNull { it.startsWith("at com.pulse.player") }
            ?: lines.firstOrNull { it.startsWith("at ") }
            ?: "unknown_frame"
        return "$exceptionClass|$appFrame"
    }

    fun mergeCrashRecord(existing: List<CrashRecord>, incoming: CrashRecord): List<CrashRecord> {
        val list = existing.toMutableList()
        val matchIdx = list.indexOfFirst { it.signature == incoming.signature }
        if (matchIdx >= 0) {
            val prev = list.removeAt(matchIdx)
            list.add(
                0,
                incoming.copy(
                    firstSeenMs = minOf(prev.firstSeenMs, incoming.firstSeenMs),
                    lastSeenMs = maxOf(prev.lastSeenMs, incoming.lastSeenMs),
                    count = prev.count + incoming.count,
                    unacknowledged = true
                )
            )
        } else {
            list.add(0, incoming)
        }
        return if (list.size > MAX_CRASH_RECORDS) list.take(MAX_CRASH_RECORDS) else list
    }

    fun acknowledgeRecovery() {
        synchronized(lock) {
            val updated = _crashes.value.map { it.copy(unacknowledged = false) }
            _crashes.value = updated
            _pendingRecoveryCrash.value = null
            appFilesDir?.let { writeCrashesToFile(File(it, CRASH_FILE), updated) }
        }
    }

    fun clearCrashes() {
        synchronized(lock) {
            _crashes.value = emptyList()
            _pendingRecoveryCrash.value = null
            appFilesDir?.let { File(it, CRASH_FILE).delete() }
        }
    }

    fun persistErrors(errors: List<AppError>) {
        val dir = appFilesDir ?: return
        synchronized(lock) {
            runCatching {
                val arr = JSONArray()
                errors.take(MAX_ERROR_RECORDS).forEach { e ->
                    arr.put(
                        JSONObject().apply {
                            put("id", e.id)
                            put("timestampMs", e.timestampMs)
                            put("category", e.category.name)
                            put("title", e.title)
                            put("message", e.message)
                            put("technicalDetails", e.technicalDetails ?: JSONObject.NULL)
                            put("actionable", e.actionable)
                        }
                    )
                }
                File(dir, ERROR_FILE).writeText(arr.toString(), Charsets.UTF_8)
            }
        }
    }

    fun clearPersistedErrors() {
        synchronized(lock) {
            appFilesDir?.let { File(it, ERROR_FILE).delete() }
        }
    }

    fun formatCrashReport(crash: CrashRecord): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US)
        return buildString {
            appendLine("=== Pulse Music Player Crash Report ===")
            appendLine("App Version: ${crash.versionName} (${crash.versionCode})")
            appendLine("Device: ${crash.deviceInfo}")
            appendLine("Occurrences: ${crash.count}")
            appendLine("First Seen: ${fmt.format(Date(crash.firstSeenMs))}")
            appendLine("Last Seen: ${fmt.format(Date(crash.lastSeenMs))}")
            appendLine("Thread: ${crash.threadName}")
            appendLine("Exception: ${crash.exceptionClass}")
            appendLine("Message: ${crash.message}")
            appendLine()
            appendLine(crash.stackTrace)
        }
    }

    private fun loadCrashesLocked() {
        val dir = appFilesDir ?: return
        val loaded = readCrashesFromFile(File(dir, CRASH_FILE))
        _crashes.value = loaded
        _pendingRecoveryCrash.value = loaded.firstOrNull { it.unacknowledged }
    }

    private fun loadPersistedErrorsLocked() {
        val dir = appFilesDir ?: return
        val file = File(dir, ERROR_FILE)
        if (!file.exists()) return
        runCatching {
            val arr = JSONArray(file.readText(Charsets.UTF_8))
            val out = ArrayList<AppError>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val category = runCatching {
                    ErrorCategory.valueOf(o.optString("category", ErrorCategory.GENERAL.name))
                }.getOrDefault(ErrorCategory.GENERAL)
                out += AppError(
                    id = o.optString("id"),
                    timestampMs = o.optLong("timestampMs"),
                    category = category,
                    title = o.optString("title"),
                    message = o.optString("message"),
                    technicalDetails = o.optString("technicalDetails").takeIf { it.isNotBlank() && it != "null" },
                    actionable = o.optBoolean("actionable", true)
                )
            }
            if (out.isNotEmpty()) {
                ErrorDispatcher.hydratePersisted(out)
            }
        }
    }

    private fun readCrashesFromFile(file: File): List<CrashRecord> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText(Charsets.UTF_8))
            val list = ArrayList<CrashRecord>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                list += CrashRecord(
                    signature = o.optString("signature"),
                    exceptionClass = o.optString("exceptionClass"),
                    message = o.optString("message"),
                    threadName = o.optString("threadName", "main"),
                    stackTrace = o.optString("stackTrace"),
                    versionName = o.optString("versionName", BuildConfig.VERSION_NAME),
                    versionCode = o.optInt("versionCode", BuildConfig.VERSION_CODE),
                    deviceInfo = o.optString("deviceInfo", "Android"),
                    firstSeenMs = o.optLong("firstSeenMs"),
                    lastSeenMs = o.optLong("lastSeenMs"),
                    count = o.optInt("count", 1).coerceAtLeast(1),
                    unacknowledged = o.optBoolean("unacknowledged", false)
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    private fun writeCrashesToFile(file: File, records: List<CrashRecord>) {
        runCatching {
            val arr = JSONArray()
            records.take(MAX_CRASH_RECORDS).forEach { c ->
                arr.put(
                    JSONObject().apply {
                        put("signature", c.signature)
                        put("exceptionClass", c.exceptionClass)
                        put("message", c.message)
                        put("threadName", c.threadName)
                        put("stackTrace", c.stackTrace)
                        put("versionName", c.versionName)
                        put("versionCode", c.versionCode)
                        put("deviceInfo", c.deviceInfo)
                        put("firstSeenMs", c.firstSeenMs)
                        put("lastSeenMs", c.lastSeenMs)
                        put("count", c.count)
                        put("unacknowledged", c.unacknowledged)
                    }
                )
            }
            file.writeText(arr.toString(), Charsets.UTF_8)
        }
    }
}
