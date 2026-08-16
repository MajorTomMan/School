package com.majortomman.school.ui

import android.graphics.Canvas as AndroidCanvas
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.nativeCanvas as composeNativeCanvas

internal val Canvas.nativeCanvas: AndroidCanvas
    get() = this.composeNativeCanvas
