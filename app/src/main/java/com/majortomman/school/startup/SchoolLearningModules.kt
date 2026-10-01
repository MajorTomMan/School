package com.majortomman.school.startup

import com.majortomman.school.learning.activity.ActivityCapabilityKeys
import com.majortomman.school.learning.activity.CoreTextAnswerActivityHandler
import com.majortomman.school.learning.activity.SchoolActivityRuntimeCatalog
import com.majortomman.school.learning.activity.math.PlaceOnNumberLineActivityHandler
import com.majortomman.school.learning.capability.CapabilityDescriptor
import com.majortomman.school.learning.capability.CapabilityKind
import com.majortomman.school.learning.capability.SchoolCapabilityCatalog
import com.majortomman.school.learning.capability.SubjectId
import com.majortomman.school.learning.capability.SubjectModule
import com.majortomman.school.learning.cloud.CoreTextAnswerActivityDecoder
import com.majortomman.school.learning.cloud.CourseActivitySpecCatalog
import com.majortomman.school.learning.cloud.PlaceOnNumberLineActivityDecoder
import com.majortomman.school.visualization.SchoolVisualizationCatalog
import com.majortomman.school.visualization.renderers.math.MathematicsVisualizationModule

object SchoolLearningModules {
    fun install() {
        MathematicsVisualizationModule.install()

        SchoolActivityRuntimeCatalog.install(CoreTextAnswerActivityHandler)
        SchoolActivityRuntimeCatalog.install(PlaceOnNumberLineActivityHandler)
        CourseActivitySpecCatalog.install(CoreTextAnswerActivityDecoder)
        CourseActivitySpecCatalog.install(PlaceOnNumberLineActivityDecoder)

        SchoolCapabilityCatalog.install(CoreSubjectModule)
        SchoolCapabilityCatalog.install(
            MathematicsSubjectModule(
                visualizations = SchoolVisualizationCatalog.registeredCapabilities()
                    .filterKeys { it.value.startsWith("mathematics.") }
                    .mapKeys { (key, _) -> key.value }
                    .toSortedMap(),
            ),
        )
    }
}

private object CoreSubjectModule : SubjectModule {
    override val id = SubjectId("core")
    override val capabilities = listOf(
        CapabilityDescriptor(
            key = ActivityCapabilityKeys.TEXT_ANSWER,
            subject = id,
            kind = CapabilityKind.ACTIVITY,
            schemaVersion = 1,
        ),
    )
}

private data class MathematicsSubjectModule(
    val visualizations: Map<String, Int>,
) : SubjectModule {
    override val id = SubjectId("mathematics")
    override val capabilities = buildList {
        add(
            CapabilityDescriptor(
                key = ActivityCapabilityKeys.PLACE_ON_NUMBER_LINE,
                subject = id,
                kind = CapabilityKind.ACTIVITY,
                schemaVersion = 1,
            ),
        )
        visualizations.forEach { (key, schemaVersion) ->
            add(
                CapabilityDescriptor(
                    key = com.majortomman.school.learning.capability.CapabilityKey(key),
                    subject = id,
                    kind = CapabilityKind.VISUALIZATION,
                    schemaVersion = schemaVersion,
                ),
            )
        }
    }
}
