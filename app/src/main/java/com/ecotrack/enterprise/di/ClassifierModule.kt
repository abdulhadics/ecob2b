package com.ecotrack.enterprise.di

import com.ecotrack.enterprise.data.ml.PytorchTransportClassifier
import com.ecotrack.enterprise.domain.repository.TransportModeClassifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ClassifierModule {

    @Binds
    @Singleton
    abstract fun bindClassifier(
        pytorchClassifier: PytorchTransportClassifier
    ): TransportModeClassifier
}
