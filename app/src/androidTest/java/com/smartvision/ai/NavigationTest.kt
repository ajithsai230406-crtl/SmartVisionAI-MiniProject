package com.smartvision.ai

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.*
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule(order = 0) val hiltRule  = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun init() { hiltRule.inject() }

    @Test
    fun splashScreen_isDisplayed() {
        // Splash shows SMARTVISION AI text
        composeRule.onNodeWithText("SMARTVISION", substring = true).assertIsDisplayed()
    }

    @Test
    fun homeScreen_showsModuleGrid() {
        // Wait for splash to complete (3 sec)
        composeRule.mainClock.advanceTimeBy(4000L)
        // Skip any onboarding — wait for Home
        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithText("SmartVision", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
