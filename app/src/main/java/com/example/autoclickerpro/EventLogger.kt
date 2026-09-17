package com.example.autoclickerpro

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * فاز ۱۹ — سیستمِ لاگِ رویدادها.
 *
 * چرا یک فایلِ جدا، و نه فقط Log.d/Log.e؟ چون Logcat یک بافرِ حافظه‌ایه:
 * با ریست‌شدنِ گوشی (دقیقاً همون مشکلی که قبلاً داشتیم) یا حتی فقط با
 * گذشتِ زمان/شلوغیِ لاگِ کل سیستم، کاملاً از بین می‌ره. این کلاس هر
 * رویدادِ مهم رو (شروع/توقفِ سرویس‌ها، هر کلیک/سوایپِ واقعی، هر خطا، و
 * هر کرشِ گرفته‌نشده — نگاه کن به AutoClickerApp) توی یک فایلِ متنی روی
 * حافظه‌ی داخلیِ خودِ اپ می‌نویسه، خط‌به‌خط و بلافاصله flush‌شده — یعنی
 * حتی اگه بلافاصله بعدش گوشی ریست بشه، تا همون لحظه‌ی آخر روی دیسک
 * مونده و قابلِ خوندنه.
 *
 * نوشتن‌ها روی یک HandlerThread جداگانه انجام می‌شن تا هیچ‌وقت
 * ترد اصلی (UI) یا تردِ AccessibilityService رو کند نکنن.
 */
object EventLogger {

    private const val MAX_FILE_BYTES = 1_000_000L // ~۱ مگابایت برای فایلِ جاری
    private const val LOG_FILE_NAME = "event_log.txt"
    private const val LOG_FILE_OLD_NAME = "event_log_old.txt"

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    private val thread = HandlerThread("EventLoggerThread").apply { start() }
    private val handler = Handler(thread.looper)

    private fun logFile(context: Context) = File(context.filesDir, LOG_FILE_NAME)
    private fun oldLogFile(context: Context) = File(context.filesDir, LOG_FILE_OLD_NAME)

    /** ثبتِ یک رویدادِ عادی (نه خطا). */
    fun log(context: Context, tag: String, message: String) {
        writeLine(context, "INFO", tag, message, null)
    }

    /** ثبتِ یک خطا/استثنا، همراه با کل stack trace‌ش. */
    fun logError(context: Context, tag: String, message: String, throwable: Throwable? = null) {
        writeLine(context, "ERROR", tag, message, throwable)
    }

    private fun writeLine(context: Context, level: String, tag: String, message: String, throwable: Throwable?) {
        // هم توی Logcat بنویسیم (برای دیباگِ زنده‌ی حین توسعه با adb logcat)
        if (throwable != null) Log.e(tag, message, throwable) else Log.d(tag, message)

        val appContext = context.applicationContext
        val timestamp = dateFormat.format(Date())
        val line = buildString {
            append('[').append(timestamp).append("] [").append(level).append("] [").append(tag).append("] ")
            append(message)
            if (throwable != null) {
                append(" — ").append(throwable.javaClass.simpleName).append(": ").append(throwable.message)
                append('\n').append(Log.getStackTraceString(throwable))
            }
        }

        handler.post {
            runCatching {
                val file = logFile(appContext)
                if (file.exists() && file.length() > MAX_FILE_BYTES) {
                    val old = oldLogFile(appContext)
                    if (old.exists()) old.delete()
                    file.renameTo(old)
                }
                file.appendText(line + "\n")
            }
        }
    }

    /**
     * کل لاگِ ذخیره‌شده (فایلِ قدیمیِ چرخیده + فایلِ جاری) رو برای نمایش
     * یا اشتراک‌گذاری برمی‌گردونه. چون این تابع مستقیماً از UI thread صدا
     * زده می‌شه (برای نمایش سریع)، از یک I/O همزمانِ سبک استفاده می‌کنه؛
     * برای فایل‌هایی با سقفِ ~۲ مگابایت این قابلِ قبوله.
     */
    fun readAll(context: Context): String {
        val appContext = context.applicationContext
        val old = oldLogFile(appContext)
        val current = logFile(appContext)
        val sb = StringBuilder()
        if (old.exists()) sb.append(runCatching { old.readText() }.getOrDefault(""))
        if (current.exists()) sb.append(runCatching { current.readText() }.getOrDefault(""))
        return sb.toString().ifBlank { "هنوز هیچ رویدادی ثبت نشده." }
    }

    /** پاک‌کردنِ کاملِ لاگ (برای شروعِ یک ثبتِ تازه قبل از تستِ یک سناریوی خاص). */
    fun clear(context: Context) {
        val appContext = context.applicationContext
        handler.post {
            runCatching { logFile(appContext).delete() }
            runCatching { oldLogFile(appContext).delete() }
        }
    }
}
