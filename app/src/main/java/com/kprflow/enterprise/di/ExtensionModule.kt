package com.kprflow.enterprise.di

import com.kprflow.enterprise.data.repository.ExtensionRepositoryImpl
import com.kprflow.enterprise.domain.repository.IExtensionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ExtensionModule {

    @Binds
    @Singleton
    abstract fun bindExtensionRepository(
        extensionRepositoryImpl: ExtensionRepositoryImpl
    ): IExtensionRepository
}
