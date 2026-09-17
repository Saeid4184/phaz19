package com.example.autoclickerpro

import android.app.Application

/**
 * فاز ۱۹: با ثبتِ یک UncaughtExceptionHandler سراسری، هر کرشِ گرفته‌نشده
 * (توی هر Thread، هر Service، هر Activity) قبل از اینکه پروسه بمیره،
 * با کل stack trace‌ش توی EventLogger ثبت می‌شه. این دقیقاً همون چیزیه
 * که برای فهمیدنِ «چرا بعد از اجرای یک سناریو گوشی ریست شد» لازمه —
 * چون Logcat با ریست از بین می‌ره، ولی این لاگ روی دیسک می‌مونه.
 *
 * بعد از ثبت، handler پیش‌فرضِ سیستم رو صدا می‌زنیم (defaultHandler) تا
 * رفتارِ عادیِ اندروید (بستنِ اپ، دیالوگِ ANR/کرش و غیره) دقیقاً مثل قبل
 * بمونه — فقط قبلش لاگ می‌گیریم.
 */
class AutoClickerApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                EventLogger.logError(
                    this,
                    "CRASH",
                    "کرشِ گرفته‌نشده در ترد «${thread.name}» — پروسه به‌زودی بسته می‌شه",
                    throwable
                )
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        EventLogger.log(this, "App", "AutoClicker Pro استارت شد")
    }
}
