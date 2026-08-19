package com.majortomman.school.data.math

import android.content.Context
import com.majortomman.school.data.local.MathKnowledgeMasteryEntity
import com.majortomman.school.data.local.MathMistakeEntity
import com.majortomman.school.data.local.MathPracticeAttemptEntity
import com.majortomman.school.data.local.PracticeAttemptEntity
import com.majortomman.school.data.local.SchoolDatabase
import com.majortomman.school.learning.cloud.InstalledCourse
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class MathQuestionBankRepository(context: Context) {
    private val learningDao = SchoolDatabase.getInstance(context).learningDao()

    fun observeMastery(course: InstalledCourse): Flow<List<MathMasterySnapshot>> =
        learningDao.observeMathMastery(course.id).map { entities ->
            val existing = entities.associateBy { it.knowledgePointId }
            MathKnowledgeCatalog.forCourse(course).map { point ->
                val entity = existing[point.id]
                MathMasterySnapshot(
                    knowledgePointId = point.id,
                    title = point.title,
                    score = entity?.score ?: INITIAL_SCORE,
                    attempts = entity?.attempts ?: 0,
                    correctStreak = entity?.correctStreak ?: 0,
                    dueAt = entity?.dueAt ?: 0L,
                )
            }.sortedWith(compareBy({ it.score }, { it.dueAt }))
        }

    fun observeMistakes(course: InstalledCourse): Flow<List<MathMistakeSnapshot>> =
        learningDao.observeMathMistakes(course.id).map { entities ->
            entities.mapNotNull { entity ->
                runCatching {
                    MathMistakeSnapshot(
                        mistakeKey = entity.mistakeKey,
                        question = MathQuestion.fromJson(org.json.JSONObject(entity.questionJson)),
                        wrongCount = entity.wrongCount,
                        resolvedStreak = entity.resolvedStreak,
                        lastAnswer = entity.lastAnswer,
                        mistakeType = entity.mistakeType,
                        dueAt = entity.dueAt,
                    )
                }.getOrNull()
            }
        }

    fun observeRecentAttempts(course: InstalledCourse): Flow<List<MathAttemptSummary>> =
        learningDao.observeMathAttempts(course.id).map { attempts ->
            attempts.map { entity ->
                MathAttemptSummary(entity.questionId, entity.knowledgePointId, entity.correct, entity.mistakeType, entity.createdAt)
            }
        }

    suspend fun nextQuestion(
        course: InstalledCourse,
        mode: MathPracticeMode,
        seed: Long = System.nanoTime(),
    ): MathQuestion {
        require(course.isMath()) { "当前课程不是数学课程" }
        val points = MathKnowledgeCatalog.forCourse(course)
        require(points.isNotEmpty()) { "当前课程没有 APK 支持的数学题型" }
        val mastery = learningDao.observeMathMastery(course.id).first().associateBy { it.knowledgePointId }
        val recentTemplates = learningDao.recentMathTemplateIds(course.id).toSet()
        val random = Random(seed)

        if (mode == MathPracticeMode.MISTAKES) {
            val mistakes = learningDao.observeMathMistakes(course.id).first()
            val candidate = mistakes.filter { it.dueAt <= System.currentTimeMillis() }.ifEmpty { mistakes }
                .maxWithOrNull(compareBy<MathMistakeEntity> { it.wrongCount }.thenByDescending { it.updatedAt })
            if (candidate != null) {
                val original = runCatching { MathQuestion.fromJson(org.json.JSONObject(candidate.questionJson)) }.getOrNull()
                return MathQuestionTemplateCatalog.generate(
                    textbookKey = course.id,
                    lessonId = candidate.lessonId,
                    knowledgePointId = candidate.knowledgePointId,
                    difficulty = difficultyFor(mastery[candidate.knowledgePointId]?.score ?: INITIAL_SCORE),
                    seed = seed + candidate.wrongCount * 997L,
                    source = MathQuestionSource.MISTAKE_VARIANT,
                    sourceContext = original?.let { MathSourceContext(it.lessonId, it.sourcePage, it.sourceExcerpt) },
                )
            }
        }

        if (mode == MathPracticeMode.TEXTBOOK) {
            val supportedIds = points.map(MathKnowledgePoint::id).toSet()
            val lessons = course.lessons.filter { lesson -> lesson.knowledgePointIds.any(supportedIds::contains) }
            if (lessons.isNotEmpty()) {
                val lesson = lessons[random.nextInt(lessons.size)]
                val pointId = lesson.knowledgePointIds.first(supportedIds::contains)
                val reference = lesson.references.firstOrNull()
                return MathQuestionTemplateCatalog.generate(
                    textbookKey = course.id,
                    lessonId = lesson.id,
                    knowledgePointId = pointId,
                    difficulty = difficultyFor(mastery[pointId]?.score ?: INITIAL_SCORE),
                    seed = seed,
                    source = MathQuestionSource.TEXTBOOK_VARIANT,
                    sourceContext = MathSourceContext(lesson.id, reference?.pageStart, null),
                    excludedTemplateIds = recentTemplates,
                )
            }
        }

        val point = when (mode) {
            MathPracticeMode.WEAKNESS, MathPracticeMode.MISTAKES -> points.minByOrNull { point ->
                val state = mastery[point.id]
                val duePenalty = if (state != null && state.dueAt <= System.currentTimeMillis()) -0.15 else 0.0
                (state?.score ?: INITIAL_SCORE) + duePenalty
            } ?: points.first()
            MathPracticeMode.TEXTBOOK -> points[random.nextInt(points.size)]
            MathPracticeMode.MIXED -> weightedPoint(points, mastery, random)
        }
        val state = mastery[point.id]
        return MathQuestionTemplateCatalog.generate(
            textbookKey = course.id,
            lessonId = MathKnowledgeCatalog.lessonIdFor(course, point.id),
            knowledgePointId = point.id,
            difficulty = difficultyFor(state?.score ?: INITIAL_SCORE),
            seed = seed,
            source = MathQuestionSource.SYSTEM_TEMPLATE,
            excludedTemplateIds = recentTemplates,
        )
    }

    suspend fun submit(
        question: MathQuestion,
        answer: String,
        usedHint: Boolean,
        durationMillis: Long,
    ): MathSubmissionResult {
        val now = System.currentTimeMillis()
        val evaluation = MathExpressionEngine.evaluate(question, answer)
        learningDao.insertMathAttempt(
            MathPracticeAttemptEntity(
                textbookKey = question.textbookKey,
                lessonId = question.lessonId,
                questionId = question.id,
                templateId = question.templateId,
                knowledgePointId = question.knowledgePointId,
                questionJson = question.toJson().toString(),
                answer = answer.take(4_000),
                canonicalAnswer = question.canonicalAnswer.take(1_000),
                correct = evaluation.correct,
                mistakeType = evaluation.mistakeType?.take(120),
                usedHint = usedHint,
                durationMillis = durationMillis.coerceAtLeast(0L),
                createdAt = now,
            ),
        )
        learningDao.insertAttempt(
            PracticeAttemptEntity(
                lessonId = question.lessonId ?: "math-bank:${question.knowledgePointId}",
                questionId = question.id,
                questionText = question.prompt.take(4_000),
                answer = answer.take(4_000),
                correct = evaluation.correct,
                feedback = evaluation.feedback.take(4_000),
                mistakeType = evaluation.mistakeType?.take(120),
                createdAt = now,
            ),
        )

        val previous = learningDao.getMathMastery(question.textbookKey, question.knowledgePointId)
        val updatedMastery = nextMastery(previous, question, evaluation.correct, usedHint, now)
        learningDao.upsertMathMastery(updatedMastery)
        updateMistake(question, answer, evaluation, now)
        return MathSubmissionResult(question, evaluation, updatedMastery.score)
    }

    private fun weightedPoint(
        points: List<MathKnowledgePoint>,
        mastery: Map<String, MathKnowledgeMasteryEntity>,
        random: Random,
    ): MathKnowledgePoint {
        val now = System.currentTimeMillis()
        val weighted = points.map { point ->
            val state = mastery[point.id]
            val weakness = 1.0 - (state?.score ?: INITIAL_SCORE)
            val dueBoost = if (state == null || state.dueAt <= now) 0.35 else 0.0
            point to (0.2 + weakness + dueBoost)
        }
        val total = weighted.sumOf { it.second }
        var cursor = random.nextDouble() * total
        for ((point, weight) in weighted) {
            cursor -= weight
            if (cursor <= 0) return point
        }
        return weighted.last().first
    }

    private fun difficultyFor(score: Double): MathDifficulty = when {
        score < 0.35 -> MathDifficulty.BASIC
        score < 0.58 -> MathDifficulty.CONSOLIDATION
        score < 0.78 -> MathDifficulty.IMPROVEMENT
        else -> MathDifficulty.CHALLENGE
    }

    private fun nextMastery(
        previous: MathKnowledgeMasteryEntity?,
        question: MathQuestion,
        correct: Boolean,
        usedHint: Boolean,
        now: Long,
    ): MathKnowledgeMasteryEntity {
        val currentScore = previous?.score ?: INITIAL_SCORE
        val difficultyFactor = 0.85 + question.difficulty.level * 0.08
        val score = if (correct) {
            val gain = (if (usedHint) 0.055 else 0.11) * difficultyFactor
            currentScore + (1.0 - currentScore) * gain
        } else {
            currentScore * 0.76
        }.coerceIn(0.05, 0.98)
        val correctStreak = if (correct) (previous?.correctStreak ?: 0) + 1 else 0
        val wrongStreak = if (correct) 0 else (previous?.wrongStreak ?: 0) + 1
        val intervalDays = if (!correct) 1 else when (correctStreak) {
            1 -> 1
            2 -> 3
            3 -> 7
            4 -> 14
            else -> 30
        }
        return MathKnowledgeMasteryEntity(
            textbookKey = question.textbookKey,
            knowledgePointId = question.knowledgePointId,
            score = score,
            attempts = (previous?.attempts ?: 0) + 1,
            correctStreak = correctStreak,
            wrongStreak = wrongStreak,
            lastCorrect = correct,
            dueAt = now + intervalDays * DAY_MILLIS,
            updatedAt = now,
        )
    }

    private suspend fun updateMistake(question: MathQuestion, answer: String, evaluation: MathAnswerEvaluation, now: Long) {
        val mistakeKey = "${question.textbookKey}:${question.knowledgePointId}:${question.templateId}"
        val previous = learningDao.getMathMistake(mistakeKey)
        if (!evaluation.correct) {
            learningDao.upsertMathMistake(
                MathMistakeEntity(
                    mistakeKey = mistakeKey,
                    textbookKey = question.textbookKey,
                    lessonId = question.lessonId,
                    questionId = question.id,
                    templateId = question.templateId,
                    knowledgePointId = question.knowledgePointId,
                    questionJson = question.toJson().toString(),
                    wrongCount = (previous?.wrongCount ?: 0) + 1,
                    resolvedStreak = 0,
                    lastAnswer = answer.take(2_000),
                    mistakeType = evaluation.mistakeType ?: "待进一步诊断",
                    dueAt = now + DAY_MILLIS,
                    updatedAt = now,
                ),
            )
            return
        }
        if (previous != null) {
            val resolved = previous.resolvedStreak + 1
            if (resolved >= 2) {
                learningDao.deleteMathMistake(mistakeKey)
            } else {
                learningDao.upsertMathMistake(previous.copy(resolvedStreak = resolved, dueAt = now + 3 * DAY_MILLIS, updatedAt = now))
            }
        }
    }

    companion object {
        private const val INITIAL_SCORE = 0.25
        private const val DAY_MILLIS = 24L * 60L * 60L * 1_000L
    }
}

data class MathAttemptSummary(
    val questionId: String,
    val knowledgePointId: String,
    val correct: Boolean,
    val mistakeType: String?,
    val createdAt: Long,
)

private fun Double.percentLabel(): String = "${(coerceIn(0.0, 1.0) * 100).roundToInt()}%"
