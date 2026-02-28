package com.kprflow.enterprise.di

import com.kprflow.enterprise.data.repository.AuthRepository
import com.kprflow.enterprise.data.repository.DocumentRepository
import com.kprflow.enterprise.data.repository.KprRepository
import com.kprflow.enterprise.domain.repository.IAuthRepository
import com.kprflow.enterprise.domain.repository.IDocumentRepository
import com.kprflow.enterprise.domain.repository.IKprRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepository: AuthRepository
    ): IAuthRepository

    @Binds
    @Singleton
    abstract fun bindKprRepository(
        kprRepository: KprRepository
    ): IKprRepository

    @Binds
    @Singleton
    abstract fun bindDocumentRepository(
        documentRepository: DocumentRepository
    ): IDocumentRepository
}
