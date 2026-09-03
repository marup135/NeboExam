package com.nebo.exam.ui

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.nebo.exam.databinding.ActivityLockedBinding
import com.nebo.exam.security.LockManager
import java.util.Locale

class LockedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockedBinding
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        binding = ActivityLockedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Disable back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing to block back button when locked
            }
        })

        binding.btnUnlockSupervisor.setOnClickListener {
            showSupervisorPinDialog()
        }

        startPenaltyTimer()
    }

    private fun startPenaltyTimer() {
        val remainingMs = LockManager.getRemainingPenaltyTimeMs(this)
        if (remainingMs <= 0) {
            unlockAndGoHome()
            return
        }

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(remainingMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val updatedRemaining = LockManager.getRemainingPenaltyTimeMs(this@LockedActivity)
                if (updatedRemaining <= 0) {
                    cancel()
                    unlockAndGoHome()
                    return
                }
                updateTimerText(updatedRemaining)
            }

            override fun onFinish() {
                LockManager.resetPenalty(this@LockedActivity)
                unlockAndGoHome()
            }
        }.start()
    }

    private fun updateTimerText(ms: Long) {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        binding.tvCountdown.text = String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d",
            hours,
            minutes,
            seconds
        )
    }

    private fun showSupervisorPinDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Masukkan PIN Pengawas"
        }

        AlertDialog.Builder(this)
            .setTitle("Buka Kunci Pengawas")
            .setMessage("Masukkan PIN Pengawas untuk membuka kunci perangkat:")
            .setView(input)
            .setPositiveButton("Buka Kunci") { dialog, _ ->
                val pin = input.text.toString()
                if (LockManager.verifySupervisorPin(pin)) {
                    LockManager.resetPenalty(this)
                    dialog.dismiss()
                    unlockAndGoHome()
                } else {
                    Toast.makeText(this, "PIN Salah!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun unlockAndGoHome() {
        countDownTimer?.cancel()
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
