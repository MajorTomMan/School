package com.majortomman.school.learning.cloud

import com.majortomman.school.learning.activity.ActivityCapabilityKeys
import com.majortomman.school.learning.activity.ActivityId
import com.majortomman.school.learning.activity.ActivitySpec
import com.majortomman.school.learning.activity.PlaceOnNumberLineActivitySpec
import com.majortomman.school.learning.capability.CapabilityKey
import org.json.JSONObject

internal object PlaceOnNumberLineActivityDecoder : CourseActivitySpecDecoder {
    override val capability: CapabilityKey = ActivityCapabilityKeys.PLACE_ON_NUMBER_LINE
    override val schemaVersion: Int = 1

    override fun decode(id: ActivityId, parameters: JSONObject): ActivitySpec {
        parameters.requireActivityShape(required = setOf("min", "max", "step", "initialValue"))
        return PlaceOnNumberLineActivitySpec(
            id = id,
            min = parameters.activityNumber("min"),
            max = parameters.activityNumber("max"),
            step = parameters.activityNumber("step"),
            initialValue = parameters.activityNumber("initialValue"),
        )
    }
}

