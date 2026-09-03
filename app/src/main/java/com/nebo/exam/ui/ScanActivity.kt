package com.nebo.exam.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.nebo.exam.crypto.CryptoHelper
import com.nebo.exam.databinding.ActivityScanBinding

class ScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanBinding
    private var isProcessingScan = false

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 101
        const val EXTRA_EXAM_URL = "EXAM_URL"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            startScanning()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning()
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Izin Kamera Diperlukan")
                    .setMessage("Aplikasi membutuhkan akses kamera untuk memindai QR Code Ujian.")
                    .setPositiveButton("Coba Lagi") { _, _ -> checkCameraPermission() }
                    .setNegativeButton("Keluar") { _, _ -> finish() }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    private fun startScanning() {
        binding.barcodeScanner.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                if (isProcessingScan) return
                val scannedText = result?.text ?: ""
                if (scannedText.isEmpty()) return

                isProcessingScan = true
                binding.barcodeScanner.pause()

                val (isSuccess, resultUrlOrError) = CryptoHelper.decryptPayload(scannedText)

                if (isSuccess) {
                    val intent = Intent(this@ScanActivity, ExamActivity::class.java).apply {
                        putExtra("EXAM_URL", resultUrlOrError)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    AlertDialog.Builder(this@ScanActivity)
                        .setTitle("Pindai Gagal")
                        .setMessage(resultUrlOrError)
                        .setPositiveButton("Coba Lagi") { dialog, _ ->
                            dialog.dismiss()
                            isProcessingScan = false
                            binding.barcodeScanner.resume()
                        }
                        .setCancelable(false)
                        .show()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            binding.barcodeScanner.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeScanner.pause()
    }
}
