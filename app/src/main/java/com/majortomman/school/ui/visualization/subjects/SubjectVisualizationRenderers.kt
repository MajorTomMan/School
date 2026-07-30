package com.majortomman.school.ui.visualization.subjects

import com.majortomman.school.ui.visualization.core.SubjectVisualizationRenderer
import com.majortomman.school.ui.visualization.core.VisualizationSubject

/** Base class for mathematical diagrams, functions, number lines, statistics and geometric models. */
abstract class MathematicsVisualizationRenderer : SubjectVisualizationRenderer() {
    final override val subject: VisualizationSubject = VisualizationSubject.MATHEMATICS
}

/** Base class for mechanics, optics, electricity, waves, thermodynamics and experiment simulations. */
abstract class PhysicsVisualizationRenderer : SubjectVisualizationRenderer() {
    final override val subject: VisualizationSubject = VisualizationSubject.PHYSICS
}

/** Base class for particles, molecules, apparatus, reactions, solutions and quantitative chemistry. */
abstract class ChemistryVisualizationRenderer : SubjectVisualizationRenderer() {
    final override val subject: VisualizationSubject = VisualizationSubject.CHEMISTRY
}

/** Base class for cells, anatomy, ecology, genetics, evolution and biological experiment models. */
abstract class BiologyVisualizationRenderer : SubjectVisualizationRenderer() {
    final override val subject: VisualizationSubject = VisualizationSubject.BIOLOGY
}

/** Fallback base for visualizations shared by more than one subject. */
abstract class GeneralVisualizationRenderer : SubjectVisualizationRenderer() {
    final override val subject: VisualizationSubject = VisualizationSubject.GENERAL
}
