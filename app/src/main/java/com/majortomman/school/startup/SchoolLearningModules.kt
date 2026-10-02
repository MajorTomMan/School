package com.majortomman.school.startup

import com.majortomman.school.learning.activity.CoreTextAnswerActivityHandler
import com.majortomman.school.learning.activity.SchoolActivityRuntimeCatalog
import com.majortomman.school.learning.activity.math.PlaceOnNumberLineActivityHandler
import com.majortomman.school.learning.cloud.CoreTextAnswerActivityDecoder
import com.majortomman.school.learning.cloud.CourseActivitySpecCatalog
import com.majortomman.school.learning.cloud.PlaceOnNumberLineActivityDecoder
import com.majortomman.school.visualization.renderers.math.MathematicsVisualizationModule

object SchoolLearningModules {
    fun install() {
        MathematicsVisualizationModule.install()

        SchoolActivityRuntimeCatalog.install(CoreTextAnswerActivityHandler)
        SchoolActivityRuntimeCatalog.install(PlaceOnNumberLineActivityHandler)

        CourseActivitySpecCatalog.install(CoreTextAnswerActivityDecoder)
        CourseActivitySpecCatalog.install(PlaceOnNumberLineActivityDecoder)
    }
}
