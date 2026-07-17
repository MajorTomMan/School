package com.majortomman.school.learning.language

import com.majortomman.school.learning.verification.DiagnosticResult
import com.majortomman.school.learning.verification.DiagnosticStep
import com.majortomman.school.learning.verification.ErrorType
import com.majortomman.school.learning.verification.VerificationStatus
import java.text.Normalizer
import java.util.Locale

enum class LanguageCode {
    ENGLISH,
    JAPANESE,
}

enum class TokenRole {
    SUBJECT,
    PREDICATE,
    OBJECT,
    TIME,
    PLACE,
    PARTICLE,
    AUXILIARY,
    OTHER,
}

data class LanguageToken(
    val id: String,
    val surface: String,
    val lemma: String? = null,
    val reading: String? = null,
    val romanization: String? = null,
    val meaning: String? = null,
    val role: TokenRole = TokenRole.OTHER,
)

data class SentencePattern(
    val id: String,
    val language: LanguageCode,
    val tokens: List<LanguageToken>,
    val acceptedOrders: List<List<String>> = listOf(tokens.map { it.id }),
    val separator: String = if (language == LanguageCode.ENGLISH) " " else "",
    val ending: String = if (language == LanguageCode.ENGLISH) "." else "。",
) {
    init {
        require(tokens.map { it.id }.toSet().size == tokens.size) { "Token ids must be unique." }
        val knownIds = tokens.map { it.id }.toSet()
        require(acceptedOrders.isNotEmpty()) { "At least one accepted token order is required." }
        require(acceptedOrders.all { order -> order.size == tokens.size && order.toSet() == knownIds }) {
            "Every accepted order must contain every token exactly once."
        }
    }

    private val tokenById: Map<String, LanguageToken> = tokens.associateBy { it.id }

    fun textFor(order: List<String>): String = order
        .mapNotNull(tokenById::get)
        .joinToString(separator = separator, postfix = ending) { it.surface }

    fun token(id: String): LanguageToken? = tokenById[id]
}

data class LanguageAnswerPolicy(
    val ignoreCase: Boolean = false,
    val ignorePunctuation: Boolean = false,
    val collapseWhitespace: Boolean = true,
    val acceptedAlternatives: Set<String> = emptySet(),
    val contractionEquivalents: Map<String, String> = emptyMap(),
)

enum class EnglishForm {
    BASE,
    THIRD_PERSON_SINGULAR,
    PAST,
    PAST_PARTICIPLE,
    GERUND,
}

data class EnglishLexeme(
    val lemma: String,
    val forms: Map<EnglishForm, String>,
) {
    init {
        require(forms[EnglishForm.BASE] != null) { "English lexeme requires a base form." }
    }
}

enum class JapaneseForm {
    DICTIONARY,
    POLITE_PRESENT,
    POLITE_NEGATIVE,
    POLITE_PAST,
    POLITE_PAST_NEGATIVE,
}

data class JapaneseLexeme(
    val dictionaryForm: String,
    val reading: String,
    val forms: Map<JapaneseForm, String>,
) {
    init {
        require(forms[JapaneseForm.DICTIONARY] != null) { "Japanese lexeme requires a dictionary form." }
    }
}

enum class SpeechRegister {
    CASUAL,
    POLITE,
}

data class DialogueContext(
    val speakerRole: String,
    val listenerRole: String,
    val expectedRegister: SpeechRegister,
    val description: String,
)

data class JapaneseParticleRule(
    val preferred: String,
    val accepted: Set<String>,
    val explanationByParticle: Map<String, String>,
) {
    init {
        require(preferred in accepted) { "Preferred particle must be accepted." }
    }
}

object LanguageAnswerVerifier {
    private val punctuationRegex = Regex("[\\p{Punct}\\p{P}]")
    private val whitespaceRegex = Regex("\\s+")

