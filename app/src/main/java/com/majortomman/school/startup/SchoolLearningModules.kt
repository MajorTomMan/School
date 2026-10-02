package com.majortomman.school.startup

import com.majortomman.school.learning.activity.ActivityRuntimeHandler
import com.majortomman.school.learning.activity.CoreTextAnswerActivityHandler
import com.majortomman.school.learning.activity.SchoolActivityRuntimeCatalog
import com.majortomman.school.learning.activity.math.PlaceOnNumberLineActivityHandler
import com.majortomman.school.learning.cloud.CoreTextAnswerActivityDecoder
import com.majortomman.school.learning.cloud.CourseActivitySpecCatalog
import com.majortomman.school.learning.cloud.CourseActivitySpecDecoder
import com.majortomman.school.learning.cloud.PlaceOnNumberLineActivityDecoder
import com.majortomman.school.ui.ActivityUiHost
import com.majortomman.school.ui.CoreTextAnswerActivityUiHost
import com.majortomman.school.ui.PlaceOnNumberLineActivityUiHost
import com.majortomman.school.ui.SchoolActivityUiCatalog
import com.majortomman.school.visualization.renderers.math.MathematicsVisualizationModule

object SchoolLearningModules {
    fun install() {
        MathematicsVisualizationModule.install()
        installActivity(CoreTextAnswerActivityHandler, CoreTextAnswerActivityDecoder, CoreTextAnswerActivityUiHost)
        installActivity(PlaceOnNumberLineActivityHandler, PlaceOnNumberLineActivityDecoder, PlaceOnNumberLineActivityUiHost)
    }

    private fun installActivity(
        handler: ActivityRuntimeHandler,
        decoder: CourseActivitySpecDecoder,
        host: ActivityUiHost,
    ) {
        require(handler.capability == decoder.capability) {
            "Activity runtime/decoder capability 不一致：runtime=${handler.capability}, decoder=${decoder.capability}"
        }
        require(handler.schemaVersion == decoder.schemaVersion) {
            "Activity ${handler.capability} runtime/decoder schemaVersion 不一致：runtime=${handler.schemaVersion}, decoder=${decoder.schemaVersion}"
        }
        require(handler.capability == host.capability) {
            "Activity runtime/UI capability 不一致：runtime=${handler.capability}, ui=${host.capability}"
        }
        require(handler.schemaVersion == host.schemaVersion) {
            "Activity ${handler.capability} runtime/UI schemaVersion 不一致：runtime=${handler.schemaVersion}, ui=${host.schemaVersion}"
        }
        SchoolActivityRuntimeCatalog.install(handler)
        CourseActivitySpecCatalog.install(decoder)
        SchoolActivityUiCatalog.install(host)
    }
}
