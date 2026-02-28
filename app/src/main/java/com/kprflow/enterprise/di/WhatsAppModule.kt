package com.kprflow.enterprise.di

import com.kprflow.enterprise.data.repository.WhatsAppRepositoryImpl
import com.kprflow.enterprise.domain.repository.IWhatsAppRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WhatsAppModule {

    @Binds
    @Singleton
    abstract fun bindWhatsAppRepository(
        whatsAppRepositoryImpl: WhatsAppRepositoryImpl
    ): IWhatsAppRepository
}
