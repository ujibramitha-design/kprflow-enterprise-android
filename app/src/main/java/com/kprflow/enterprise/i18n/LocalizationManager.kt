package com.kprflow.enterprise.i18n

import android.content.Context
import java.util.*

/**
 * Localization Manager for multi-language support
 */
class LocalizationManager(private val context: Context) {
    
    private val currentLocale = Locale("id", "ID") // Default to Indonesian
    
    /**
     * Get localized string
     */
    fun getString(key: String, language: String = "id"): String {
        return when (language) {
            "id" -> getIndonesianString(key)
            "en" -> getEnglishString(key)
            else -> getIndonesianString(key)
        }
    }
    
    /**
     * Get Indonesian strings
     */
    private fun getIndonesianString(key: String): String {
        return when (key) {
            "app_name" -> "KPRFlow Enterprise"
            "dashboard_title" -> "Dashboard"
            "customer_dashboard" -> "Dashboard Pelanggan"
            "legal_dashboard" -> "Dashboard Legal"
            "finance_dashboard" -> "Dashboard Keuangan"
            "marketing_dashboard" -> "Dashboard Marketing"
            "executive_dashboard" -> "Dashboard Eksekutif"
            
            "status_lead" -> "Lead"
            "status_pemberkasan" -> "Pemberkasan"
            "status_verifikasi_kelengkapan" -> "Verifikasi Kelengkapan"
            "status_pengajuan_bank" -> "Pengajuan Bank"
            "status_approval_bank" -> "Approval Bank"
            "status_akad" -> "Akad"
            "status_bast" -> "BAST"
            "status_disbursed" -> "Disbursed"
            "status_completed" -> "Selesai"
            "status_cancelled" -> "Dibatalkan"
            
            "document_ktp" -> "KTP"
            "document_kk" -> "Kartu Keluarga"
            "document_npwp" -> "NPWP"
            "document_slip_gaji" -> "Slip Gaji"
            "document_rekening_koran" -> "Rekening Koran"
            "document_sertifikat" -> "Sertifikat"
            "document_imb" -> "IMB"
            "document_pbb" -> "PBB"
            
            "payment_booking_fee" -> "Booking Fee"
            "payment_dp_1" -> "DP 1"
            "payment_dp_2" -> "DP 2"
            "payment_dp_pelunasan" -> "DP Pelunasan"
            "payment_biaya_strategis" -> "Biaya Strategis"
            "payment_biaya_admin" -> "Biaya Admin"
            "payment_biaya_notaris" -> "Biaya Notaris"
            "payment_biaya_asuransi" -> "Biaya Asuransi"
            
            "role_customer" -> "Pelanggan"
            "role_marketing" -> "Marketing"
            "role_legal" -> "Legal"
            "role_finance" -> "Keuangan"
            "role_estate" -> "Estate"
            "role_bod" -> "Direksi"
            "role_manager" -> "Manager"
            
            "action_upload" -> "Unggah"
            "action_download" -> "Unduh"
            "action_edit" -> "Edit"
            "action_delete" -> "Hapus"
            "action_save" -> "Simpan"
            "action_cancel" -> "Batal"
            "action_submit" -> "Kirim"
            "action_verify" -> "Verifikasi"
            "action_approve" -> "Setujui"
            "action_reject" -> "Tolak"
            
            "error_network" -> "Koneksi internet bermasalah"
            "error_server" -> "Server sedang bermasalah"
            "error_validation" -> "Data tidak valid"
            "error_permission" -> "Izin ditolak"
            "error_file_not_found" -> "File tidak ditemukan"
            "error_file_too_large" -> "File terlalu besar"
            "error_unsupported_format" -> "Format file tidak didukung"
            
            "success_upload" -> "File berhasil diunggah"
            "success_save" -> "Data berhasil disimpan"
            "success_delete" -> "Data berhasil dihapus"
            "success_send" -> "Data berhasil dikirim"
            "success_verify" -> "Data berhasil diverifikasi"
            "success_approve" -> "Data berhasil disetujui"
            
            "message_loading" -> "Memuat..."
            "message_processing" -> "Memproses..."
            "message_uploading" -> "Mengunggah..."
            "message_downloading" -> "Mengunduh..."
            "message_saving" -> "Menyimpan..."
            
            "label_name" -> "Nama"
            "label_email" -> "Email"
            "label_phone" -> "Telepon"
            "label_address" -> "Alamat"
            "label_date" -> "Tanggal"
            "label_time" -> "Waktu"
            "label_status" -> "Status"
            "label_amount" -> "Jumlah"
            "label_description" -> "Deskripsi"
            "label_notes" -> "Catatan"
            
            "confirmation_delete" -> "Apakah Anda yakin ingin menghapus data ini?"
            "confirmation_cancel" -> "Apakah Anda yakin ingin membatalkan?"
            "confirmation_logout" -> "Apakah Anda yakin ingin keluar?"
            
            "notification_new_lead" -> "Lead baru tersedia"
            "notification_status_change" -> "Status telah diperbarui"
            "notification_document_uploaded" -> "Dokumen telah diunggah"
            "notification_payment_received" -> "Pembayaran telah diterima"
            "notification_unit_cancelled" -> "Unit telah dibatalkan"
            
            else -> key // Return key if not found
        }
    }
    
