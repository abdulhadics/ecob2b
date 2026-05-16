/*
 ═══════════════════════════════════════════════════════
 EcoTrack Enterprise | DAA Algorithmic Module Header
 ═══════════════════════════════════════════════════════
 [ALGORITHMIC INTENT]
   → Provide singleton instances of core algorithmic engines via 
     Dependency Injection (Hilt).

 [PARADIGM & DATA STRUCTURE]
   → Dependency Injection Module. Ensures single source of truth for 
     engine state (sliding windows, sync queues).

 [FORMAL COMPLEXITY PROOF]
   → Injection Complexity: O(1) amortized.
   → Space: O(1) for module definition.

 [FAILURE & EDGE CASE ANALYSIS]
   → Component Failure: Hilt ensures compile-time safety for dependency 
     resolution, preventing runtime engine unavailability.

 [BUSINESS & SUSTAINABILITY UTILITY]
   → Modular architecture allows hot-swapping algorithmic strategies 
     as DEFRA regulations evolve.
 ═══════════════════════════════════════════════════════
*/
package com.ecotrack.enterprise.di

import com.ecotrack.enterprise.service.EcoRouteOptimizer
import com.ecotrack.enterprise.service.SyncSchedulerService
import com.ecotrack.enterprise.service.TrackingEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {

    @Provides
    @Singleton
    fun provideTrackingEngine(): TrackingEngine = TrackingEngine()

    @Provides
    @Singleton
    fun provideEcoRouteOptimizer(): EcoRouteOptimizer = EcoRouteOptimizer()

    @Provides
    @Singleton
    fun provideSyncSchedulerService(): SyncSchedulerService = SyncSchedulerService()
}
