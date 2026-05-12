package com.ecotrack.enterprise.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = "https://xamrcjcqrgixqdpxdrol.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhhbXJjamNxcmdpeHFkcHhkcm9sIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzg0MDA0NTIsImV4cCI6MjA5Mzk3NjQ1Mn0.7Wfu1j70Q-AFsNoonZVJLmb_WI23l3UgVNYuILVDTpk"
        ) {
            install(Postgrest)
            install(Auth)
        }
    }
}
