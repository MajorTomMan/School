package com.majortomman.school.data.curriculum

import com.majortomman.school.data.material.EducationStage
import com.majortomman.school.data.material.InstalledTextbook
import com.majortomman.school.data.material.gradeLabel
import java.io.File

internal object MaterialCurriculumGraphBuilder {
    fun build(textbooks: List<InstalledTextbook>): CurriculumRepository.MaterialGraph {
        val subjects = linkedMapOf<String, SubjectDefinition>()
        val curricula = linkedMapOf<String, Curriculum>()
        val nodes = linkedMapOf<String, CurriculumNode>()
        val knowledge = linkedMapOf<String, KnowledgePoint>()
        val refs = linkedMapOf<String, NodeKnowledgeRef>()
        val resources = linkedMapOf<String, LearningResource>()
        val bindings = linkedMapOf<String, ResourceBinding>()
        val relations = linkedMapOf<String, KnowledgeRelation>()

        textbooks.sortedWith(compareBy({ it.slot.subjectId }, { it.slot.stage.ordinal }, { it.slot.grade }, { it.slot.volume.id }))
            .forEachIndexed { textbookIndex, textbook ->
                val slot = textbook.slot
                val subject = BuiltinCurriculumCatalog.subject(slot.subjectId, slot.subjectTitle)
                if (slot.subjectId !in BuiltinCurriculumCatalog.subjectById) subjects[subject.id] = subject

                val curriculumId = curriculumIdFor(slot.subjectId, slot.stage)
                curricula.putIfAbsent(
                    curriculumId,
                    Curriculum(
                        id = curriculumId,
                        subjectId = slot.subjectId,
                        title = "${slot.stage.label}${subject.title}课程",
                        levelSystemId = if (slot.stage == EducationStage.UNIVERSITY) {
                            BuiltinCurriculumCatalog.UNIVERSITY_LEVEL_SYSTEM
                        } else {
                            BuiltinCurriculumCatalog.BASIC_EDUCATION_LEVEL_SYSTEM
                        },
                        standard = "School 可扩展课程树",
                        region = "CN",
                        version = MATERIAL_CURRICULUM_VERSION,
                        source = CurriculumSource.MATERIAL,
                        orderIndex = subject.orderIndex,
                    ),
                )

                val rootId = "$curriculumId:root"
                nodes.putIfAbsent(
                    rootId,
                    CurriculumNode(rootId, curriculumId, null, CurriculumNodeType.ROOT, curricula.getValue(curriculumId).title, 0),
                )
                val levelId = "$curriculumId:level:${slot.grade}"
                nodes.putIfAbsent(
                    levelId,
                    CurriculumNode(
                        id = levelId,
                        curriculumId = curriculumId,
                        parentId = rootId,
                        type = CurriculumNodeType.LEVEL,
                        title = gradeLabel(slot.grade),
                        orderIndex = slot.grade,
                        metadata = mapOf("legacyGrade" to slot.grade.toString(), "stage" to slot.stage.id),
                    ),
                )
                val termId = "$curriculumId:term:${slot.grade}:${slot.volume.id}"
                nodes.putIfAbsent(
                    termId,
                    CurriculumNode(
                        id = termId,
                        curriculumId = curriculumId,
                        parentId = levelId,
                        type = CurriculumNodeType.TERM,
                        title = slot.volumeLabel,
                        orderIndex = slot.volume.id,
                        metadata = mapOf("legacyVolume" to slot.volume.id.toString()),
                    ),
                )
                val courseId = "$curriculumId:course:${slot.key}"
                val pdfBound = textbook.pack.pdfFile.isFile
                nodes[courseId] = CurriculumNode(
                    id = courseId,
                    curriculumId = curriculumId,
                    parentId = termId,
                    type = CurriculumNodeType.COURSE,
                    title = textbook.pack.manifest.title,
                    orderIndex = textbookIndex,
                    metadata = mapOf(
                        "legacyTextbookKey" to slot.key,
                        "pdfBound" to pdfBound.toString(),
                        "pageCount" to textbook.pageCount.toString(),
                    ),
                )

                val resourceId = "material:${slot.key}"
                resources[resourceId] = LearningResource(
                    id = resourceId,
                    subjectId = slot.subjectId,
                    type = if (pdfBound) ResourceType.TEXTBOOK_PDF else ResourceType.PREBUILT_COURSE,
                    title = textbook.pack.manifest.title,
                    uri = textbook.pack.pdfFile.takeIf(File::isFile)?.toURI()?.toString(),
                    publisher = textbookPublisher(textbook),
                    edition = textbookEdition(textbook),
                    legacyTextbookKey = slot.key,
                    metadata = mapOf(
                        "version" to textbook.pack.manifest.version,
                        "sha256" to textbook.pack.manifest.pdf.sha256,
                        "pageIndexOffset" to textbook.pack.manifest.pdf.pageIndexOffset.toString(),
                    ),
                )
                bindings["$resourceId:$courseId"] = ResourceBinding(
                    id = "$resourceId:$courseId",
                    resourceId = resourceId,
                    nodeId = courseId,
                    knowledgePointId = null,
                    role = ResourceBindingRole.PRIMARY,
                    pageStart = null,
                    pageEnd = null,
                    orderIndex = 0,
                )

                var previousPoint: KnowledgePoint? = null
                textbook.lessons.sortedWith(compareBy({ it.orderIndex }, { it.pageStart }, { it.title }))
                    .forEachIndexed { lessonIndex, lesson ->
                        var parentId = courseId
                        var pathIdentity = ""
                        lesson.path.forEach { pathNode ->
                            pathIdentity += "/${pathNode.id.ifBlank { pathNode.title }}"
                            val stablePathId = BuiltinCurriculumCatalog.stableId(pathIdentity)
                            val pathId = "$curriculumId:path:${slot.key}:$stablePathId"
                            nodes.putIfAbsent(
                                pathId,
                                CurriculumNode(
                                    id = pathId,
                                    curriculumId = curriculumId,
                                    parentId = parentId,
                                    type = pathNode.type.toCurriculumNodeType(),
                                    title = pathNode.title,
                                    orderIndex = pathNode.orderIndex,
                                    metadata = mapOf(
                                        "sourcePathId" to pathNode.id,
                                        "prebuiltHierarchy" to "true",
                                    ),
                                ),
                            )
                            parentId = pathId
                        }

                        val lessonNodeId = "$curriculumId:lesson:${slot.key}:${BuiltinCurriculumCatalog.stableId(lesson.sourceId)}"
                        val leafType = if (lesson.path.lastOrNull()?.type.equals("LESSON", ignoreCase = true)) {
                            CurriculumNodeType.TOPIC
                        } else {
                            CurriculumNodeType.LESSON
                        }
                        nodes[lessonNodeId] = CurriculumNode(
                            id = lessonNodeId,
                            curriculumId = curriculumId,
                            parentId = parentId,
                            type = leafType,
                            title = lesson.title,
                            orderIndex = lesson.orderIndex.takeIf { it != 0 } ?: lessonIndex,
                            legacyLessonId = lesson.id,
                            metadata = mapOf(
                                "sourceId" to lesson.sourceId,
                                "pageStart" to lesson.pageStart.toString(),
                                "pageEnd" to lesson.pageEnd.toString(),
                                "estimatedMinutes" to lesson.estimatedMinutes.toString(),
                                "learningRole" to lesson.role,
                            ),
                        )
                        val point = BuiltinCurriculumCatalog.inferKnowledge(slot.subjectId, lesson.title)
                        if (point.id !in BuiltinCurriculumCatalog.knowledgeById) knowledge.putIfAbsent(point.id, point)
                        refs["$lessonNodeId:${point.id}"] = NodeKnowledgeRef(
                            nodeId = lessonNodeId,
                            knowledgePointId = point.id,
                            role = KnowledgeRole.PRIMARY,
                            orderIndex = 0,
                        )
                        bindings["$resourceId:$lessonNodeId"] = ResourceBinding(
                            id = "$resourceId:$lessonNodeId",
                            resourceId = resourceId,
                            nodeId = lessonNodeId,
                            knowledgePointId = point.id,
                            role = ResourceBindingRole.EVIDENCE,
                            pageStart = lesson.pageStart,
                            pageEnd = lesson.pageEnd,
                            orderIndex = lessonIndex,
                        )
                        previousPoint?.takeIf { it.id != point.id }?.let { previous ->
                            val key = "${previous.id}:${point.id}:EXTENDS"
                            relations.putIfAbsent(
                                key,
                                KnowledgeRelation(
                                    fromKnowledgeId = previous.id,
                                    toKnowledgeId = point.id,
                                    type = KnowledgeRelationType.EXTENDS,
                                    weight = 0.35,
                                ),
                            )
                        }
                        previousPoint = point
                    }
            }

        val allKnowledge = (BuiltinCurriculumCatalog.knowledgePoints + knowledge.values).distinctBy(KnowledgePoint::id)
        val snapshot = CurriculumSnapshot(
            subjects = (BuiltinCurriculumCatalog.subjects + subjects.values).distinctBy(SubjectDefinition::id),
            levelSystems = BuiltinCurriculumCatalog.levelSystems,
            levels = BuiltinCurriculumCatalog.levels,
            curricula = curricula.values.toList(),
            nodes = nodes.values.toList(),
            knowledgePoints = allKnowledge,
            knowledgeRelations = (BuiltinCurriculumCatalog.knowledgeRelations + relations.values)
                .distinctBy { "${it.fromKnowledgeId}:${it.toKnowledgeId}:${it.type}" },
            nodeKnowledgeRefs = refs.values.toList(),
            resources = resources.values.toList(),
            resourceBindings = bindings.values.toList(),
        )
        return CurriculumRepository.MaterialGraph(
            snapshot = snapshot,
            additionalSubjects = subjects.values.toList(),
            generatedKnowledge = knowledge.values.toList(),
            materialRelations = relations.values.toList(),
        )
    }

