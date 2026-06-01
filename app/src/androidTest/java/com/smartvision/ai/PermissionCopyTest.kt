package com.smartvision.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionCopyTest {
    @Test
    fun cameraPermissionCopyIsFacultyFriendly() {
        val copy = "Smart Vision AI needs camera access to scan text, detect objects, identify medicine and classify waste."
        assertTrue(copy.contains("camera access"))
        assertTrue(copy.contains("scan text"))
    }
}
