package com.majortomman.school.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePushTest {
    @Test
    fun newerSchoolUpdateSchedulesFreshManifestCheck() {
        assertTrue(
            shouldSchedulePushCheck(
                type = "school_update",
                advertisedVersion = 100300,
                currentVersion = 100299,
                autoCheck = true,
            ),
        )
    }

    @Test
    fun staleOrDisabledPushIsIgnored() {
        assertFalse(shouldSchedulePushCheck("school_update", 100299, 100299, true))
        assertFalse(shouldSchedulePushCheck("other", 100300, 100299, true))
        assertFalse(shouldSchedulePushCheck("school_update", 100300, 100299, false))
    }

    @Test
    fun missingAdvertisedVersionStillPerformsSignedManifestCheck() {
        assertTrue(shouldSchedulePushCheck("school_update", null, 100299, true))
    }
}
