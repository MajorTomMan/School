package com.majortomman.school.ui.visualization

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.majortomman.school.ui.visualization.core.SubjectVisualizationRenderer
import com.majortomman.school.ui.visualization.core.VisualizationArguments
import com.majortomman.school.ui.visualization.core.VisualizationKey
import com.majortomman.school.ui.visualization.core.VisualizationRegistry
import com.majortomman.school.ui.visualization.core.VisualizationSubject
import com.majortomman.school.ui.visualization.subjects.math.AccountTrendVisualizationRenderer
import com.majortomman.school.ui.visualization.subjects.math.GrowthRateTrendVisualizationRenderer
import com.majortomman.school.ui.visualization.subjects.math.PartToleranceVisualizationRenderer

/**
 * One module per subject. New scenes inherit their subject renderer parent and are added to the matching module.
 * Keeping the catalog explicit avoids reflection, makes startup deterministic and lets tests detect duplicate keys.
 */
abstract class SubjectVisualizationModule {
    abstract val subject: VisualizationSubject
    abstract val renderers: List<SubjectVisualizationRenderer>

    fun validatedRenderers(): List<SubjectVisualizationRenderer> {
        require(renderers.all { it.subject == subject }) {
            "${subject.label}模块包含了其他学科的可视化"
        }
        return renderers
    }
}

object MathematicsVisualizationModule : SubjectVisualizationModule() {
    override val subject: VisualizationSubject = VisualizationSubject.MATHEMATICS
    override val renderers: List<SubjectVisualizationRenderer> = listOf(
        AccountTrendVisualizationRenderer,
        GrowthRateTrendVisualizationRenderer,
        PartToleranceVisualizationRenderer,
    )
}

object PhysicsVisualizationModule : SubjectVisualizationModule() {
    override val subject: VisualizationSubject = VisualizationSubject.PHYSICS
    override val renderers: List<SubjectVisualizationRenderer> = emptyList()
}

object ChemistryVisualizationModule : SubjectVisualizationModule() {
    override val subject: VisualizationSubject = VisualizationSubject.CHEMISTRY
    override val renderers: List<SubjectVisualizationRenderer> = emptyList()
}

object BiologyVisualizationModule : SubjectVisualizationModule() {
    override val subject: VisualizationSubject = VisualizationSubject.BIOLOGY
    override val renderers: List<SubjectVisualizationRenderer> = emptyList()
}

object GeneralVisualizationModule : SubjectVisualizationModule() {
    override val subject: VisualizationSubject = VisualizationSubject.GENERAL
    override val renderers: List<SubjectVisualizationRenderer> = emptyList()
}

object SchoolVisualizationCatalog {
    private val modules: List<SubjectVisualizationModule> = listOf(
        MathematicsVisualizationModule,
        PhysicsVisualizationModule,
        ChemistryVisualizationModule,
        BiologyVisualizationModule,
        GeneralVisualizationModule,
    )
    private val registry = VisualizationRegistry(modules.flatMap { it.validatedRenderers() })

    fun registeredKeys(): Set<VisualizationKey> = registry.registeredKeys()

    @Composable
    fun Render(
        key: VisualizationKey,
        arguments: VisualizationArguments,
        modifier: Modifier = Modifier,
    ) {
        registry.Render(key = key, arguments = arguments, modifier = modifier)
    }
}
