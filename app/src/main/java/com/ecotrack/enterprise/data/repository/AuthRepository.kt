package com.ecotrack.enterprise.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    suspend fun signIn(email: String, password: String): Boolean {
        return try {
            supabaseClient.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun signUp(email: String, password: String): Boolean {
        return try {
            supabaseClient.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun signOut() {
        try {
            supabaseClient.auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isUserLoggedIn(): Boolean {
        return supabaseClient.auth.currentSessionOrNull() != null
    }
}
