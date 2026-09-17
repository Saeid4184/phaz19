package com.example.autoclickerpro

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.autoclickerpro.databinding.ActivityEventLogBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * فاز ۱۹ — نمایش/اشتراک‌گذاری/پاک‌کردنِ لاگِ فنیِ کاملِ اپ (EventLogger).
 * برخلاف RunLogActivity (که فقط اجرای قوانین رو نشون می‌ده)، این‌جا کل
 * رویدادهای فنی — از جمله خطاها و کرش‌های گرفته‌نشده — دیده می‌شه؛
 * دقیقاً برای مواقعی که یه چیزی (مثل ریست‌شدنِ گوشی) بعد از یه اجرای
 * خاص اتفاق افتاده و لازمه دقیقاً قبلش چی ثبت شده رو دید.
 */
class EventLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventLogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.btnRefreshLog.setOnClickListener { refresh() }

        binding.btnShareLog.setOnClickListener {
            val content = EventLogger.readAll(this)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "گزارش رویدادهای AutoClicker Pro")
                putExtra(Intent.EXTRA_TEXT, content)
            }
            startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاریِ گزارش"))
        }

        binding.btnClearEventLog.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("پاک کردن گزارش")
                .setMessage("کل گزارشِ رویدادها حذف بشه؟")
                .setPositiveButton("پاک کن") { _, _ ->
                    EventLogger.clear(this)
                    refresh()
                }
                .setNegativeButton("انصراف", null)
                .show()
        }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        binding.tvEventLog.text = EventLogger.readAll(this)
    }
}
