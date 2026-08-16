package com.majortomman.school.visualization

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp

internal enum class AnnotationPlacement {
    ABOVE,
    BELOW,
    LEFT,
    RIGHT,
    CENTER,
}

@Composable
internal fun ZoomableVisualizationSurface(
    modifier: Modifier = Modifier,
    minScale: Float = 1f,
    maxScale: Float = 3.5f,
    geometry: DrawScope.() -> Unit,
    annotations: @Composable BoxScope.() -> Unit = {},
) {
    var scale by remember { mutableFloatStateOf(minScale) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    fun constrained(nextScale: Float, nextOffset: Offset): Offset {
        val maxX = viewport.width * (nextScale - 1f) / 2f
        val maxY = viewport.height * (nextScale - 1f) / 2f
        return Offset(nextOffset.x.coerceIn(-maxX, maxX), nextOffset.y.coerceIn(-maxY, maxY))
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(minScale, maxScale)
        offset = if (nextScale <= minScale + 0.001f) Offset.Zero else constrained(nextScale, offset + panChange)
        scale = nextScale
    }

    LaunchedEffect(viewport, scale) {
        offset = constrained(scale, offset)
    }

    Box(
        modifier = modifier.clipToBounds().onSizeChanged { viewport = it }.pointerInput(minScale, maxScale) {
            detectTapGestures(onDoubleTap = {
                if (scale > minScale + 0.05f) {
                    scale = minScale
                    offset = Offset.Zero
                } else {
                    scale = 2f.coerceAtMost(maxScale)
                    offset = Offset.Zero
                }
            })
        }.transformable(transformState),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
                transformOrigin = TransformOrigin.Center
            },
        ) {
            Canvas(Modifier.fillMaxSize(), onDraw = geometry)
            annotations()
        }
    }
}

@Composable
internal fun BoxScope.VisualizationAnnotation(
    text: String,
    x: Float,
    y: Float,
    color: Color,
    placement: AnnotationPlacement = AnnotationPlacement.ABOVE,
    emphasized: Boolean = false,
) {
    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            Text(
                text = text,
                color = color,
                fontSize = if (emphasized) 14.sp else 12.sp,
                fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 2,
            )
        },
    ) { measurables, constraints ->
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeable = measurables.single().measure(childConstraints)
        val anchorX = (constraints.maxWidth * x.coerceIn(0f, 1f)).toInt()
        val anchorY = (constraints.maxHeight * y.coerceIn(0f, 1f)).toInt()
        val gap = 8
        val left = when (placement) {
            AnnotationPlacement.LEFT -> anchorX - placeable.width - gap
            AnnotationPlacement.RIGHT -> anchorX + gap
            else -> anchorX - placeable.width / 2
        }.coerceIn(0, (constraints.maxWidth - placeable.width).coerceAtLeast(0))
        val top = when (placement) {
            AnnotationPlacement.ABOVE -> anchorY - placeable.height - gap
            AnnotationPlacement.BELOW -> anchorY + gap
            AnnotationPlacement.LEFT, AnnotationPlacement.RIGHT, AnnotationPlacement.CENTER -> anchorY - placeable.height / 2
        }.coerceIn(0, (constraints.maxHeight - placeable.height).coerceAtLeast(0))
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.placeRelative(left, top)
        }
    }
}
