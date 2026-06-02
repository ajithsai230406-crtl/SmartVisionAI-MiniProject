package com.smartvision.ai.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.smartvision.ai.domain.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore("sv_session")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_PHOTO_URL = stringPreferencesKey("user_photo_url")
    }

    val onboardingCompleted: Flow<Boolean> = context.sessionDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.ONBOARDING_COMPLETED] ?: false }

    val isLoggedIn: Flow<Boolean> = context.sessionDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.IS_LOGGED_IN] ?: false }

    val userProfile: Flow<UserProfile?> = context.sessionDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val loggedIn = prefs[Keys.IS_LOGGED_IN] ?: false
            if (loggedIn) {
                UserProfile(
                    name = prefs[Keys.USER_NAME] ?: "Ajith Kumar",
                    email = prefs[Keys.USER_EMAIL] ?: "Premium AI user",
                    photoUrl = prefs[Keys.USER_PHOTO_URL]
                )
            } else {
                null
            }
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.sessionDataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun saveSession(profile: UserProfile) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = true
            prefs[Keys.USER_NAME] = profile.name
            prefs[Keys.USER_EMAIL] = profile.email
            profile.photoUrl?.let { prefs[Keys.USER_PHOTO_URL] = it } ?: prefs.remove(Keys.USER_PHOTO_URL)
        }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = false
            prefs.remove(Keys.USER_NAME)
            prefs.remove(Keys.USER_EMAIL)
            prefs.remove(Keys.USER_PHOTO_URL)
        }
    }
}
