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

    private val sharedPrefs = context.getSharedPreferences("sv_session_prefs", Context.MODE_PRIVATE)

    private val _isGuestLoggedIn = MutableStateFlow(
        sharedPrefs.getBoolean("is_guest", false) ||
        (try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.isAnonymous == true
        } catch (_: Exception) {
            false
        })
    )
    val isGuestLoggedIn: StateFlow<Boolean> = _isGuestLoggedIn.asStateFlow()

    val onboardingCompleted: Flow<Boolean> = context.sessionDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.ONBOARDING_COMPLETED] ?: false }

    val isLoggedIn: Flow<Boolean> = combine(
        context.sessionDataStore.data
            .catch { emit(emptyPreferences()) }
            .map { it[Keys.IS_LOGGED_IN] ?: false },
        _isGuestLoggedIn
    ) { dbLoggedIn, guestLoggedIn ->
        dbLoggedIn || guestLoggedIn
    }

    val userProfile: Flow<UserProfile?> = combine(
        context.sessionDataStore.data
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
            },
        _isGuestLoggedIn
    ) { dbProfile, guestLoggedIn ->
        if (guestLoggedIn) {
            UserProfile(
                name = "Guest User",
                email = "guest@smartvision.ai",
                photoUrl = null
            )
        } else {
            dbProfile
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.sessionDataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun saveSession(profile: UserProfile, isGuest: Boolean) {
        sharedPrefs.edit().putBoolean("is_guest", isGuest).apply()
        if (isGuest) {
            _isGuestLoggedIn.value = true
            context.sessionDataStore.edit { prefs ->
                prefs[Keys.IS_LOGGED_IN] = false
                prefs.remove(Keys.USER_NAME)
                prefs.remove(Keys.USER_EMAIL)
                prefs.remove(Keys.USER_PHOTO_URL)
            }
        } else {
            _isGuestLoggedIn.value = false
            context.sessionDataStore.edit { prefs ->
                prefs[Keys.IS_LOGGED_IN] = true
                prefs[Keys.USER_NAME] = profile.name
                prefs[Keys.USER_EMAIL] = profile.email
                profile.photoUrl?.let { prefs[Keys.USER_PHOTO_URL] = it } ?: prefs.remove(Keys.USER_PHOTO_URL)
            }
        }
    }

    suspend fun clearSession() {
        sharedPrefs.edit().putBoolean("is_guest", false).apply()
        _isGuestLoggedIn.value = false
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = false
            prefs.remove(Keys.USER_NAME)
            prefs.remove(Keys.USER_EMAIL)
            prefs.remove(Keys.USER_PHOTO_URL)
        }
    }
}
