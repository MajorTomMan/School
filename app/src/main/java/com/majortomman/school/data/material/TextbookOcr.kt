package com.majortomman.school.data.material

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import java.io.Closeable
import java.io.File
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

const val TEXTBOOK_OCR_SCHEMA_VERSION = 2
private const val OCR_LOG_TAG = "SchoolTextbookOCR"

data class OcrTextLine(
    val text: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val ignoredReason: String? = null,
    val suspicious: Boolean = false,
) {
    val included: Boolean
        get() = ignoredReason == null

    fun toJson(): JSONObject = JSONObject()
        .put("text", text)
        .put("left", left.toDouble())
        .put("top", top.toDouble())
        .put("right", right.toDouble())
        .put("bottom", bottom.toDouble())
        .put("ignoredReason", ignoredReason)
        .put("suspicious", suspicious)

    companion object {
        fun fromJson(root: JSONObject): OcrTextLine = OcrTextLine(
            text = root.optString("text").trim(),
            left = root.optDouble("left", 0.0).toFloat().coerceIn(0f, 1f),
            top = root.optDouble("top", 0.0).toFloat().coerceIn(0f, 1f),
            right = root.optDouble("right", 1.0).toFloat().coerceIn(0f, 1f),
            bottom = root.optDouble("bottom", 1.0).toFloat().coerceIn(0f, 1f),
            ignoredReason = root.optString("ignoredReason").takeIf { it.isNotBlank() },
            suspicious = root.optBoolean("suspicious", false),
        )
    }
}

data class OcrPageDiagnostics(
    val durationMs: Long = 0L,
    val rawLineCount: Int = 0,
    val keptLineCount: Int = 0,
    val ignoredLineCount: Int = 0,
    val suspiciousTokens: List<String> = emptyList(),
) {
    fun toJson(): JSONObject = JSONObject()
        .put("durationMs", durationMs)
        .put("rawLineCount", rawLineCount)
        .put("keptLineCount", keptLineCount)
        .put("ignoredLineCount", ignoredLineCount)
        .put("suspiciousTokens", JSONArray(suspiciousTokens))

    companion object {
        fun fromJson(root: JSONObject?): OcrPageDiagnostics {
            if (root == null) return OcrPageDiagnostics()
            val suspicious = buildList {
                val array = root.optJSONArray("suspiciousTokens") ?: JSONArray()
                for (index in 0 until array.length()) {
                    array.optString(index).trim().takeIf { it.isNotBlank() }?.let(::add)
                }
            }
            return OcrPageDiagnostics(
                durationMs = root.optLong("durationMs", 0L),
                rawLineCount = root.optInt("rawLineCount", 0),
                keptLineCount = root.optInt("keptLineCount", 0),
                ignoredLineCount = root.optInt("ignoredLineCount", 0),
                suspiciousTokens = suspicious,
            )
        }
    }
}

data class OcrPageResult(
    val schemaVersion: Int = TEXTBOOK_OCR_SCHEMA_VERSION,
    val printedPage: Int,
    val pdfIndex: Int,
    val width: Int,
    val height: Int,
    val text: String,
    val lines: List<OcrTextLine>,
    val engine: String = "ML_KIT_CHINESE",
    val rawText: String = text,
    val diagnostics: OcrPageDiagnostics = OcrPageDiagnostics(
        rawLineCount = lines.size,
        keptLineCount = lines.count(OcrTextLine::included),
        ignoredLineCount = lines.count { !it.included },
    ),
) {
    val compactText: String
        get() = text.replace(Regex("\\s+"), " ").trim()

    val isUsable: Boolean
        get() {
            val compact = compactText.filterNot(Char::isWhitespace)
            if (compact.length < MIN_USABLE_CHARACTERS) return false
            val meaningful = compact.count { character ->
                character.isLetterOrDigit() || character in "，。；：！？、（）()[]【】+-×÷=<>|%°"
            }
            return meaningful.toFloat() / compact.length.coerceAtLeast(1) >= MIN_MEANINGFUL_RATIO
        }

    fun toJson(): JSONObject = JSONObject()
        .put("schemaVersion", schemaVersion)
        .put("printedPage", printedPage)
        .put("pdfIndex", pdfIndex)
        .put("width", width)
        .put("height", height)
        .put("engine", engine)
        .put("rawText", rawText)
        .put("text", text)
        .put("lines", JSONArray().apply { lines.forEach { put(it.toJson()) } })
        .put("diagnostics", diagnostics.toJson())

    companion object {
        private const val MIN_USABLE_CHARACTERS = 24
        private const val MIN_MEANINGFUL_RATIO = 0.45f

        fun fromJson(root: JSONObject): OcrPageResult {
            val lineArray = root.optJSONArray("lines") ?: JSONArray()
            val lines = buildList {
                for (index in 0 until lineArray.length()) {
                    add(OcrTextLine.fromJson(lineArray.getJSONObject(index)))
                }
            }
            val cleanedText = root.optString("text")
            return OcrPageResult(
                schemaVersion = root.optInt("schemaVersion", TEXTBOOK_OCR_SCHEMA_VERSION),
                printedPage = root.getInt("printedPage"),
                pdfIndex = root.getInt("pdfIndex"),
                width = root.optInt("width", 1).coerceAtLeast(1),
                height = root.optInt("height", 1).coerceAtLeast(1),
                text = cleanedText,
                lines = lines,
                engine = root.optString("engine", "ML_KIT_CHINESE"),
                rawText = root.optString("rawText", cleanedText),
                diagnostics = OcrPageDiagnostics.fromJson(root.optJSONObject("diagnostics")),
            )
        }
    }
}

