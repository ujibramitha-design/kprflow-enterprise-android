package com.kprflow.enterprise.data.model

enum class KprStatus(val displayName: String, val order: Int) {
    LEAD("Lead", 0),
    PEMBERKASAN("Document Collection", 1),
    PROSES_BANK("Bank Processing", 2),
    PUTUSAN_KREDIT_ACC("Credit Approved", 3),
    SP3K_TERBIT("SP3K Issued", 4),
    PRA_AKAD("Pre-Akad", 5),
    AKAD_BELUM_CAIR("Akad Signed - Not Disbursed", 6),
    FUNDS_DISBURSED("Funds Disbursed", 7),
    BAST_READY("BAST Ready", 8),
    BAST_COMPLETED("BAST Completed", 9),
    FLOATING_DOSSIER("Floating Dossier", 10),
    CANCELLED_BY_SYSTEM("Cancelled by System", 11);
    
    companion object {
        fun fromString(value: String): KprStatus {
            return values().find { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown status: $value")
        }
        
        fun getProgressStatuses(): List<KprStatus> {
            return values().filter { it != FLOATING_DOSSIER && it != CANCELLED_BY_SYSTEM }
        }
    }
}
