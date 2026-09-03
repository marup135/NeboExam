package com.nebo.exam

import com.nebo.exam.crypto.CryptoHelper
import com.nebo.exam.utils.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class CryptoTest {

    @Test
    fun testAesDecryptionFromJava() {
        val secretKey = Constants.SECRET_KEY
        val testUrl = "https://docs.google.com/forms/d/e/1FAIpQLSc_test/viewform"

        val ivBytes = ByteArray(16) { (it + 1).toByte() }
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val keySpec = SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "AES")
        val ivSpec = IvParameterSpec(ivBytes)

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        val encryptedBytes = cipher.doFinal(testUrl.toByteArray(Charsets.UTF_8))

        val ivBase64 = Base64.getEncoder().encodeToString(ivBytes)
        val cipherBase64 = Base64.getEncoder().encodeToString(encryptedBytes)

        val payload = "NEBOSECURE::$ivBase64::$cipherBase64"
        println("Generated test payload: $payload")

        val (isSuccess, resultUrl) = CryptoHelper.decryptPayload(payload)
        println("Decrypted result: $resultUrl")

        assertTrue("Decryption should succeed", isSuccess)
        assertEquals(testUrl, resultUrl)
    }
}