    fun verifyText(
        expected: String,
        actual: String,
        policy: LanguageAnswerPolicy = LanguageAnswerPolicy(),
    ): DiagnosticResult {
        if (actual.isBlank()) {
            return DiagnosticResult(
                status = VerificationStatus.INPUT_IN_PROGRESS,
                message = "继续输入答案。",
            )
        }

        val acceptedRaw = buildSet {
            add(expected)
            addAll(policy.acceptedAlternatives)
        }
        val normalizedActual = normalize(actual, policy)
        val normalizedAccepted = acceptedRaw.map { normalize(it, policy) }.toSet()
        if (normalizedActual in normalizedAccepted) {
            return DiagnosticResult(
                status = VerificationStatus.CORRECT,
                normalizedAnswer = normalizedActual,
                steps = listOf(
                    DiagnosticStep("输入", actual, true),
                    DiagnosticStep("规范化", normalizedActual, true),
                ),
                message = "表达符合当前题目的接受规则。",
            )
        }

        val loosePolicy = policy.copy(ignoreCase = true, ignorePunctuation = true, collapseWhitespace = true)
        val looseActual = normalize(actual, loosePolicy)
        val looseExpected = acceptedRaw.map { normalize(it, loosePolicy) }.toSet()
        val errorType = when {
            looseActual in looseExpected && !policy.ignoreCase && actual.lowercase(Locale.ROOT) in
                acceptedRaw.map { it.lowercase(Locale.ROOT) }.toSet() -> ErrorType.CAPITALIZATION
            looseActual in looseExpected && !policy.ignorePunctuation -> ErrorType.PUNCTUATION
            closestDistance(normalizedActual, normalizedAccepted) <= 2 -> ErrorType.SPELLING
            else -> ErrorType.SENTENCE_STRUCTURE
        }
        val message = when (errorType) {
            ErrorType.CAPITALIZATION -> "内容基本正确，请检查大小写。"
            ErrorType.PUNCTUATION -> "内容基本正确，请检查标点。"
            ErrorType.SPELLING -> "句子结构接近，请检查拼写。"
            else -> "当前表达与本题接受的句子结构不一致。"
        }
        return DiagnosticResult(
            status = VerificationStatus.INCORRECT,
            normalizedAnswer = normalizedActual,
            steps = listOf(
                DiagnosticStep("你的表达", actual, false),
                DiagnosticStep("规范化后", normalizedActual, false),
                DiagnosticStep("参考表达", expected, null),
            ),
            errorType = errorType,
            message = message,
        )
    }

    fun verifyOrder(pattern: SentencePattern, actualTokenIds: List<String>): DiagnosticResult {
        if (actualTokenIds.isEmpty()) {
            return DiagnosticResult(
                status = VerificationStatus.INPUT_IN_PROGRESS,
                message = "依次选择词语组成句子。",
            )
        }
        if (actualTokenIds in pattern.acceptedOrders) {
            return DiagnosticResult(
                status = VerificationStatus.CORRECT,
                normalizedAnswer = pattern.textFor(actualTokenIds),
                steps = actualTokenIds.mapIndexed { index, id ->
                    DiagnosticStep("第 ${index + 1} 位", pattern.token(id)?.surface.orEmpty(), true)
                },
                message = "词语顺序正确。",
            )
        }

        val expectedIds = pattern.tokens.map { it.id }
        val sameTokens = actualTokenIds.size == expectedIds.size &&
            actualTokenIds.groupingBy { it }.eachCount() == expectedIds.groupingBy { it }.eachCount()
        val errorType = if (sameTokens) ErrorType.WORD_ORDER else ErrorType.SENTENCE_STRUCTURE
        return DiagnosticResult(
            status = VerificationStatus.INCORRECT,
            normalizedAnswer = pattern.textFor(actualTokenIds),
            steps = listOf(
                DiagnosticStep("当前顺序", pattern.textFor(actualTokenIds), false),
                DiagnosticStep("参考顺序", pattern.textFor(pattern.acceptedOrders.first()), null),
            ),
            errorType = errorType,
            message = if (sameTokens) "词语都选对了，但顺序需要调整。" else "句子中有词语缺失或重复。",
        )
    }

    fun normalize(text: String, policy: LanguageAnswerPolicy): String {
        var result = Normalizer.normalize(text, Normalizer.Form.NFKC)
            .replace('’', '\'')
            .replace('‘', '\'')
            .trim()
        policy.contractionEquivalents.forEach { (variant, canonical) ->
            result = result.replace(Regex(Regex.escape(variant), RegexOption.IGNORE_CASE), canonical)
        }
        if (policy.ignoreCase) result = result.lowercase(Locale.ROOT)
        if (policy.ignorePunctuation) result = result.replace(punctuationRegex, "")
        if (policy.collapseWhitespace) result = result.replace(whitespaceRegex, " ").trim()
        return result
    }

    private fun closestDistance(actual: String, accepted: Set<String>): Int =
        accepted.minOfOrNull { levenshtein(actual, it) } ?: Int.MAX_VALUE

