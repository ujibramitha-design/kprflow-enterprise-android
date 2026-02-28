package com.kprflow.enterprise.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(@ApplicationContext context: Context): SupabaseClient {
        // Get configuration from build config or local properties
        val supabaseUrl = getSupabaseUrl()
        val supabaseKey = getSupabaseKey()
        
        return createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKey
        ) {
            install(Auth)
            install(Postgrest)
            install(Storage)
            install(Realtime)
            
            // Configure default settings
            defaultRequest {
                // Add default headers if needed
                headers {
                    append("X-Client-Version", "1.0.0")
                    append("X-Platform", "Android")
                }
            }
        }
    }

    @Provides
    @Singleton
    fun provideAuth(supabaseClient: SupabaseClient): Auth {
        return supabaseClient.auth
    }

    @Provides
    @Singleton
    fun providePostgrest(supabaseClient: SupabaseClient): Postgrest {
        return supabaseClient.postgrest
    }

    @Provides
    @Singleton
    fun provideStorage(supabaseClient: SupabaseClient): Storage {
        return supabaseClient.storage
    }

    @Provides
    @Singleton
    fun provideRealtime(supabaseClient: SupabaseClient): Realtime {
        return supabaseClient.realtime
    }
    
    private fun getSupabaseUrl(): String {
        // In production, these should come from BuildConfig or secure storage
        return System.getenv("SUPABASE_URL") ?: "https://your-project.supabase.co"
    }
    
    private fun getSupabaseKey(): String {
        // In production, these should come from BuildConfig or secure storage
        return System.getenv("SUPABASE_ANON_KEY") ?: "your-anon-key"
    }
}
