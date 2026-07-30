package com.majortomman.school.ui.visualization.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.ui.InteractiveBlue
import com.majortomman.school.ui.InteractiveGreen
import com.majortomman.school.ui.InteractiveLine
import com.majortomman.school.ui.InteractiveMuted
import com.majortomman.school.ui.InteractivePurple
import com.majortomman.school.ui.InteractiveRed
import com.majortomman.school.ui.InteractiveWhite
import com.majortomman.school.ui.InteractiveYellow

/** Stable subject identity used by the shared visualization runtime. */
enum class VisualizationSubject(val label: String) {
    MATHEMATICS("数学"),
    PHYSICS("物理"),
    CHEMISTRY("化学"),
    BIOLOGY("生物"),
    GENERAL("通用"),
}

@JvmInline
value class VisualizationKey(val value: String) {
    init {
        require(value.matches(KEY_PATTERN)) {
            "可视化 key 只能包含小写字母、数字、点、下划线和连字符：$value"
        }
    }

    companion object {
        private val KEY_PATTERN = Regex("[a-z0-9][a-z0-9._-]*")
    }
}

enum class VisualizationValueType {
    STRING,
    NUMBER,
    BOOLEAN,
    NUMBER_LIST,
}

data class VisualizationFieldSpec(
    val name: String,
    val type: VisualizationValueType,
    val required: Boolean = true,
    val description: String = "",
)

data class VisualizationSchema(
    val fields: List<VisualizationFieldSpec> = emptyList(),
) {
    fun validate(arguments: VisualizationArguments): List<String> = buildList {
        fields.forEach { field ->
            val value = arguments.raw(field.name)
            when {
                value == null && field.required -> add("缺少参数 ${field.name}")
                value != null && !field.type.accepts(value) -> add("参数 ${field.name} 类型应为 ${field.type}")
            }
        }
    }

    private fun VisualizationValueType.accepts(value: Any): Boolean = when (this) {
        VisualizationValueType.STRING -> value is String
        VisualizationValueType.NUMBER -> value is Number
        VisualizationValueType.BOOLEAN -> value is Boolean
        VisualizationValueType.NUMBER_LIST -> value is List<*> && value.all { it is Number }
    }
}

/** Immutable, typed access to scene arguments from local code or cloud course data. */
class VisualizationArguments private constructor(
    private val values: Map<String, Any?>,
) {
    fun raw(name: String): Any? = values[name]

    fun string(name: String, default: String = ""): String = values[name] as? String ?: default

    fun float(name: String, default: Float = 0f): Float = (values[name] as? Number)?.toFloat() ?: default

    fun boolean(name: String, default: Boolean = false): Boolean = values[name] as? Boolean ?: default

    fun floatList(name: String): List<Float> = (values[name] as? List<*>)
        ?.mapNotNull { (it as? Number)?.toFloat() }
        .orEmpty()

    fun with(name: String, value: Any?): VisualizationArguments =
        VisualizationArguments(values + (name to value))

    companion object {
        val Empty = VisualizationArguments(emptyMap())

        fun of(vararg values: Pair<String, Any?>): VisualizationArguments =
            VisualizationArguments(linkedMapOf(*values))
    }
}

data class VisualizationCapabilities(
    val zoom: Boolean = true,
    val pan: Boolean = true,
    val doubleTapReset: Boolean = true,
    val animated: Boolean = true,
    val accessibleLabels: Boolean = true,
)

data class VisualizationPalette(
    val primary: Color,
    val positive: Color,
    val negative: Color,
    val success: Color,
    val danger: Color,
    val foreground: Color,
    val muted: Color,
    val grid: Color,
)

data class VisualizationRenderContext(
    val arguments: VisualizationArguments,
    val subject: VisualizationSubject,
    val palette: VisualizationPalette,
    val capabilities: VisualizationCapabilities,
)

/**
 * Parent renderer for every subject visualization.
 *
 * Subclasses declare a stable key and schema, then implement only [RenderContent]. The parent owns validation,
 * subject palette, fallback rendering and common lifecycle rules. Drawing primitives stay composable tools instead
 * of being duplicated inside subclasses.
 */
abstract class SubjectVisualizationRenderer {
    abstract val key: VisualizationKey
    abstract val subject: VisualizationSubject
    open val schema: VisualizationSchema = VisualizationSchema()
    open val capabilities: VisualizationCapabilities = VisualizationCapabilities()

    @Composable
    fun Render(
        arguments: VisualizationArguments,
        modifier: Modifier = Modifier,
    ) {
        val issues = schema.validate(arguments)
        val palette = visualizationPalette(subject)
        if (issues.isNotEmpty()) {
            VisualizationErrorState(
                title = "${subject.label}可视化数据不完整",
                details = issues,
                palette = palette,
                modifier = modifier,
            )
            return
        }

        RenderContent(
            context = VisualizationRenderContext(
                arguments = arguments,
                subject = subject,
                palette = palette,
                capabilities = capabilities,
            ),
            modifier = modifier,
        )
    }

    @Composable
    protected abstract fun RenderContent(
        context: VisualizationRenderContext,
        modifier: Modifier,
    )
}

/** Explicit registry: deterministic on Android, testable, and free from runtime classpath scanning. */
class VisualizationRegistry(
    renderers: List<SubjectVisualizationRenderer>,
) {
    private val renderersByKey: Map<VisualizationKey, SubjectVisualizationRenderer>

    init {
        val duplicates = renderers.groupBy(SubjectVisualizationRenderer::key).filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "重复的可视化 key：${duplicates.joinToString { it.value }}" }
        renderersByKey = renderers.associateBy(SubjectVisualizationRenderer::key)
    }

    fun rendererFor(key: VisualizationKey): SubjectVisualizationRenderer? = renderersByKey[key]

    fun registeredKeys(): Set<VisualizationKey> = renderersByKey.keys

    @Composable
    fun Render(
        key: VisualizationKey,
        arguments: VisualizationArguments,
        modifier: Modifier = Modifier,
    ) {
        val renderer = rendererFor(key)
        if (renderer == null) {
            VisualizationErrorState(
                title = "未注册的可视化",
                details = listOf(key.value),
                palette = visualizationPalette(VisualizationSubject.GENERAL),
                modifier = modifier,
            )
        } else {
            renderer.Render(arguments, modifier)
        }
    }
}

@Composable
private fun VisualizationErrorState(
    title: String,
    details: List<String>,
    palette: VisualizationPalette,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            color = palette.danger,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        details.forEach { detail ->
            Text("— $detail", color = palette.muted, fontSize = 12.sp, lineHeight = 18.sp)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(palette.grid))
    }
}

private fun visualizationPalette(subject: VisualizationSubject): VisualizationPalette = VisualizationPalette(
    primary = when (subject) {
        VisualizationSubject.MATHEMATICS -> InteractiveBlue
        VisualizationSubject.PHYSICS -> InteractivePurple
        VisualizationSubject.CHEMISTRY -> InteractiveGreen
        VisualizationSubject.BIOLOGY -> InteractiveGreen
        VisualizationSubject.GENERAL -> InteractiveBlue
    },
    positive = InteractiveBlue,
    negative = InteractiveYellow,
    success = InteractiveGreen,
    danger = InteractiveRed,
    foreground = InteractiveWhite,
    muted = InteractiveMuted,
    grid = InteractiveLine,
)
