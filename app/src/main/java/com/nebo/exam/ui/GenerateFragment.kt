package com.nebo.exam.ui

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.nebo.exam.databinding.FragmentGenerateBinding
import com.nebo.exam.utils.Constants
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class GenerateFragment : Fragment() {

    private var _binding: FragmentGenerateBinding? = null
    private val binding get() = _binding!!

    private var generatedQrBitmap: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGenerateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnGenerate.setOnClickListener {
            generateQrCode()
        }

        binding.btnSaveQr.setOnClickListener {
            saveQrImageToGallery()
        }
    }

    private fun generateQrCode() {
        val title = binding.etTitle.text?.toString()?.trim() ?: ""
        val url = binding.etUrl.text?.toString()?.trim() ?: ""

        if (url.isEmpty()) {
            binding.tilUrl.error = "Masukkan URL Google Form"
            return
        } else {
            binding.tilUrl.error = null
        }

        val isGoogleDocs = url.startsWith("https://docs.google.com/forms/") || url.startsWith("http://docs.google.com/forms/")
        val isShortForms = url.startsWith("https://forms.gle/") || url.startsWith("http://forms.gle/")

        if (!isGoogleDocs && !isShortForms) {
            Toast.makeText(
                requireContext(),
                "URL harus diawali https://docs.google.com/forms/ atau https://forms.gle/",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        try {
            // Enkripsi AES-256-CBC
            val rawKeyBytes = Constants.SECRET_KEY.trim().toByteArray(Charsets.UTF_8)
            val keyBytes = Arrays.copyOf(rawKeyBytes, 32)
            val keySpec = SecretKeySpec(keyBytes, "AES")

            val ivBytes = ByteArray(16)
            SecureRandom().nextBytes(ivBytes)
            val ivSpec = IvParameterSpec(ivBytes)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

            val encryptedBytes = cipher.doFinal(url.toByteArray(Charsets.UTF_8))

            val ivBase64 = Base64.encodeToString(ivBytes, Base64.NO_WRAP)
            val cipherBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)

            val payload = "${Constants.QR_PREFIX}$ivBase64::$cipherBase64"

            // Render QR Code 512x512
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(payload, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }

            generatedQrBitmap = bmp
            binding.ivQrCode.setImageBitmap(bmp)
            binding.ivQrCode.visibility = View.VISIBLE
            binding.btnSaveQr.visibility = View.VISIBLE

            Toast.makeText(requireContext(), "QR Code Berhasil Dibuat!", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Gagal membuat QR: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveQrImageToGallery() {
        val qrBitmap = generatedQrBitmap ?: return
        val titleText = binding.etTitle.text?.toString()?.trim()?.ifEmpty { "Ujian SMKN 1 Bojong" } ?: "Ujian SMKN 1 Bojong"

        // Create composite canvas 800 x 1000 px
        val canvasWidth = 800
        val canvasHeight = 1000
        val combinedBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(combinedBitmap)

        // Solid white background
        canvas.drawColor(Color.WHITE)

        // Header paint
        val headerPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 36f
            isAntiAlias = true
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("SMKN 1 BOJONG - NEBOEXAM", canvasWidth / 2f, 90f, headerPaint)

        // Subheader paint
        val subheaderPaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 22f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Sistem Ujian Terstandar & Aman", canvasWidth / 2f, 135f, subheaderPaint)

        // Subject Title paint
        val titlePaint = Paint().apply {
            color = Color.parseColor("#1E3A8A")
            textSize = 32f
            isAntiAlias = true
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(titleText, canvasWidth / 2f, 210f, titlePaint)

        // Draw QR Code centered (500x500 at Y=250)
        val qrSize = 500
        val qrLeft = (canvasWidth - qrSize) / 2f
        val qrTop = 250f
        val destRect = RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize)
        canvas.drawBitmap(qrBitmap, null, destRect, null)

        // Draw border line around QR code
        val borderPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRect(destRect, borderPaint)

        // Footer note paint
        val footerPaint = Paint().apply {
            color = Color.parseColor("#DC2626")
            textSize = 22f
            isAntiAlias = true
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("* Pindai menggunakan NeboExam resmi", canvasWidth / 2f, 830f, footerPaint)

        // Save composite bitmap to gallery
        val cleanFileName = "${titleText.replace("[^a-zA-Z0-9_-]".toRegex(), "_")}_${System.currentTimeMillis()}.png"

        try {
            val contentResolver = requireContext().contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, cleanFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/NeboExam")
                }
            }

            val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
                contentResolver.openOutputStream(imageUri).use { outStream ->
                    if (outStream != null) {
                        combinedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                    }
                }
                Toast.makeText(requireContext(), "Kartu QR Berhasil Disimpan ke Galeri!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "Gagal menyimpan gambar", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Gagal menyimpan: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
