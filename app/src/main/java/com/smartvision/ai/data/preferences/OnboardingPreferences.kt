package com.smartvision.ai.data.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_FIRST_RUN = "isFirstRun"
    }

    private val _onboardingCompletedFlow = MutableStateFlow(!isFirstRun)
    val onboardingCompletedFlow: Flow<Boolean> = _onboardingCompletedFlow.asStateFlow()

    var isFirstRun: Boolean
        get() = prefs.getBoolean(KEY_IS_FIRST_RUN, true)
        set(value) {
            prefs.edit().putBoolean(KEY_IS_FIRST_RUN, value).apply()
            _onboardingCompletedFlow.value = !value
        }
}
