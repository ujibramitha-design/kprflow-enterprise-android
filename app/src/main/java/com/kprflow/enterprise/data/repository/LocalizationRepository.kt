package com.kprflow.enterprise.data.repository

import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalizationRepository @Inject constructor(
    private val postgrest: Postgrest
) {
    
    companion object {
        // Supported languages
        const val LANGUAGE_INDONESIAN = "id"
        const val LANGUAGE_ENGLISH = "en"
        
        // Default language
        const val DEFAULT_LANGUAGE = LANGUAGE_INDONESIAN
        
        // Translation categories
        const val CATEGORY_COMMON = "common"
        const val CATEGORY_DASHBOARD = "dashboard"
        const val CATEGORY_DOSSIER = "dossier"
        const val CATEGORY_DOCUMENT = "document"
        const val CATEGORY_PAYMENT = "payment"
        const val CATEGORY_NOTIFICATION = "notification"
        const val CATEGORY_ERROR = "error"
        const val CATEGORY_VALIDATION = "validation"
    }
    
    private val supportedLanguages = listOf(LANGUAGE_INDONESIAN, LANGUAGE_ENGLISH)
    
    private val defaultTranslations = mapOf(
        // Common translations
        "app_name" to mapOf(
            LANGUAGE_ENGLISH to "KPRFlow Enterprise",
            LANGUAGE_INDONESIAN to "KPRFlow Enterprise"
        ),
        "welcome" to mapOf(
            LANGUAGE_ENGLISH to "Welcome",
            LANGUAGE_INDONESIAN to "Selamat Datang"
        ),
        "login" to mapOf(
            LANGUAGE_ENGLISH to "Login",
            LANGUAGE_INDONESIAN to "Masuk"
        ),
        "logout" to mapOf(
            LANGUAGE_ENGLISH to "Logout",
            LANGUAGE_INDONESIAN to "Keluar"
        ),
        "dashboard" to mapOf(
            LANGUAGE_ENGLISH to "Dashboard",
            LANGUAGE_INDONESIAN to "Dasbor"
        ),
        "profile" to mapOf(
            LANGUAGE_ENGLISH to "Profile",
            LANGUAGE_INDONESIAN to "Profil"
        ),
        "settings" to mapOf(
            LANGUAGE_ENGLISH to "Settings",
            LANGUAGE_INDONESIAN to "Pengaturan"
        ),
        "save" to mapOf(
            LANGUAGE_ENGLISH to "Save",
            LANGUAGE_INDONESIAN to "Simpan"
        ),
        "cancel" to mapOf(
            LANGUAGE_ENGLISH to "Cancel",
            LANGUAGE_INDONESIAN to "Batal"
        ),
        "delete" to mapOf(
            LANGUAGE_ENGLISH to "Delete",
            LANGUAGE_INDONESIAN to "Hapus"
        ),
        "edit" to mapOf(
            LANGUAGE_ENGLISH to "Edit",
            LANGUAGE_INDONESIAN to "Edit"
        ),
        "search" to mapOf(
            LANGUAGE_ENGLISH to "Search",
            LANGUAGE_INDONESIAN to "Cari"
        ),
        "loading" to mapOf(
            LANGUAGE_ENGLISH to "Loading...",
            LANGUAGE_INDONESIAN to "Memuat..."
        ),
        "error" to mapOf(
            LANGUAGE_ENGLISH to "Error",
            LANGUAGE_INDONESIAN to "Kesalahan"
        ),
        "success" to mapOf(
            LANGUAGE_ENGLISH to "Success",
            LANGUAGE_INDONESIAN to "Berhasil"
        ),
        "warning" to mapOf(
            LANGUAGE_ENGLISH to "Warning",
            LANGUAGE_INDONESIAN to "Peringatan"
        ),
        "info" to mapOf(
            LANGUAGE_ENGLISH to "Information",
            LANGUAGE_INDONESIAN to "Informasi"
        ),
        "yes" to mapOf(
            LANGUAGE_ENGLISH to "Yes",
            LANGUAGE_INDONESIAN to "Ya"
        ),
        "no" to mapOf(
            LANGUAGE_ENGLISH to "No",
            LANGUAGE_INDONESIAN to "Tidak"
        ),
        "ok" to mapOf(
            LANGUAGE_ENGLISH to "OK",
            LANGUAGE_INDONESIAN to "OK"
        ),
        "retry" to mapOf(
            LANGUAGE_ENGLISH to "Retry",
            LANGUAGE_INDONESIAN to "Coba Lagi"
        ),
        "refresh" to mapOf(
            LANGUAGE_ENGLISH to "Refresh",
            LANGUAGE_INDONESIAN to "Segarkan"
        ),
        "submit" to mapOf(
            LANGUAGE_ENGLISH to "Submit",
            LANGUAGE_INDONESIAN to "Kirim"
        ),
        "upload" to mapOf(
            LANGUAGE_ENGLISH to "Upload",
            LANGUAGE_INDONESIAN to "Unggah"
        ),
        "download" to mapOf(
            LANGUAGE_ENGLISH to "Download",
            LANGUAGE_INDONESIAN to "Unduh"
        ),
        "view" to mapOf(
            LANGUAGE_ENGLISH to "View",
            LANGUAGE_INDONESIAN to "Lihat"
        ),
        "close" to mapOf(
            LANGUAGE_ENGLISH to "Close",
            LANGUAGE_INDONESIAN to "Tutup"
        ),
        "back" to mapOf(
            LANGUAGE_ENGLISH to "Back",
            LANGUAGE_INDONESIAN to "Kembali"
        ),
        "next" to mapOf(
            LANGUAGE_ENGLISH to "Next",
            LANGUAGE_INDONESIAN to "Selanjutnya"
        ),
        "previous" to mapOf(
            LANGUAGE_ENGLISH to "Previous",
            LANGUAGE_INDONESIAN to "Sebelumnya"
        ),
        
        // Dashboard specific
        "kpr_application" to mapOf(
            LANGUAGE_ENGLISH to "KPR Application",
            LANGUAGE_INDONESIAN to "Aplikasi KPR"
        ),
        "application_status" to mapOf(
            LANGUAGE_ENGLISH to "Application Status",
            LANGUAGE_INDONESIAN to "Status Aplikasi"
        ),
        "documents" to mapOf(
            LANGUAGE_ENGLISH to "Documents",
            LANGUAGE_INDONESIAN to "Dokumen"
        ),
        "payments" to mapOf(
            LANGUAGE_ENGLISH to "Payments",
            LANGUAGE_INDONESIAN to "Pembayaran"
        ),
        "notifications" to mapOf(
            LANGUAGE_ENGLISH to "Notifications",
            LANGUAGE_INDONESIAN to "Notifikasi"
        ),
        
        // Dossier specific
        "dossier_id" to mapOf(
            LANGUAGE_ENGLISH to "Dossier ID",
            LANGUAGE_INDONESIAN to "ID Dossier"
        ),
        "customer_name" to mapOf(
            LANGUAGE_ENGLISH to "Customer Name",
            LANGUAGE_INDONESIAN to "Nama Pelanggan"
        ),
        "property_type" to mapOf(
            LANGUAGE_ENGLISH to "Property Type",
            LANGUAGE_INDONESIAN to "Tipe Properti"
        ),
        "loan_amount" to mapOf(
            LANGUAGE_ENGLISH to "Loan Amount",
            LANGUAGE_INDONESIAN to "Jumlah Pinjaman"
        ),
        "down_payment" to mapOf(
            LANGUAGE_ENGLISH to "Down Payment",
            LANGUAGE_INDONESIAN to "Uang Muka"
        ),
        "bank_name" to mapOf(
            LANGUAGE_ENGLISH to "Bank Name",
            LANGUAGE_INDONESIAN to "Nama Bank"
        ),
        
        // Document specific
        "upload_document" to mapOf(
            LANGUAGE_ENGLISH to "Upload Document",
            LANGUAGE_INDONESIAN to "Unggah Dokumen"
        ),
        "document_type" to mapOf(
            LANGUAGE_ENGLISH to "Document Type",
            LANGUAGE_INDONESIAN to "Jenis Dokumen"
        ),
        "verification_status" to mapOf(
            LANGUAGE_ENGLISH to "Verification Status",
            LANGUAGE_INDONESIAN to "Status Verifikasi"
        ),
        "verified" to mapOf(
            LANGUAGE_ENGLISH to "Verified",
            LANGUAGE_INDONESIAN to "Terverifikasi"
        ),
        "pending" to mapOf(
            LANGUAGE_ENGLISH to "Pending",
            LANGUAGE_INDONESIAN to "Menunggu"
        ),
        "rejected" to mapOf(
            LANGUAGE_ENGLISH to "Rejected",
            LANGUAGE_INDONESIAN to "Ditolak"
        ),
        
        // Payment specific
        "payment_schedule" to mapOf(
            LANGUAGE_ENGLISH to "Payment Schedule",
            LANGUAGE_INDONESIAN to "Jadwal Pembayaran"
        ),
        "installment" to mapOf(
            LANGUAGE_ENGLISH to "Installment",
            LANGUAGE_INDONESIAN to "Cicilan"
        ),
        "due_date" to mapOf(
            LANGUAGE_ENGLISH to "Due Date",
            LANGUAGE_INDONESIAN to "Tanggal Jatuh Tempo"
        ),
        "paid_amount" to mapOf(
            LANGUAGE_ENGLISH to "Paid Amount",
            LANGUAGE_INDONESIAN to "Jumlah Dibayar"
        ),
        "remaining_amount" to mapOf(
            LANGUAGE_ENGLISH to "Remaining Amount",
            LANGUAGE_INDONESIAN to "Sisa Jumlah"
        ),
        "overdue" to mapOf(
            LANGUAGE_ENGLISH to "Overdue",
            LANGUAGE_INDONESIAN to "Terlambat"
        ),
        
        // Error messages
        "error_network" to mapOf(
            LANGUAGE_ENGLISH to "Network error. Please check your connection.",
            LANGUAGE_INDONESIAN to "Kesalahan jaringan. Silakan periksa koneksi Anda."
        ),
        "error_server" to mapOf(
            LANGUAGE_ENGLISH to "Server error. Please try again later.",
            LANGUAGE_INDONESIAN to "Kesalahan server. Silakan coba lagi nanti."
        ),
        "error_validation" to mapOf(
            LANGUAGE_ENGLISH to "Validation error. Please check your input.",
            LANGUAGE_INDONESIAN to "Kesalahan validasi. Silakan periksa input Anda."
        ),
        "error_unauthorized" to mapOf(
            LANGUAGE_ENGLISH to "Unauthorized access. Please login again.",
            LANGUAGE_INDONESIAN to "Akses tidak sah. Silakan masuk kembali."
        ),
        "error_not_found" to mapOf(
            LANGUAGE_ENGLISH to "Data not found.",
            LANGUAGE_INDONESIAN to "Data tidak ditemukan."
        ),
        
        // Validation messages
        "validation_required" to mapOf(
            LANGUAGE_ENGLISH to "This field is required.",
            LANGUAGE_INDONESIAN to "Field ini wajib diisi."
        ),
        "validation_email" to mapOf(
            LANGUAGE_ENGLISH to "Please enter a valid email address.",
            LANGUAGE_INDONESIAN to "Silakan masukkan alamat email yang valid."
        ),
        "validation_phone" to mapOf(
            LANGUAGE_ENGLISH to "Please enter a valid phone number.",
            LANGUAGE_INDONESIAN to "Silakan masukkan nomor telepon yang valid."
        ),
        "validation_min_length" to mapOf(
            LANGUAGE_ENGLISH to "Minimum length is {0} characters.",
            LANGUAGE_INDONESIAN to "Panjang minimum adalah {0} karakter."
        ),
        "validation_max_length" to mapOf(
            LANGUAGE_ENGLISH to "Maximum length is {0} characters.",
            LANGUAGE_INDONESIAN to "Panjang maksimum adalah {0} karakter."
        )
    )
    
    suspend fun getTranslation(
        key: String,
        language: String = DEFAULT_LANGUAGE,
        parameters: Map<String, String> = emptyMap()
    ): Result<String> {
        return try {
            // First try to get from database
            val translation = getTranslationFromDatabase(key, language)
                .getOrNull()
            
            val finalTranslation = translation ?: defaultTranslations[key]?.get(language)
                ?: defaultTranslations[key]?.get(DEFAULT_LANGUAGE)
                ?: key // Fallback to key if no translation found
            
            // Apply parameters if provided
            val result = if (parameters.isNotEmpty()) {
                parameters.entries.fold(finalTranslation) { acc, (paramKey, paramValue) ->
                    acc.replace("{$paramKey}", paramValue)
                }
            } else {
                finalTranslation
            }
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTranslations(
        language: String = DEFAULT_LANGUAGE,
        category: String? = null
    ): Result<Map<String, String>> {
        return try {
            // Get translations from database
            val dbTranslations = getTranslationsFromDatabase(language, category)
                .getOrNull().orEmpty()
            
            // Merge with default translations
            val allTranslations = mutableMapOf<String, String>()
            
            // Add default translations
            defaultTranslations.forEach { (key, translations) ->
                val translation = translations[language] ?: translations[DEFAULT_LANGUAGE] ?: key
                allTranslations[key] = translation
            }
            
            // Override with database translations
            dbTranslations.forEach { translation ->
                allTranslations[translation.key] = translation.value
            }
            
            // Filter by category if specified
            val filteredTranslations = if (category != null) {
                allTranslations.filter { (key, _) ->
                    key.startsWith("${category}_")
                }
            } else {
                allTranslations
            }
            
            Result.success(filteredTranslations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserLanguage(userId: String): Result<String> {
        return try {
            val user = postgrest.from("user_profiles")
                .select("preferred_language")
                .filter { eq("id", userId) }
                .maybeSingle()
                .data
            
            val language = user?.preferred_language ?: DEFAULT_LANGUAGE
            Result.success(language)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun setUserLanguage(
        userId: String,
        language: String
    ): Result<Unit> {
        return try {
            if (!supportedLanguages.contains(language)) {
                return Result.failure(Exception("Unsupported language: $language"))
            }
            
            postgrest.from("user_profiles")
                .update(
                    mapOf(
                        "preferred_language" to language,
                        "updated_at" to java.time.Instant.now().toString()
                    )
                )
                .filter { eq("id", userId) }
                .maybeSingle()
                .data
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun addTranslation(
        key: String,
        language: String,
        value: String,
        category: String? = null,
        description: String? = null
    ): Result<Unit> {
        return try {
            val translationData = mapOf(
                "key" to key,
                "language" to language,
                "value" to value,
                "category" to category,
                "description" to description,
                "created_at" to java.time.Instant.now().toString(),
                "updated_at" to java.time.Instant.now().toString()
            )
            
            postgrest.from("translations")
                .insert(translationData)
                .maybeSingle()
                .data
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateTranslation(
        key: String,
        language: String,
        value: String
    ): Result<Unit> {
        return try {
            postgrest.from("translations")
                .update(
                    mapOf(
                        "value" to value,
                        "updated_at" to java.time.Instant.now().toString()
                    )
                )
                .filter { 
                    eq("key", key)
                    eq("language", language)
                }
                .maybeSingle()
                .data
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteTranslation(
        key: String,
        language: String
    ): Result<Unit> {
        return try {
            postgrest.from("translations")
                .delete()
                .filter { 
                    eq("key", key)
                    eq("language", language)
                }
                .maybeSingle()
                .data
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTranslationFromDatabase(
        key: String,
        language: String
    ): Result<String> {
        return try {
            val translation = postgrest.from("translations")
                .select("value")
                .filter { 
                    eq("key", key)
                    eq("language", language)
                }
                .maybeSingle()
                .data
            
            translation?.let { 
                    Result.success(it.value)
                }
                ?: Result.failure(Exception("Translation not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTranslationsFromDatabase(
        language: String,
        category: String? = null
    ): Result<List<Translation>> {
        return try {
            var query = postgrest.from("translations")
                .select()
                .filter { eq("language", language) }
            
            category?.let { query = query.filter { eq("category", it) } }
            
            val translations = query
                .order("key")
                .data
            
            Result.success(translations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAllLanguages(): Result<List<LanguageInfo>> {
        return try {
            val languages = supportedLanguages.map { langCode ->
                LanguageInfo(
                    code = langCode,
                    name = when (langCode) {
                        LANGUAGE_ENGLISH -> "English"
                        LANGUAGE_INDONESIAN -> "Bahasa Indonesia"
                        else -> langCode.uppercase()
                    },
                    isDefault = langCode == DEFAULT_LANGUAGE,
                    isActive = true
                )
            }
            
            Result.success(languages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTranslationStatistics(): Result<TranslationStatistics> {
        return try {
            val stats = postgrest.from("translations")
                .select("language, count(*) as count")
                .group("language")
                .order("count", ascending = false)
                .data
            
            val totalTranslations = stats.sumOf { it.count }
            val languageStats = stats.map { stat ->
                LanguageStat(
                    language = stat.language,
                    count = stat.count,
                    percentage = if (totalTranslations > 0) {
                        (stat.count.toDouble() / totalTranslations) * 100
                    } else 0.0
                )
            }
            
            val statistics = TranslationStatistics(
                totalTranslations = totalTranslations,
                languageStats = languageStats,
                supportedLanguages = supportedLanguages.size,
                generatedAt = java.time.Instant.now().toString()
            )
            
            Result.success(statistics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observeLanguageChanges(): Flow<LanguageUpdate> = flow {
        try {
            // TODO: Implement real-time updates via Supabase Realtime
            emit(LanguageUpdate.TranslationsUpdated)
        } catch (e: Exception) {
            emit(LanguageUpdate.Error(e.message ?: "Unknown error"))
        }
    }
    
    fun formatCurrency(
        amount: java.math.BigDecimal,
        language: String = DEFAULT_LANGUAGE
    ): String {
        return when (language) {
            LANGUAGE_ENGLISH -> "Rp ${String.format("%,.0f", amount)}"
            LANGUAGE_INDONESIAN -> "Rp ${String.format("%,.0f", amount)}"
            else -> "Rp ${String.format("%,.0f", amount)}"
        }
    }
    
    fun formatDate(
        date: java.time.LocalDate,
        language: String = DEFAULT_LANGUAGE
    ): String {
        val formatter = when (language) {
            LANGUAGE_ENGLISH -> java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")
                .withLocale(Locale.ENGLISH)
            LANGUAGE_INDONESIAN -> java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")
                .withLocale(Locale.forLanguageTag("id-ID"))
            else -> java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
        }
        
        return date.format(formatter)
    }
    
    fun formatDateTime(
        dateTime: java.time.LocalDateTime,
        language: String = DEFAULT_LANGUAGE
    ): String {
        val formatter = when (language) {
            LANGUAGE_ENGLISH -> java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
                .withLocale(Locale.ENGLISH)
            LANGUAGE_INDONESIAN -> java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
                .withLocale(Locale.forLanguageTag("id-ID"))
            else -> java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
        }
        
        return dateTime.format(formatter)
    }
}

// Data classes
data class Translation(
    val id: String,
    val key: String,
    val language: String,
    val value: String,
    val category: String?,
    val description: String?,
    val createdAt: String,
    val updatedAt: String
)

data class LanguageInfo(
    val code: String,
    val name: String,
    val isDefault: Boolean,
    val isActive: Boolean
)

data class LanguageStat(
    val language: String,
    val count: Int,
    val percentage: Double
)

data class TranslationStatistics(
    val totalTranslations: Int,
    val languageStats: List<LanguageStat>,
    val supportedLanguages: Int,
    val generatedAt: String
)

sealed class LanguageUpdate {
    object TranslationsUpdated : LanguageUpdate()
    object LanguageChanged : LanguageUpdate()
    object NewLanguageAdded : LanguageUpdate()
    data class Error(val message: String) : LanguageUpdate()
}
