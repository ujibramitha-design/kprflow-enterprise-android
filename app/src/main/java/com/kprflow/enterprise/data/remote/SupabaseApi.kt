package com.kprflow.enterprise.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseApi @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    
    val auth: Auth get() = supabaseClient.auth
    val postgrest: Postgrest get() = supabaseClient.postgrest
    val storage: Storage get() = supabaseClient.storage
    val realtime: Realtime get() = supabaseClient.realtime
    
    suspend fun <T> safeCall(
        operation: suspend () -> T
    ): Result<T> {
        return try {
            val result = operation()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun isUserAuthenticated(): Boolean {
        return supabaseClient.auth.currentUser != null
    }
    
    suspend fun getCurrentUserId(): String? {
        return supabaseClient.auth.currentUser?.id
    }
    
    suspend fun signOut() {
        supabaseClient.auth.signOut()
    }
}
