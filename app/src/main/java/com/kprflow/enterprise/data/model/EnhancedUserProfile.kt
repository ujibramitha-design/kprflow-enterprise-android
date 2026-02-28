package com.kprflow.enterprise.data.model

import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class EnhancedUserProfile(
    val id: String,
    val name: String? = null,                    // Nama lengkap (auto dari KTP OCR)
    val email: String? = null,                   // Email aktif (untuk notifikasi)
    val nik: String,                            // 16 digit NIK (login & validasi)
    val phoneNumber: String? = null,             // Nomor HP aktif (WhatsApp)
    val maritalStatus: String? = null,           // Status pernikahan (auto dari KTP)
    val birthPlaceDate: String? = null,          // Tempat/tgl lahir "Jakarta/01-01-1990"
    val currentJobId: String? = null,            // ID kategori pekerjaan
    val jobCategory: JobCategory? = null,        // Detail kategori pekerjaan
    val companyName: String? = null,             // Nama perusahaan tempat bekerja
    val position: String? = null,                // Posisi/jabatan
    val incomeSourceId: String? = null,          // ID sumber income (Cash/Transfer)
    val incomeSource: IncomeSource? = null,      // Detail sumber income
    val incomeTypeId: String? = null,            // ID jenis income (Payroll/Non-Payroll)
    val incomeType: IncomeType? = null,          // Detail jenis income
    val monthlyIncome: BigDecimal? = null,       // Income bulanan
    val profileCompletionPercentage: Int = 0,   // Persentase kelengkapan profil
    val ktpVerified: Boolean = false,            // Status verifikasi KTP
    val phoneVerified: Boolean = false,          // Status verifikasi telepon
    val emailVerified: Boolean = false,          // Status verifikasi email
    val ktpImageUrl: String? = null,             // URL gambar KTP
    val ktpExtractedData: KtpExtractedData? = null, // Data hasil OCR KTP
    val lastLoginAt: String? = null,             // Terakhir login
    val createdAt: String,                       // Tanggal dibuat
    val updatedAt: String,                       // Terakhir update
    val isActive: Boolean = true                 // Status aktif
)

@Serializable
data class JobCategory(
    val id: String,
    val categoryName: String,                   // Nama kategori pekerjaan
    val categoryCode: String,                    // Kode kategori
    val description: String? = null,             // Deskripsi
    val isActive: Boolean = true                 // Status aktif
)

@Serializable
data class IncomeSource(
    val id: String,
    val sourceName: String,                      // Nama sumber income
    val sourceCode: String,                      // Kode sumber
    val description: String? = null,             // Deskripsi
    val isActive: Boolean = true                 // Status aktif
)

@Serializable
data class IncomeVariationType(
    val id: String,
    val variationName: String,
    val variationCode: String,
    val description: String,
    val isActive: Boolean = true
)

