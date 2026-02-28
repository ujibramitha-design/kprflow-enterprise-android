package com.kprflow.enterprise.util

object Constants {
    
    // Supabase Configuration
    const val SUPABASE_URL = "https://your-project.supabase.co"
    const val SUPABASE_ANON_KEY = "your-anon-key"
    
    // Storage Buckets
    const val DOCUMENTS_BUCKET = "kpr_documents"
    const val BANK_SUBMISSIONS_BUCKET = "bank_submissions"
    const val PROFILE_IMAGES_BUCKET = "profile_images"
    
    // Database Tables
    const val USER_PROFILES_TABLE = "user_profiles"
    const val UNIT_PROPERTIES_TABLE = "unit_properties"
    const val KPR_DOSSIERS_TABLE = "kpr_dossiers"
    const val DOCUMENTS_TABLE = "documents"
    const val FINANCIAL_TRANSACTIONS_TABLE = "financial_transactions"
    const val UNIT_SWAP_REQUESTS_TABLE = "unit_swap_requests"
    
    // Real-time Channels
    const val KPR_DOSSIERS_CHANNEL = "kpr_dossiers"
    const val DOCUMENTS_CHANNEL = "documents"
    const val UNITS_CHANNEL = "unit_properties"
    
    // File Upload Limits
    const val MAX_FILE_SIZE_MB = 10
    const val MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024
    
    // Supported File Types
    val SUPPORTED_DOCUMENT_TYPES = listOf(
        "application/pdf",
        "image/jpeg",
        "image/png",
        "image/jpg"
    )
    
    // SLA Durations (in days)
    const val DOCUMENT_SLA_DAYS = 14
    const val BANK_SLA_DAYS = 60
    const val BANK_EXTENSION_DAYS = 30
    
    // Pagination
    const val DEFAULT_PAGE_SIZE = 20
    const val MAX_PAGE_SIZE = 100
    
    // Cache Duration (in milliseconds)
    const val CACHE_DURATION = 5 * 60 * 1000L // 5 minutes
    
    // Error Messages
    const val ERROR_NETWORK = "Network error. Please check your connection."
    const val ERROR_AUTH = "Authentication failed. Please login again."
    const val ERROR_UPLOAD = "File upload failed. Please try again."
    const val ERROR_PERMISSION = "Permission denied. You don't have access to this resource."
    const val ERROR_NOT_FOUND = "Resource not found."
    const val ERROR_VALIDATION = "Validation error. Please check your input."
    
    // Success Messages
    const val SUCCESS_UPLOAD = "File uploaded successfully."
    const val SUCCESS_UPDATE = "Data updated successfully."
    const val SUCCESS_DELETE = "Data deleted successfully."
    const val SUCCESS_VERIFICATION = "Document verified successfully."
    
    // Notification Types
    const val NOTIFICATION_DOCUMENT_MISSING = "document_missing"
    const val NOTIFICATION_SP3K_ISSUED = "sp3k_issued"
    const val NOTIFICATION_SLA_WARNING = "sla_warning"
    const val NOTIFICATION_STATUS_CHANGE = "status_change"
    
    // WhatsApp Template Names
    const val WA_TEMPLATE_DOCUMENT_REMINDER = "document_reminder"
    const val WA_TEMPLATE_SP3K_NOTIFICATION = "sp3k_notification"
    const val WA_TEMPLATE_SLA_WARNING = "sla_warning"
    const val WA_TEMPLATE_BAST_INVITATION = "bast_invitation"
    
    // Financial Precision
    const val FINANCIAL_SCALE = 2 // Decimal places for financial calculations
    
    // Date Formats
    const val DATE_FORMAT_API = "yyyy-MM-dd"
    const val DATE_FORMAT_DISPLAY = "dd MMM yyyy"
    const val DATETIME_FORMAT_API = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    const val DATETIME_FORMAT_DISPLAY = "dd MMM yyyy HH:mm"
}
