package com.majortomman.school.learning.cloud

import java.net.HttpURLConnection
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseManifestAvailabilityTest {
    @Test
    fun missingOrGoneManifestMeansNotPublished() {
        assertTrue(CourseSyncManager.isManifestNotPublishedResponse(HttpURLConnection.HTTP_NOT_FOUND))
        assertTrue(CourseSyncManager.isManifestNotPublishedResponse(HttpURLConnection.HTTP_GONE))
    }

    @Test
    fun successAndServerErrorsAreNotClassifiedAsNotPublished() {
        assertFalse(CourseSyncManager.isManifestNotPublishedResponse(HttpURLConnection.HTTP_OK))
        assertFalse(CourseSyncManager.isManifestNotPublishedResponse(HttpURLConnection.HTTP_INTERNAL_ERROR))
        assertFalse(CourseSyncManager.isManifestNotPublishedResponse(HttpURLConnection.HTTP_UNAVAILABLE))
    }
}