@Serializable
data class CustomerIncomeVariation(
    val id: String,
    val customerId: String,
    val variationType: IncomeVariationType,
    val bankAccountNumber: String,
    val bankName: String,
    val bankAccountStatus: String,
    val monthlyIncome: BigDecimal?,
    val additionalIncome: BigDecimal,
    val incomeProofDocumentUrl: String?,
    val verificationStatus: String,
    val verifiedBy: String?,
    val verifiedAt: String?,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class KtpExtractedData(
    val name: String? = null,                    // Nama dari KTP
    val nik: String? = null,                     // NIK dari KTP
    val birthPlaceDate: String? = null,          // Tempat/tgl lahir dari KTP
    val maritalStatus: String? = null,           // Status pernikahan dari KTP
    val address: String? = null,                // Alamat dari KTP
    val rt: String? = null,                      // RT dari KTP
    val rw: String? = null,                      // RW dari KTP
    val kelurahan: String? = null,               // Kelurahan dari KTP
    val kecamatan: String? = null,               // Kecamatan dari KTP
    val kabupaten: String? = null,               // Kabupaten dari KTP
    val provinsi: String? = null,                // Provinsi dari KTP
    val agama: String? = null,                   // Agama dari KTP
    val pekerjaan: String? = null,               // Pekerjaan dari KTP
    val kewarganegaraan: String? = null,          // Kewarganegaraan dari KTP
    val berlakuHingga: String? = null,           // Masa berlaku KTP
    val imageUrl: String? = null,                // URL gambar KTP
    val confidenceScore: Double = 0.0,           // Skor kepercayaan OCR
    val extractionTime: String? = null            // Waktu ekstraksi
)

@Serializable
data class ProfileCompletionStatus(
    val userId: String,
    val totalFields: Int = 12,                   // Total field yang harus diisi
    val completedFields: Int = 0,                // Field yang sudah diisi
    val completionPercentage: Int = 0,           // Persentase kelengkapan
    val missingFields: List<String> = emptyList(), // Field yang belum diisi
    val isProfileComplete: Boolean = false,      // Status kelengkapan profil
    val requiresManualCompletion: Boolean = false, // Perlu pengisian manual
    val ktpVerificationStatus: String = "PENDING", // Status verifikasi KTP
    val nextStep: String? = null                 // Langkah selanjutnya
)

@Serializable
data class NikLoginRequest(
    val nik: String,                             // 16 digit NIK
    val deviceInfo: DeviceInfo? = null           // Info device untuk logging
)

@Serializable
data class DeviceInfo(
    val deviceId: String? = null,                // Device ID
    val deviceType: String? = null,              // Device type (mobile/web)
    val osVersion: String? = null,               // OS version
    val appVersion: String? = null,              // App version
    val ipAddress: String? = null,               // IP address
    val userAgent: String? = null                 // User agent
)

@Serializable
data class NikLoginResponse(
    val success: Boolean,
    val userId: String? = null,
    val message: String,
    val profileComplete: Int = 0,               // Persentase kelengkapan profil
    val requiresCompletion: Boolean = false,      // Perlu lengkapi profil
    val isNewUser: Boolean = false,              // User baru
    val token: String? = null                    // JWT token
)

@Serializable
data class KtpUploadRequest(
    val userId: String,
    val ktpImageBase64: String,                 // Gambar KTP dalam base64
    val imageFormat: String = "jpg",             // Format gambar
    val deviceInfo: DeviceInfo? = null
)

@Serializable
data class KtpUploadResponse(
    val success: Boolean,
    val message: String,
    val extractedData: KtpExtractedData? = null,
    val confidenceScore: Double = 0.0,
    val requiresManualInput: Boolean = false,
    val autoFilledFields: List<String> = emptyList(),
    val missingFields: List<String> = emptyList()
)

@Serializable
data class ProfileUpdateRequest(
    val userId: String,
    val name: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val maritalStatus: String? = null,
    val birthPlaceDate: String? = null,
    val currentJobId: String? = null,
    val companyName: String? = null,
    val position: String? = null,
    val incomeSourceId: String? = null,
    val incomeTypeId: String? = null,
    val monthlyIncome: BigDecimal? = null
)

@Serializable
data class ProfileUpdateResponse(
    val success: Boolean,
    val message: String,
    val updatedProfile: EnhancedUserProfile? = null,
    val completionPercentage: Int = 0,
    val isProfileComplete: Boolean = false
)

@Serializable
data class VerificationRequest(
    val userId: String,
    val verificationType: String,                 // "PHONE" or "EMAIL"
    val verificationValue: String,               // Phone number or email
    val deviceInfo: DeviceInfo? = null
)

@Serializable
data class VerificationResponse(
    val success: Boolean,
    val message: String,
    val verificationCode: String? = null,        // OTP code
    val expiresAt: String? = null               // Expiration time
)

@Serializable
data class VerificationConfirmRequest(
    val userId: String,
    val verificationType: String,
    val verificationCode: String
)

@Serializable
data class VerificationConfirmResponse(
    val success: Boolean,
    val message: String,
    val isVerified: Boolean = false
)

// Enum untuk status verifikasi
enum class VerificationStatus {
    PENDING,           // Menunggu verifikasi
    SUCCESS,           // Verifikasi berhasil
    FAILED,            // Verifikasi gagal
    EXPIRED            // Kode verifikasi kadaluarsa
}

// Enum untuk status profil
enum class ProfileStatus {
    INCOMPLETE,        // Profil belum lengkap
    PENDING_REVIEW,    // Menunggu review
    APPROVED,          // Profil disetujui
    REJECTED           // Profil ditolak
}
