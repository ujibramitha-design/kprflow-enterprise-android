package com.kprflow.enterprise.di

import android.content.Context
import com.kprflow.enterprise.data.repository.AuthRepository
import com.kprflow.enterprise.data.repository.DocumentRepository
import com.kprflow.enterprise.data.repository.KprRepository
import com.kprflow.enterprise.domain.repository.IAuthRepository
import com.kprflow.enterprise.domain.repository.IDocumentRepository
import com.kprflow.enterprise.domain.repository.IKprRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: io.github.jan.supabase.auth.Auth,
        postgrest: io.github.jan.supabase.postgrest.Postgrest
    ): IAuthRepository {
        return AuthRepository(auth, postgrest)
    }
    
    @Provides
    @Singleton
    fun provideDocumentRepository(
        storage: io.github.jan.supabase.storage.Storage,
        postgrest: io.github.jan.supabase.postgrest.Postgrest
    ): IDocumentRepository {
        return DocumentRepository(storage, postgrest)
    }
    
    @Provides
    @Singleton
    fun provideKprRepository(
        postgrest: io.github.jan.supabase.postgrest.Postgrest,
        realtime: io.github.jan.supabase.realtime.Realtime
    ): IKprRepository {
        return KprRepository(postgrest, realtime)
    }
}
