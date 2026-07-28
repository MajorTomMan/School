package com.majortomman.school.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

/**
 * Shared zoom/pan surface for mathematical visualizations.
 * Pinch zooms, one-finger drag pans while zoomed, and double tap resets or zooms to 2x.
 */
@Composable
internal fun ZoomableMathCanvas(
    modifier: Modifier = Modifier,
    minScale: Float = 1f,
    maxScale: Float = 3.5f,
    content: DrawScope.() -> Unit,
) {
    var scale by remember { mutableFloatStateOf(minScale) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    fun constrained(nextScale: Float, nextOffset: Offset): Offset {
        val maxX = viewport.width * (nextScale - 1f) / 2f
        val maxY = viewport.height * (nextScale - 1f) / 2f
        return Offset(
  x = nextOffset.x.coerceIn(-maxX, maxX),
  y = nextOffset.y.coerceIn(-maxY, maxY),
        )
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(minScale, maxScale)
        offset = if (nextScale <= minScale + 0.001f) {
  Offset.Zero
        } else {
  constrained(nextScale, offset + panChange)
        }
        scale = nextScale
    }

    LaunchedEffect(viewport, scale) {
        offset = constrained(scale, offset)
    }

    Box(
        modifier = modifier
  .clipToBounds()
  .onSizeChanged { viewport = it }
  .pointerInput(minScale, maxScale) {
      detectTapGestures(
          onDoubleTap = {
              if (scale > minScale + 0.05f) {
                  scale = minScale
                  offset = Offset.Zero
              } else {
                  scale = 2f.coerceAtMost(maxScale)
                  offset = Offset.Zero
              }
          },
      )
  }
  .transformable(transformState),
    ) {
        Canvas(
  modifier = Modifier
      .fillMaxSize()
      .graphicsLayer {
          scaleX = scale
          scaleY = scale
          translationX = offset.x
          translationY = offset.y
          transformOrigin = TransformOrigin.Center
      },
  onDraw = content,
        )
    }
}