    /**
     * Get English strings
     */
    private fun getEnglishString(key: String): String {
        return when (key) {
            "app_name" -> "KPRFlow Enterprise"
            "dashboard_title" -> "Dashboard"
            "customer_dashboard" -> "Customer Dashboard"
            "legal_dashboard" -> "Legal Dashboard"
            "finance_dashboard" -> "Finance Dashboard"
            "marketing_dashboard" -> "Marketing Dashboard"
            "executive_dashboard" -> "Executive Dashboard"
            
            "status_lead" -> "Lead"
            "status_pemberkasan" -> "Documentation"
            "status_verifikasi_kelengkapan" -> "Completeness Verification"
            "status_pengajuan_bank" -> "Bank Application"
            "status_approval_bank" -> "Bank Approval"
            "status_akad" -> "Akad"
            "status_bast" -> "BAST"
            "status_disbursed" -> "Disbursed"
            "status_completed" -> "Completed"
            "status_cancelled" -> "Cancelled"
            
            "document_ktp" -> "ID Card"
            "document_kk" -> "Family Card"
            "document_npwp" -> "Tax ID"
            "document_slip_gaji" -> "Payslip"
            "document_rekening_koran" -> "Bank Statement"
            "document_sertifikat" -> "Certificate"
            "document_imb" -> "Building Permit"
            "document_pbb" -> "Property Tax"
            
            "payment_booking_fee" -> "Booking Fee"
            "payment_dp_1" -> "Down Payment 1"
            "payment_dp_2" -> "Down Payment 2"
            "payment_dp_pelunasan" -> "Final Down Payment"
            "payment_biaya_strategis" -> "Strategic Fee"
            "payment_biaya_admin" -> "Admin Fee"
            "payment_biaya_notaris" -> "Notary Fee"
            "payment_biaya_asuransi" -> "Insurance Fee"
            
            "role_customer" -> "Customer"
            "role_marketing" -> "Marketing"
            "role_legal" -> "Legal"
            "role_finance" -> "Finance"
            "role_estate" -> "Estate"
            "role_bod" -> "Board of Directors"
            "role_manager" -> "Manager"
            
            "action_upload" -> "Upload"
            "action_download" -> "Download"
            "action_edit" -> "Edit"
            "action_delete" -> "Delete"
            "action_save" -> "Save"
            "action_cancel" -> "Cancel"
            "action_submit" -> "Submit"
            "action_verify" -> "Verify"
            "action_approve" -> "Approve"
            "action_reject" -> "Reject"
            
            "error_network" -> "Network connection error"
            "error_server" -> "Server error"
            "error_validation" -> "Invalid data"
            "error_permission" -> "Permission denied"
            "error_file_not_found" -> "File not found"
            "error_file_too_large" -> "File too large"
            "error_unsupported_format" -> "Unsupported file format"
            
            "success_upload" -> "File uploaded successfully"
            "success_save" -> "Data saved successfully"
            "success_delete" -> "Data deleted successfully"
            "success_send" -> "Data sent successfully"
            "success_verify" -> "Data verified successfully"
            "success_approve" -> "Data approved successfully"
            
            "message_loading" -> "Loading..."
            "message_processing" -> "Processing..."
            "message_uploading" -> "Uploading..."
            "message_downloading" -> "Downloading..."
            "message_saving" -> "Saving..."
            
            "label_name" -> "Name"
            "label_email" -> "Email"
            "label_phone" -> "Phone"
            "label_address" -> "Address"
            "label_date" -> "Date"
            "label_time" -> "Time"
            "label_status" -> "Status"
            "label_amount" -> "Amount"
            "label_description" -> "Description"
            "label_notes" -> "Notes"
            
            "confirmation_delete" -> "Are you sure you want to delete this data?"
            "confirmation_cancel" -> "Are you sure you want to cancel?"
            "confirmation_logout" -> "Are you sure you want to logout?"
            
            "notification_new_lead" -> "New lead available"
            "notification_status_change" -> "Status has been updated"
            "notification_document_uploaded" -> "Document has been uploaded"
            "notification_payment_received" -> "Payment has been received"
            "notification_unit_cancelled" -> "Unit has been cancelled"
            
            else -> key // Return key if not found
        }
    }
    
