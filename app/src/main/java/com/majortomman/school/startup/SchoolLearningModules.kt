package com.majortomman.school.startup

import com.majortomman.school.learning.activity.ActivityRuntimeHandler
import com.majortomman.school.learning.activity.CoreTextAnswerActivityHandler
import com.majortomman.school.learning.activity.SchoolActivityRuntimeCatalog
import com.majortomman.school.learning.activity.math.PlaceOnNumberLineActivityHandler
import com.majortomman.school.learning.cloud.CoreTextAnswerActivityDecoder
import com.majortomman.school.learning.cloud.CourseActivitySpecCatalog
import com.majortomman.school.learning.cloud.CourseActivitySpecDecoder
import com.majortomman.school.learning.cloud.PlaceOnNumberLineActivityDecoder
import com.majortomman.school.visualization.renderers.math.MathematicsVisualizationModule

object SchoolLearningModules {
    fun install() {
        MathematicsVisualizationModule.install()
        installActivity(CoreTextAnswerActivityHandler, CoreTextAnswerActivityDecoder)
        installActivity(PlaceOnNumberLineActivityHandler, PlaceOnNumberLineActivityDecoder)
    }

    private fun installActivity(
        handler: ActivityRuntimeHandler,
        decoder: CourseActivitySpecDecoder,
    ) {
        require(handler.capability == decoder.capability) {
            "Activity runtime/decoder capability 不一致：runtime=${handler.capability}, decoder=${decoder.capability}"
        }
        require(handler.schemaVersion == decoder.schemaVersion) {
            "Activity ${handler.capability} runtime/decoder schemaVersion 不一致：runtime=${handler.schemaVersion}, decoder=${decoder.schemaVersion}"
        }
        SchoolActivityRuntimeCatalog.install(handler)
        CourseActivitySpecCatalog.install(decoder)
    }
}
