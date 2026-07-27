package com.majortomman.school.learning.cloud

import android.graphics.BitmapFactory
import com.majortomman.school.learning.assessment.contract.ASSESSMENTS_FILE_NAME
import com.majortomman.school.learning.assessment.contract.AssessmentDocumentParser
import com.majortomman.school.learning.assessment.contract.AssessmentPackageContract
import com.majortomman.school.learning.assessment.contract.ContentAssetDefinition
import com.majortomman.school.learning.assessment.contract.KNOWLEDGE_POINTS_FILE_NAME
import com.majortomman.school.learning.assessment.contract.KnowledgePointDocumentParser
import com.majortomman.school.learning.course.CourseDocument
import java.io.File

internal object AssessmentPackageStagingValidator {
    fun validate(
        remote: CourseTextbookManifest,
        staging: File,
        course: CourseDocument,
    ) {
        val assessmentsFile = File(staging, ASSESSMENTS_FILE_NAME)
        val knowledgePointsFile = File(staging, KNOWLEDGE_POINTS_FILE_NAME)
        require(assessmentsFile.isFile == knowledgePointsFile.isFile) {
            "$ASSESSMENTS_FILE_NAME 与 $KNOWLEDGE_POINTS_FILE_NAME 必须同时存在"
        }
        if (!assessmentsFile.isFile) return

        require(remote.files.any { it.path == ASSESSMENTS_FILE_NAME }) {
            "课程清单未声明 $ASSESSMENTS_FILE_NAME"
        }
        require(remote.files.any { it.path == KNOWLEDGE_POINTS_FILE_NAME }) {
            "课程清单未声明 $KNOWLEDGE_POINTS_FILE_NAME"
        }

        val assessments = AssessmentDocumentParser.decode(assessmentsFile.readText(Charsets.UTF_8))
        val knowledgePoints = KnowledgePointDocumentParser.decode(knowledgePointsFile.readText(Charsets.UTF_8))
        AssessmentPackageContract.validate(course, assessments, knowledgePoints)
        assessments.assets.forEach { asset -> validateAsset(remote, staging, asset) }
    }

    private fun validateAsset(
        remote: CourseTextbookManifest,
        staging: File,
        asset: ContentAssetDefinition,
    ) {
        require(remote.files.any { it.path == asset.path }) {
            "课程清单未声明题目资产：${asset.path}"
        }
        val file = safeResolve(staging, asset.path)
        require(file.isFile) { "题目资产不存在：${asset.path}" }

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        require(options.outWidth > 0 && options.outHeight > 0) {
            "题目资产不是 APK 支持的图片：${asset.path}"
        }
        require(options.outWidth == asset.width && options.outHeight == asset.height) {
            "题目资产尺寸不一致：${asset.path}，契约=${asset.width}x${asset.height}，实际=${options.outWidth}x${options.outHeight}"
        }
        require(options.outMimeType == asset.mediaType.wireValue) {
            "题目资产格式不一致：${asset.path}，契约=${asset.mediaType.wireValue}，实际=${options.outMimeType}"
        }
    }

    private fun safeResolve(parent: File, relativePath: String): File {
        val target = File(parent, relativePath)
        val parentPath = parent.canonicalFile.toPath()
        val targetPath = target.canonicalFile.toPath()
        require(targetPath.startsWith(parentPath)) { "题目资产路径越界：$relativePath" }
        return target
    }
}
