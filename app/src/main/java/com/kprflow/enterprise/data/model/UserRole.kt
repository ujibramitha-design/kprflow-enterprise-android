package com.kprflow.enterprise.data.model

enum class UserRole(val displayName: String) {
    CUSTOMER("Customer"),
    MARKETING("Marketing"),
    LEGAL("Legal"),
    FINANCE("Finance"),
    BANK("Bank"),
    TEKNIK("Technical"),
    ESTATE("Estate"),
    BOD("Board of Directors");
    
    companion object {
        fun fromString(value: String): UserRole {
            return values().find { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown role: $value")
        }
    }
}