internal data class OcrDiagnosticRecord(
    val printedPage: Int,
    val pdfIndex: Int,
    val diagnostics: OcrPageDiagnostics,
    val rawPreview: String,
    val cleanedPreview: String,
    val ignoredLines: List<String>,
)

internal object TextbookOcrStore {
    private const val OCR_DIRECTORY = "generated/ocr"
    private const val DIAGNOSTIC_DIRECTORY = "$OCR_DIRECTORY/diagnostics"

    fun read(textbookRoot: File, printedPage: Int): OcrPageResult? = runCatching {
        val file = pageFile(textbookRoot, printedPage)
        if (!file.isFile) return@runCatching null
        val result = OcrPageResult.fromJson(JSONObject(file.readText(Charsets.UTF_8)))
        result.takeIf { it.schemaVersion == TEXTBOOK_OCR_SCHEMA_VERSION }
    }.getOrNull()

    fun write(textbookRoot: File, result: OcrPageResult) {
        writeAtomic(pageFile(textbookRoot, result.printedPage), result.toJson().toString(2))
        val diagnostic = JSONObject()
            .put("schemaVersion", TEXTBOOK_OCR_SCHEMA_VERSION)
            .put("printedPage", result.printedPage)
            .put("pdfIndex", result.pdfIndex)
            .put("diagnostics", result.diagnostics.toJson())
            .put("rawPreview", result.rawText.take(1_500))
            .put("cleanedPreview", result.text.take(1_500))
            .put(
                "ignoredLines",
                JSONArray().apply {
                    result.lines.filterNot(OcrTextLine::included).take(30).forEach { line ->
                        put("${line.ignoredReason}：${line.text}")
                    }
                },
            )
        writeAtomic(diagnosticFile(textbookRoot, result.printedPage), diagnostic.toString(2))
    }

    fun readDiagnostics(textbookRoot: File): List<OcrDiagnosticRecord> = runCatching {
        val directory = File(textbookRoot, DIAGNOSTIC_DIRECTORY)
        if (!directory.isDirectory) return@runCatching emptyList()
        directory.listFiles { file -> file.isFile && file.extension.equals("json", ignoreCase = true) }
            .orEmpty()
            .mapNotNull { file ->
                runCatching {
                    val root = JSONObject(file.readText(Charsets.UTF_8))
                    OcrDiagnosticRecord(
                        printedPage = root.optInt("printedPage", 1),
                        pdfIndex = root.optInt("pdfIndex", 0),
                        diagnostics = OcrPageDiagnostics.fromJson(root.optJSONObject("diagnostics")),
                        rawPreview = root.optString("rawPreview"),
                        cleanedPreview = root.optString("cleanedPreview"),
                        ignoredLines = buildList {
                            val array = root.optJSONArray("ignoredLines") ?: JSONArray()
                            for (index in 0 until array.length()) {
                                array.optString(index).takeIf { it.isNotBlank() }?.let(::add)
                            }
                        },
                    )
                }.getOrNull()
            }
            .sortedBy { it.printedPage }
    }.getOrDefault(emptyList())

    private fun writeAtomic(file: File, content: String) {
        file.parentFile?.let { parent ->
            require(parent.mkdirs() || parent.isDirectory) { "无法创建 OCR 页面目录" }
        }
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(content, Charsets.UTF_8)
        if (file.exists()) file.delete()
        require(temporary.renameTo(file)) { "无法保存 OCR 页面结果" }
    }

    private fun pageFile(root: File, printedPage: Int): File =
        File(root, "$OCR_DIRECTORY/page-${printedPage.toString().padStart(5, '0')}.json")

    private fun diagnosticFile(root: File, printedPage: Int): File =
        File(root, "$DIAGNOSTIC_DIRECTORY/page-${printedPage.toString().padStart(5, '0')}.json")
}

