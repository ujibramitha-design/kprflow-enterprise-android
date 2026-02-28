package com.kprflow.enterprise.data.model

enum class DocumentType(val displayName: String, val isMandatory: Boolean) {
    KTP("KTP", true),
    KK("Kartu Keluarga", true),
    NPWP("NPWP", true),
    MARRIAGE_CERTIFICATE("Marriage Certificate", false),
    PAYSLIP("Payslip", true),
    BANK_STATEMENT("Bank Statement", false),
    WORKPLACE_PHOTO("Workplace Photo", false),
    SPR_FORM("SPR Form", false);
    
    companion object {
        fun fromString(value: String): DocumentType {
            return values().find { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown document type: $value")
        }
        
        fun getMandatoryDocuments(): List<DocumentType> {
            return values().filter { it.isMandatory }
        }
    }
}
