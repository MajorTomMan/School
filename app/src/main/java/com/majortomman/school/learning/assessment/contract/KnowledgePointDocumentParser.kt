package com.majortomman.school.learning.assessment.contract

import com.majortomman.school.learning.assessment.domain.KnowledgePointId
import org.json.JSONObject

object KnowledgePointDocumentParser {
    fun decode(raw: String): KnowledgePointDocument = decode(JSONObject(raw))

    fun decode(root: JSONObject): KnowledgePointDocument {
        val location = "knowledge-points"
        root.requireContractShape(
            location,
            required = setOf("courseId", "knowledgePoints"),
        )
        return KnowledgePointDocument(
            courseId = requireCourseIdentifier(
                root.requireContractText("courseId", location),
                "$location.courseId",
            ),
            knowledgePoints = root.requireContractObjects("knowledgePoints", location)
                .mapIndexed(::decodeKnowledgePoint),
        )
    }

    private fun decodeKnowledgePoint(index: Int, json: JSONObject): KnowledgePointDefinition {
        val location = "knowledge-points.knowledgePoints[$index]"
        json.requireContractShape(
            location,
            required = setOf(
                "id",
                "title",
                "description",
                "prerequisiteIds",
                "sectionIds",
            ),
        )
        return KnowledgePointDefinition(
            id = KnowledgePointId(
                requireSectionIdentifier(json.requireContractText("id", location), "$location.id"),
            ),
            title = json.requireContractText("title", location),
            description = json.requireContractText("description", location),
            prerequisiteIds = json.requireContractStrings("prerequisiteIds", location).map { raw ->
                KnowledgePointId(requireSectionIdentifier(raw, "$location.prerequisiteIds"))
            },
            sectionIds = json.requireContractStrings("sectionIds", location).map { raw ->
                requireSectionIdentifier(raw, "$location.sectionIds")
            },
        )
    }
}
