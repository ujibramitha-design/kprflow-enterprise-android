package com.kprflow.enterprise.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val nik: String,
    val phoneNumber: String,
    val maritalStatus: String,
    val role: UserRole,
    val createdAt: String,
    val updatedAt: String,
    val isActive: Boolean = true
)
