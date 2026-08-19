package com.majortomman.school.data.material

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class EducationStageModelsTest {
    @Test
    fun legacySlotWithoutStageIsAssignedFromGrade() {
        val legacy = JSONObject().put("subjectId", "math").put("subjectTitle", "subject").put("grade", 7).put("volume", 1)
        val slot = TextbookSlot.fromJson(legacy)

        assertEquals(EducationStage.JUNIOR_HIGH, slot.stage)
        assertEquals("math-7-1", slot.key)
    }

    @Test
    fun stageIsDerivedFromGradeRange() {
        val senior = TextbookSlot("math", "subject", 10, TextbookVolume.FIRST)
        val university = TextbookSlot("computer", "subject", 14, TextbookVolume.SECOND)

        assertEquals(EducationStage.SENIOR_HIGH, senior.stage)
        assertEquals(EducationStage.UNIVERSITY, university.stage)
    }

    @Test
    fun stageSurvivesJsonRoundTrip() {
        val original = TextbookSlot(subjectId = "physics", subjectTitle = "subject", grade = 11, volume = TextbookVolume.SECOND, stage = EducationStage.SENIOR_HIGH)
        val restored = TextbookSlot.fromJson(original.toJson())

        assertEquals(original, restored)
    }
}
