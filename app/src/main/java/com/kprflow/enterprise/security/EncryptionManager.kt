package com.kprflow.enterprise.security

import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.kprflow.enterprise.domain.model.EncryptedData
import java.io.File
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptionManager @Inject constructor() {
    
    companion object {
        private const val ALGORITHM = "AES/CBC/PKCS5Padding"
        private const val KEY_ALGORITHM = "AES"
        private const val KEY_SIZE = 256
        private const val IV_SIZE = 16
    }
    
    private val secretKey: SecretKey by lazy {
        generateSecretKey()
    }
    
    private fun generateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM)
        keyGenerator.init(KEY_SIZE)
        return keyGenerator.generateKey()
    }
    
    fun encrypt(data: String): EncryptedData {
        try {
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(IV_SIZE)
            java.security.SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)
            
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val encryptedBytes = cipher.doFinal(data.toByteArray(StandardCharsets.UTF_8))
            
            return EncryptedData(
                encryptedData = encryptedBytes,
                iv = iv,
                algorithm = ALGORITHM
            )
        } catch (e: Exception) {
            throw SecurityException("Encryption failed", e)
        }
    }
    
    fun decrypt(encryptedData: EncryptedData): String {
        try {
            val cipher = Cipher.getInstance(encryptedData.algorithm)
            val ivSpec = IvParameterSpec(encryptedData.iv)
            
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            val decryptedBytes = cipher.doFinal(encryptedData.encryptedData)
            
            return String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            throw SecurityException("Decryption failed", e)
        }
    }
    
    fun encryptFile(inputFile: File, outputFile: File) {
        try {
            val masterKey = MasterKey.Builder(inputFile.context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            val encryptedFile = EncryptedFile.Builder(
                outputFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
            
            encryptedFile.openFileOutput().use { output ->
                inputFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            throw SecurityException("File encryption failed", e)
        }
    }
    
    fun decryptFile(encryptedFile: File, outputFile: File) {
        try {
            val masterKey = MasterKey.Builder(encryptedFile.context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            val encrypted = EncryptedFile.Builder(
                encryptedFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
            
            encrypted.openFileInput().use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            throw SecurityException("File decryption failed", e)
        }
    }
    
    fun hashData(data: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(data.toByteArray(StandardCharsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            throw SecurityException("Hashing failed", e)
        }
    }
    
    fun generateSecureToken(): String {
        return try {
            val random = java.security.SecureRandom()
            val bytes = ByteArray(32)
            random.nextBytes(bytes)
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            throw SecurityException("Token generation failed", e)
        }
    }
}
