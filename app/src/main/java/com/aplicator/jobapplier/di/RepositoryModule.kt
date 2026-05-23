package com.aplicator.jobapplier.di

import com.aplicator.jobapplier.data.repository.AiRepository
import com.aplicator.jobapplier.data.repository.AiRepositoryImpl
import com.aplicator.jobapplier.data.repository.AuthRepository
import com.aplicator.jobapplier.data.repository.AuthRepositoryImpl
import com.aplicator.jobapplier.data.repository.JobRepository
import com.aplicator.jobapplier.data.repository.JobRepositoryImpl
import com.aplicator.jobapplier.data.repository.ProfileRepository
import com.aplicator.jobapplier.data.repository.ProfileRepositoryImpl
import com.aplicator.jobapplier.data.repository.QuotaRepository
import com.aplicator.jobapplier.data.repository.QuotaRepositoryImpl
import com.aplicator.jobapplier.data.repository.ResumeImportRepository
import com.aplicator.jobapplier.data.repository.ResumeImportRepositoryImpl
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
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindJobRepository(impl: JobRepositoryImpl): JobRepository

    @Binds
    @Singleton
    abstract fun bindAiRepository(impl: AiRepositoryImpl): AiRepository

    @Binds
    @Singleton
    abstract fun bindResumeImportRepository(impl: ResumeImportRepositoryImpl): ResumeImportRepository

    @Binds
    @Singleton
    abstract fun bindQuotaRepository(impl: QuotaRepositoryImpl): QuotaRepository
}
