package com.kprflow.enterprise.data.model

enum class UnitStatus(val displayName: String) {
    AVAILABLE("Available"),
    BOOKED("Booked"),
    LOCKED("Locked"),
    SOLD("Sold");
    
    companion object {
        fun fromString(value: String): UnitStatus {
            return values().find { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown unit status: $value")
        }
    }
}
