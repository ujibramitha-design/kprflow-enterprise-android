package com.kprflow.enterprise.di

import com.kprflow.enterprise.data.repository.SLARepositoryImpl
import com.kprflow.enterprise.domain.repository.ISLARepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SLAModule {

    @Binds
    @Singleton
    abstract fun bindSLARepository(
        slaRepositoryImpl: SLARepositoryImpl
    ): ISLARepository
}
