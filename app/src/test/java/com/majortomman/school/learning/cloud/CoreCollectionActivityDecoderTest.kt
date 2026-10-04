package com.majortomman.school.learning.cloud

import com.majortomman.school.learning.activity.ActivityId
import com.majortomman.school.learning.activity.MatchActivitySpec
import com.majortomman.school.learning.activity.OrderActivitySpec
import com.majortomman.school.learning.activity.SelectOneActivitySpec
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CoreCollectionActivityDecoderTest {
    @Test
    fun decodesSelectOrderAndMatchParameters() {
        val select = SelectOneActivityDecoder.decode(
            ActivityId("select"),
            JSONObject("""{"options":[{"id":"a","label":"甲"},{"id":"b","label":"乙"}]}"""),
        ) as SelectOneActivitySpec
        val order = OrderActivityDecoder.decode(
            ActivityId("order"),
            JSONObject("""{"items":[{"id":"a","label":"第一"},{"id":"b","label":"第二"}]}"""),
        ) as OrderActivitySpec
        val match = MatchActivityDecoder.decode(
            ActivityId("match"),
            JSONObject("""{"left":[{"id":"l1","label":"左一"},{"id":"l2","label":"左二"}],"right":[{"id":"r1","label":"右一"},{"id":"r2","label":"右二"}]}"""),
        ) as MatchActivitySpec

        assertEquals(listOf("a", "b"), select.options.map { it.id })
        assertEquals(listOf("a", "b"), order.items.map { it.id })
        assertEquals(listOf("l1", "l2"), match.leftItems.map { it.id })
        assertEquals(listOf("r1", "r2"), match.rightItems.map { it.id })
    }

    @Test
    fun decoderRejectsUnknownOptionFields() {
        assertThrows(IllegalArgumentException::class.java) {
            SelectOneActivityDecoder.decode(
                ActivityId("select"),
                JSONObject("""{"options":[{"id":"a","label":"甲","extra":true},{"id":"b","label":"乙"}]}"""),
            )
        }
    }
}