    private fun levenshtein(left: String, right: String): Int {
        if (left == right) return 0
        if (left.isEmpty()) return right.length
        if (right.isEmpty()) return left.length
        var previous = IntArray(right.length + 1) { it }
        var current = IntArray(right.length + 1)
        left.forEachIndexed { leftIndex, leftChar ->
            current[0] = leftIndex + 1
            right.forEachIndexed { rightIndex, rightChar ->
                current[rightIndex + 1] = minOf(
                    current[rightIndex] + 1,
                    previous[rightIndex + 1] + 1,
                    previous[rightIndex] + if (leftChar == rightChar) 0 else 1,
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[right.length]
    }
}

object EnglishWordFormVerifier {
    fun verify(lexeme: EnglishLexeme, requestedForm: EnglishForm, actual: String): DiagnosticResult {
        val expected = lexeme.forms[requestedForm]
            ?: return DiagnosticResult(
                status = VerificationStatus.UNSUPPORTED,
                errorType = ErrorType.WORD_FORM,
                message = "当前词条没有配置这一词形。",
            )
        val result = LanguageAnswerVerifier.verifyText(
            expected = expected,
            actual = actual,
            policy = LanguageAnswerPolicy(ignoreCase = true, ignorePunctuation = true),
        )
        return if (result.status == VerificationStatus.CORRECT) {
            result.copy(message = "词形正确：${lexeme.lemma} → $expected。")
        } else {
            result.copy(
                errorType = ErrorType.WORD_FORM,
                message = "这里需要 ${requestedForm.displayName()}：$expected。",
            )
        }
    }

    private fun EnglishForm.displayName(): String = when (this) {
        EnglishForm.BASE -> "原形"
        EnglishForm.THIRD_PERSON_SINGULAR -> "第三人称单数形式"
        EnglishForm.PAST -> "过去式"
        EnglishForm.PAST_PARTICIPLE -> "过去分词"
        EnglishForm.GERUND -> "-ing 形式"
    }
}

object JapaneseLanguageVerifier {
    fun verifyParticle(rule: JapaneseParticleRule, actual: String): DiagnosticResult {
        if (actual.isBlank()) {
            return DiagnosticResult(
                status = VerificationStatus.INPUT_IN_PROGRESS,
                message = "选择一个助词。",
            )
        }
        val accepted = actual in rule.accepted
        val explanation = rule.explanationByParticle[actual].orEmpty()
        return DiagnosticResult(
            status = if (accepted) VerificationStatus.CORRECT else VerificationStatus.INCORRECT,
            normalizedAnswer = actual,
            steps = listOf(
                DiagnosticStep("所选助词", actual, accepted),
                DiagnosticStep("本题优先表达", rule.preferred, null),
            ),
            errorType = if (accepted) null else ErrorType.PARTICLE,
            message = if (accepted) {
                explanation.ifBlank { "助词符合当前语境。" }
            } else {
                "这个助词不符合当前句子的关系。${if (explanation.isBlank()) "" else " $explanation"}"
            },
        )
    }

    fun verifyForm(lexeme: JapaneseLexeme, requestedForm: JapaneseForm, actual: String): DiagnosticResult {
        val expected = lexeme.forms[requestedForm]
            ?: return DiagnosticResult(
                status = VerificationStatus.UNSUPPORTED,
                errorType = ErrorType.CONJUGATION,
                message = "当前词条没有配置这一活用形式。",
            )
        val result = LanguageAnswerVerifier.verifyText(
            expected = expected,
            actual = actual,
            policy = LanguageAnswerPolicy(ignorePunctuation = true),
        )
        return if (result.status == VerificationStatus.CORRECT) {
            result.copy(message = "活用正确：${lexeme.dictionaryForm} → $expected。")
        } else {
            result.copy(
                errorType = ErrorType.CONJUGATION,
                message = "当前语境需要 ${requestedForm.displayName()}：$expected。",
            )
        }
    }

    fun verifyRegister(context: DialogueContext, actual: SpeechRegister): DiagnosticResult {
        val correct = actual == context.expectedRegister
        return DiagnosticResult(
            status = if (correct) VerificationStatus.CORRECT else VerificationStatus.INCORRECT,
            normalizedAnswer = actual.name,
            steps = listOf(
                DiagnosticStep("说话者", context.speakerRole, null),
                DiagnosticStep("听话者", context.listenerRole, null),
                DiagnosticStep("所选语体", actual.displayName(), correct),
            ),
            errorType = if (correct) null else ErrorType.SPEECH_REGISTER,
            message = if (correct) "语体与当前人物关系相符。" else "当前人物关系更适合${context.expectedRegister.displayName()}。",
        )
    }

    private fun JapaneseForm.displayName(): String = when (this) {
        JapaneseForm.DICTIONARY -> "辞书形"
        JapaneseForm.POLITE_PRESENT -> "礼貌体现在肯定"
        JapaneseForm.POLITE_NEGATIVE -> "礼貌体现在否定"
        JapaneseForm.POLITE_PAST -> "礼貌体过去肯定"
        JapaneseForm.POLITE_PAST_NEGATIVE -> "礼貌体过去否定"
    }

    private fun SpeechRegister.displayName(): String = when (this) {
        SpeechRegister.CASUAL -> "简体表达"
        SpeechRegister.POLITE -> "礼貌体表达"
    }
}
