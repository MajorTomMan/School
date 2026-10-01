package com.majortomman.school.learning.cloud

import com.majortomman.school.learning.activity.ActivityCapabilityKeys
import com.majortomman.school.learning.activity.ActivityId
import com.majortomman.school.learning.activity.ActivitySpec
import com.majortomman.school.learning.activity.TextAnswerActivitySpec
import com.majortomman.school.learning.capability.CapabilityKey
import org.json.JSONObject

internal object CoreTextAnswerActivityDecoder : CourseActivitySpecDecoder {
    override val capability: CapabilityKey = ActivityCapabilityKeys.TEXT_ANSWER
    override val schemaVersion: Int = 1

    override fun decode(id: ActivityId, parameters: JSONObject): ActivitySpec {
        parameters.requireActivityShape(optional = setOf("placeholder"))
        return TextAnswerActivitySpec(id, parameters.optionalActivityText("placeholder"))
    }
}