    private fun String.toCurriculumNodeType(): CurriculumNodeType = when (uppercase()) {
        "STAGE" -> CurriculumNodeType.STAGE
        "LEVEL" -> CurriculumNodeType.LEVEL
        "TERM" -> CurriculumNodeType.TERM
        "COURSE" -> CurriculumNodeType.COURSE
        "MODULE" -> CurriculumNodeType.MODULE
        "CHAPTER" -> CurriculumNodeType.CHAPTER
        "LESSON" -> CurriculumNodeType.LESSON
        "TOPIC" -> CurriculumNodeType.TOPIC
        else -> CurriculumNodeType.UNIT
    }

    private fun curriculumIdFor(subjectId: String, stage: EducationStage): String =
        "material:$subjectId:${stage.id}"

    private fun textbookPublisher(textbook: InstalledTextbook): String? = runCatching {
        val catalog = org.json.JSONObject(textbook.pack.catalogFile.readText(Charsets.UTF_8))
        catalog.optJSONObject("book")?.optString("publisher")?.takeIf(String::isNotBlank)
    }.getOrNull()

    private fun textbookEdition(textbook: InstalledTextbook): String? = runCatching {
        val catalog = org.json.JSONObject(textbook.pack.catalogFile.readText(Charsets.UTF_8))
        catalog.optJSONObject("book")?.optString("edition")?.takeIf(String::isNotBlank)
    }.getOrNull()

    private const val MATERIAL_CURRICULUM_VERSION = "material-tree-v2"
}
