package com.majortomman.school.visualization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.visualization.renderers.math.MathVisualizationRenderers

internal enum class VisualizationSubject {
    MATHEMATICS,
    PHYSICS,
    CHEMISTRY,
    BIOLOGY,
    GENERAL,
}

internal data class VisualizationPalette(
    val primary: Color,
    val secondary: Color,
    val positive: Color,
    val negative: Color,
    val foreground: Color,
    val muted: Color,
    val grid: Color,
    val danger: Color,
)

internal val DefaultVisualizationPalette = VisualizationPalette(
    primary = Color(0xFF0A84FF),
    secondary = Color(0xFFFFD60A),
    positive = Color(0xFF32D74B),
    negative = Color(0xFFFFD60A),
    foreground = Color(0xFFF5F5F7),
    muted = Color(0xFF8E8E93),
    grid = Color(0xFF3A3A3C),
    danger = Color(0xFFFF453A),
)

internal data class VisualizationRenderContext(
    val invocation: VisualizationInvocation,
    val subject: VisualizationSubject,
    val palette: VisualizationPalette = DefaultVisualizationPalette,
)

internal abstract class VisualizationRenderer {
    abstract val key: VisualizationKey
    abstract val subject: VisualizationSubject
    abstract val schema: VisualizationSchema

    @Composable
    abstract fun Render(context: VisualizationRenderContext, modifier: Modifier)
}

internal class VisualizationRegistry(renderers: List<VisualizationRenderer>) {
    private val byKey: Map<VisualizationKey, VisualizationRenderer>

    init {
        val duplicateKeys = renderers.groupBy { it.key }.filterValues { it.size > 1 }.keys
        require(duplicateKeys.isEmpty()) { "重复的可视化 renderer key：${duplicateKeys.joinToString { it.value }}" }
        byKey = renderers.associateBy { it.key }
    }

    fun keys(): Set<VisualizationKey> = byKey.keys

    fun validate(invocation: VisualizationInvocation): List<String> {
        val renderer = byKey[invocation.renderer] ?: return listOf("未注册的可视化 renderer：${invocation.renderer.value}")
        return renderer.schema.validate(invocation)
    }

    @Composable
    fun Render(invocation: VisualizationInvocation, modifier: Modifier) {
        val renderer = byKey[invocation.renderer]
        if (renderer == null) {
            VisualizationError(listOf("未注册的可视化 renderer：${invocation.renderer.value}"), modifier)
            return
        }
        val issues = renderer.schema.validate(invocation)
        if (issues.isNotEmpty()) {
            VisualizationError(issues, modifier)
            return
        }
        renderer.Render(VisualizationRenderContext(invocation, renderer.subject), modifier)
    }
}

object SchoolVisualizationCatalog {
    private val registry = VisualizationRegistry(MathVisualizationRenderers.all)

    fun registeredKeys(): Set<VisualizationKey> = registry.keys()

    fun validate(invocation: VisualizationInvocation): List<String> = registry.validate(invocation)

    fun requireValid(invocation: VisualizationInvocation) {
        val issues = validate(invocation)
        require(issues.isEmpty()) { issues.joinToString(separator = "; ") }
    }

    @Composable
    fun Render(invocation: VisualizationInvocation, modifier: Modifier = Modifier) {
        registry.Render(invocation, modifier)
    }
}

@Composable
fun SchoolVisualization(invocation: VisualizationInvocation, modifier: Modifier = Modifier) {
    SchoolVisualizationCatalog.Render(invocation, modifier)
}

@Composable
private fun VisualizationError(issues: List<String>, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("可视化数据无效", color = DefaultVisualizationPalette.danger, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        issues.forEach { Text("— $it", color = DefaultVisualizationPalette.muted, fontSize = 12.sp) }
    }
}
