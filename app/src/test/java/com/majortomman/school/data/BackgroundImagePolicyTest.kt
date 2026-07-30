package com.majortomman.school.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackgroundImagePolicyTest {
    @Test
    fun acceptsSupportedPortraitImage() {
        assertNull(
            BackgroundImagePolicy.validate(
                width = 1080,
                height = 2400,
                fileSize = 4L * 1024L * 1024L,
                declaredMimeType = "image/jpeg",
                decodedMimeType = "image/jpeg",
            ),
        )
    }

    @Test
    fun acceptsSupportedLandscapeImage() {
        assertNull(
            BackgroundImagePolicy.validate(
                width = 1920,
                height = 1080,
                fileSize = 5L * 1024L * 1024L,
                declaredMimeType = "image/png",
                decodedMimeType = "image/png",
            ),
        )
    }

    @Test
    fun rejectsResolutionBelowMinimum() {
        assertEquals(
            "图片分辨率过低，至少需要短边 720px、长边 1280px",
            BackgroundImagePolicy.validate(
                width = 719,
                height = 1280,
                fileSize = 1L,
                declaredMimeType = "image/webp",
                decodedMimeType = "image/webp",
            ),
        )
    }

    @Test
    fun rejectsExcessivePixelCount() {
        assertEquals(
            "图片像素总量过高，请使用不超过 3200 万像素的图片",
            BackgroundImagePolicy.validate(
                width = 8000,
                height = 5000,
                fileSize = 10L * 1024L * 1024L,
                declaredMimeType = "image/jpeg",
                decodedMimeType = "image/jpeg",
            ),
        )
    }

    @Test
    fun rejectsOversizedFile() {
        assertEquals(
            "图片文件过大，请选择不超过 20 MB 的图片",
            BackgroundImagePolicy.validate(
                width = 1080,
                height = 1920,
                fileSize = 21L * 1024L * 1024L,
                declaredMimeType = "image/png",
                decodedMimeType = "image/png",
            ),
        )
    }

    @Test
    fun rejectsUnsupportedDecodedFormat() {
        assertEquals(
            "仅支持 JPEG、PNG 和 WebP 图片",
            BackgroundImagePolicy.validate(
                width = 1080,
                height = 1920,
                fileSize = 1L,
                declaredMimeType = "image/gif",
                decodedMimeType = "image/gif",
            ),
        )
    }

    @Test
    fun rejectsDeclaredAndActualMimeMismatch() {
        assertEquals(
            "图片扩展格式与实际文件内容不一致",
            BackgroundImagePolicy.validate(
                width = 1080,
                height = 1920,
                fileSize = 1L,
                declaredMimeType = "image/png",
                decodedMimeType = "image/jpeg",
            ),
        )
    }
}
