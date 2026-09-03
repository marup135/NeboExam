package com.nebo.exam.ui

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.nebo.exam.databinding.ActivityExamBinding
import com.nebo.exam.security.LockManager
import com.nebo.exam.utils.Constants
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExamActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExamBinding
    private var isExamSubmitted = false
    private var initialFocusSkipped = false

    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            binding.tvClock.text = currentTime
            clockHandler.postDelayed(this, 1000)
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    val batteryPct = (level * 100) / scale
                    binding.tvBattery.text = "Baterai: $batteryPct%"
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        binding = ActivityExamBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Lock task mode for kiosk
        try {
            startLockTask()
        } catch (_: Exception) {
            // Lock task may fail if not device owner/pin pinned, continue safely
        }

        // Disable back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing to block back button during exam
            }
        })

        setupClockAndBattery()
        setupWebView()

        val examUrl = intent.getStringExtra(ScanActivity.EXTRA_EXAM_URL)
            ?: "https://docs.google.com/forms/"
        binding.webView.loadUrl(examUrl)
    }

    private fun setupClockAndBattery() {
        clockHandler.post(clockRunnable)
        registerReceiver(
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false

            setOnLongClickListener { true }
            isHapticFeedbackEnabled = false

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString() ?: return true
                    return if (url.contains(Constants.BASE_FORM_DOMAIN) || url.contains("forms.gle")) {
                        false
                    } else {
                        true
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (url != null && url.contains("/formResponse")) {
                        isExamSubmitted = true
                        try {
                            stopLockTask()
                        } catch (_: Exception) {}
                        showSuccessDialog()
                    }
                }
            }
        }
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Ujian Selesai")
            .setMessage("Jawaban Anda telah berhasil dikirim. Terima kasih!")
            .setPositiveButton("Selesai") { _, _ ->
                val intent = Intent(this, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!initialFocusSkipped) {
            initialFocusSkipped = true
            return
        }

        if (!hasFocus && !isExamSubmitted) {
            LockManager.triggerPenalty(this)
            val intent = Intent(this, LockedActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        clockHandler.removeCallbacks(clockRunnable)
        try {
            unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}
    }
}