    /**
     * Format currency based on locale
     */
    fun formatCurrency(amount: Double, language: String = "id"): String {
        return when (language) {
            "id" -> "Rp ${String.format("%,.0f", amount)}"
            "en" -> "IDR ${String.format("%,.0f", amount)}"
            else -> "Rp ${String.format("%,.0f", amount)}"
        }
    }
    
    /**
     * Format date based on locale
     */
    fun formatDate(date: Date, language: String = "id"): String {
        val locale = when (language) {
            "id" -> Locale("id", "ID")
            "en" -> Locale("en", "US")
            else -> Locale("id", "ID")
        }
        
        return when (language) {
            "id" -> {
                val format = java.text.SimpleDateFormat("dd MMMM yyyy", locale)
                format.format(date)
            }
            "en" -> {
                val format = java.text.SimpleDateFormat("MMMM dd, yyyy", locale)
                format.format(date)
            }
            else -> {
                val format = java.text.SimpleDateFormat("dd MMMM yyyy", locale)
                format.format(date)
            }
        }
    }
    
    /**
     * Format date time based on locale
     */
    fun formatDateTime(date: Date, language: String = "id"): String {
        val locale = when (language) {
            "id" -> Locale("id", "ID")
            "en" -> Locale("en", "US")
            else -> Locale("id", "ID")
        }
        
        return when (language) {
            "id" -> {
                val format = java.text.SimpleDateFormat("dd MMMM yyyy HH:mm", locale)
                format.format(date)
            }
            "en" -> {
                val format = java.text.SimpleDateFormat("MMMM dd, yyyy HH:mm", locale)
                format.format(date)
            }
            else -> {
                val format = java.text.SimpleDateFormat("dd MMMM yyyy HH:mm", locale)
                format.format(date)
            }
        }
    }
    
    /**
     * Get supported languages
     */
    fun getSupportedLanguages(): List<Language> {
        return listOf(
            Language("id", "Indonesia", "🇮🇩"),
            Language("en", "English", "🇺🇸")
        )
    }
    
    /**
     * Get current language
     */
    fun getCurrentLanguage(): String {
        return "id" // Default to Indonesian
    }
}

/**
 * Language data class
 */
data class Language(
    val code: String,
    val name: String,
    val flag: String
)
