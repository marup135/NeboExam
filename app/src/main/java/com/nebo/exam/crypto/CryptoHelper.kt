package com.nebo.exam.crypto

import android.util.Base64
import android.util.Log
import com.nebo.exam.utils.Constants
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoHelper {
    private const val TAG = "NeboCrypto"

    fun decryptPayload(rawInput: String): Pair<Boolean, String> {
        return try {
            val rawPayload = rawInput.trim().replace("\r", "").replace("\n", "")

            if (!rawPayload.startsWith(Constants.QR_PREFIX)) {
                return Pair(false, "Bukan QR resmi NeboExam!")
            }

            val clean = rawPayload.removePrefix(Constants.QR_PREFIX)
            val parts = clean.split("::")
            if (parts.size != 2) {
                return Pair(false, "Format QR tidak valid!")
            }

            val ivBytes = Base64.decode(parts[0].trim(), Base64.DEFAULT)
            val cipherBytes = Base64.decode(parts[1].trim(), Base64.DEFAULT)

            val keyBytes = Constants.SECRET_KEY.toByteArray(StandardCharsets.UTF_8)
            val keySpec = SecretKeySpec(keyBytes, "AES")
            val ivSpec = IvParameterSpec(ivBytes)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)

            val decryptedBytes = cipher.doFinal(cipherBytes)
            val url = String(decryptedBytes, StandardCharsets.UTF_8).trim()

            if (url.contains("docs.google.com/forms") || url.contains("forms.gle")) {
                Pair(true, url)
            } else {
                Pair(false, "Tautan bukan Google Form resmi!")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal dekripsi: ${e.message}", e)
            Pair(false, "Gagal dekripsi: ${e.localizedMessage}")
        }
    }
}