class TextbookOcrEngine : Closeable {
    private val recognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build(),
    )

    suspend fun recognize(
        bitmap: Bitmap,
        printedPage: Int,
        pdfIndex: Int,
    ): OcrPageResult {
        val startedAt = SystemClock.elapsedRealtime()
        val recognized = recognizer.process(InputImage.fromBitmap(bitmap, 0)).awaitResult()
        val rawLines = recognized.textBlocks.flatMap { block ->
            block.lines.mapNotNull { line ->
                val bounds = line.boundingBox ?: return@mapNotNull null
                val text = normalizeText(line.text)
                if (text.isBlank()) return@mapNotNull null
                bounds.toOcrLine(text, bitmap.width, bitmap.height)
            }
        }.sortedWith(compareBy<OcrTextLine> { it.top }.thenBy { it.left })

        val classifiedLines = rawLines.map(::classifyLine)
        val keptLines = classifiedLines.filter(OcrTextLine::included)
        val rawText = rawLines.joinToString("\n") { it.text }.ifBlank { recognized.text.trim() }
        val cleanedText = keptLines.joinToString("\n") { it.text }
        val suspiciousTokens = classifiedLines
            .filter(OcrTextLine::suspicious)
            .map(OcrTextLine::text)
            .distinct()
            .take(20)
        val diagnostics = OcrPageDiagnostics(
            durationMs = SystemClock.elapsedRealtime() - startedAt,
            rawLineCount = rawLines.size,
            keptLineCount = keptLines.size,
            ignoredLineCount = classifiedLines.size - keptLines.size,
            suspiciousTokens = suspiciousTokens,
        )

        Log.i(
            OCR_LOG_TAG,
            "page=$printedPage pdfIndex=$pdfIndex raw=${diagnostics.rawLineCount} " +
                "kept=${diagnostics.keptLineCount} ignored=${diagnostics.ignoredLineCount} " +
                "chars=${cleanedText.length} durationMs=${diagnostics.durationMs}",
        )
        if (suspiciousTokens.isNotEmpty()) {
            Log.w(OCR_LOG_TAG, "page=$printedPage suspicious=${suspiciousTokens.joinToString("|")}")
        }

        return OcrPageResult(
            printedPage = printedPage,
            pdfIndex = pdfIndex,
            width = bitmap.width,
            height = bitmap.height,
            text = cleanedText,
            rawText = rawText,
            lines = classifiedLines,
            diagnostics = diagnostics,
        )
    }

    override fun close() {
        recognizer.close()
    }
}

private fun classifyLine(line: OcrTextLine): OcrTextLine {
    val text = line.text.trim()
    val compact = text.replace(" ", "")
    val pureDigits = compact.matches(Regex("[0-9]{1,8}"))
    val isolatedLongDigits = compact.matches(Regex("[0-9]{4,8}")) && (line.right - line.left) < 0.24f
    val margin = line.top < 0.075f || line.bottom > 0.925f
    val meaningful = compact.count { character ->
        character.isLetterOrDigit() || character in "，。；：！？、（）()[]【】+-×÷=<>|%°"
    }
    val meaningfulRatio = meaningful.toFloat() / compact.length.coerceAtLeast(1)
    val ignoredReason = when {
        compact.isBlank() -> "空文本"
        compact.matches(Regex("[—_~·.。…|丨]+")) -> "装饰线"
        pureDigits && margin -> "页眉页脚数字"
        isolatedLongDigits -> "孤立长数字"
        compact.length <= 2 && margin -> "页眉页脚短文本"
        meaningfulRatio < 0.34f && compact.length <= 12 -> "低信息文本"
        else -> null
    }
    val suspicious = isolatedLongDigits || Regex("(?<![0-9])[0-9]{4,8}(?![0-9])").containsMatchIn(compact)
    return line.copy(
        ignoredReason = ignoredReason,
        suspicious = suspicious,
    )
}

private fun normalizeText(raw: String): String = raw
    .replace('\u00A0', ' ')
    .replace('\u3000', ' ')
    .replace("﹣", "-")
    .replace("−", "-")
    .replace("＝", "=")
    .replace("＜", "<")
    .replace("＞", ">")
    .replace(Regex("[\\u200B-\\u200D\\uFEFF]"), "")
    .replace(Regex("[ \\t]+"), " ")
    .trim()

private fun Rect.toOcrLine(text: String, width: Int, height: Int): OcrTextLine {
    val safeWidth = width.coerceAtLeast(1).toFloat()
    val safeHeight = height.coerceAtLeast(1).toFloat()
    return OcrTextLine(
        text = text,
        left = left / safeWidth,
        top = top / safeHeight,
        right = right / safeWidth,
        bottom = bottom / safeHeight,
    )
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { value ->
        if (continuation.isActive) continuation.resume(value)
    }
    addOnFailureListener { error ->
        if (continuation.isActive) continuation.resumeWithException(error)
    }
    addOnCanceledListener {
        continuation.cancel()
    }
}
