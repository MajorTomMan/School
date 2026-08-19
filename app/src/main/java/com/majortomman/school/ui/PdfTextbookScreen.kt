package com.majortomman.school.ui

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majortomman.school.learning.cloud.InstalledCourse
import java.io.Closeable
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val ReaderBlack = Color(0xFF050608)
private val ReaderWhite = Color(0xFFF5F7FA)
private val ReaderBlue = Color(0xFF2D7BFF)
private val ReaderYellow = Color(0xFFFFCC00)
private val ReaderMuted = ReaderWhite.copy(alpha = 0.46f)

@Composable
fun PdfTextbookScreen(
    course: InstalledCourse,
    initialPrintedPage: Int,
    readingRange: IntRange? = null,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    if (!course.pdfFile.isFile) {
        ReaderError("教材 PDF 尚未下载或缓存已损坏。请返回后重新同步云端课程。", onBack)
        return
    }

    val sessionResult = remember(course.id, course.contentVersion) { runCatching { PdfRenderSession(course.pdfFile) } }
    val session = sessionResult.getOrNull()
    DisposableEffect(session) { onDispose { session?.close() } }
    if (session == null) {
        ReaderError(sessionResult.exceptionOrNull()?.message ?: "无法打开云端教材缓存", onBack)
        return
    }

    var fullBook by rememberSaveable(course.id, initialPrintedPage, readingRange?.first, readingRange?.last) { mutableStateOf(false) }
    val activeWindow = readingRange.takeUnless { fullBook }
    val initialPage = activeWindow?.let { initialPrintedPage.coerceIn(it.first, it.last) } ?: initialPrintedPage
    var pageIndex by rememberSaveable(course.id, course.contentVersion, initialPrintedPage) {
        mutableStateOf(course.printedPageToPdfIndex(initialPage).coerceIn(0, session.pageCount - 1))
    }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var renderError by remember { mutableStateOf<String?>(null) }
    var zoom by rememberSaveable(course.id, initialPrintedPage, pageIndex) { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    val printedPage = course.pdfIndexToPrintedPage(pageIndex)
    val previousPrintedPage = course.pdfIndexToPrintedPage((pageIndex - 1).coerceAtLeast(0))
    val nextPrintedPage = course.pdfIndexToPrintedPage((pageIndex + 1).coerceAtMost(session.pageCount - 1))
    val canPrevious = pageIndex > 0 && (activeWindow == null || previousPrintedPage in activeWindow)
    val canNext = pageIndex < session.pageCount - 1 && (activeWindow == null || nextPrintedPage in activeWindow)

    fun constrain(nextZoom: Float, nextPan: Offset): Offset {
        val maxX = viewport.width * (nextZoom - 1f) / 2f
        val maxY = viewport.height * (nextZoom - 1f) / 2f
        return Offset(nextPan.x.coerceIn(-maxX, maxX), nextPan.y.coerceIn(-maxY, maxY))
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextZoom = (zoom * zoomChange).coerceIn(1f, 5f)
        pan = if (nextZoom <= 1.001f) Offset.Zero else constrain(nextZoom, pan + panChange)
        zoom = nextZoom
    }

    LaunchedEffect(pageIndex) {
        zoom = 1f
        pan = Offset.Zero
    }

    Column(modifier = Modifier.fillMaxSize().background(ReaderBlack).systemBarsPadding()) {
        PdfReaderHeader(course.title, activeWindow, printedPage, (zoom * 100).roundToInt())
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val density = LocalDensity.current
            val baseWidth = with(density) { maxWidth.toPx().toInt() }.coerceIn(720, 2400)
            val renderMultiplier = when {
                zoom >= 3f -> 3
                zoom >= 1.8f -> 2
                else -> 1
            }
            val targetWidthPx = (baseWidth * renderMultiplier).coerceIn(720, 4200)
            LaunchedEffect(pageIndex, targetWidthPx) {
                bitmap = null
                renderError = null
                runCatching { withContext(Dispatchers.IO) { session.render(pageIndex, targetWidthPx) } }
                    .onSuccess { bitmap = it }
                    .onFailure { renderError = it.message ?: "页面渲染失败" }
            }

            when {
                renderError != null -> Text(renderError.orEmpty(), modifier = Modifier.align(Alignment.Center).padding(24.dp), color = ReaderWhite, textAlign = TextAlign.Center)
                bitmap == null -> Text("正在打开…", modifier = Modifier.align(Alignment.Center), color = ReaderMuted)
                else -> AnimatedContent(
                    targetState = bitmap,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
                    label = "pdfPage",
                ) { rendered ->
                    if (rendered != null) {
                        Box(
                            modifier = Modifier.fillMaxSize().clipToBounds().onSizeChanged { viewport = it }
                                .pointerInput(pageIndex) {
                                    detectTapGestures(onDoubleTap = {
                                        if (zoom > 1.05f) {
                                            zoom = 1f
                                            pan = Offset.Zero
                                        } else {
                                            zoom = 2f
                                            pan = Offset.Zero
                                        }
                                    })
                                }.transformable(transformState),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                bitmap = rendered.asImageBitmap(),
                                contentDescription = "教材第 $printedPage 页",
                                modifier = Modifier.fillMaxSize().graphicsLayer {
                                    scaleX = zoom
                                    scaleY = zoom
                                    translationX = pan.x
                                    translationY = pan.y
                                    transformOrigin = TransformOrigin.Center
                                },
                                contentScale = ContentScale.Fit,
                            )
                        }
                    }
                }
            }
        }

        if (readingRange != null) {
            Text(
                text = if (fullBook) "返回本节范围" else "查看完整教材",
                modifier = Modifier.fillMaxWidth().clickable {
                    fullBook = !fullBook
                    if (!fullBook) {
                        val clamped = course.pdfIndexToPrintedPage(pageIndex).coerceIn(readingRange.first, readingRange.last)
                        pageIndex = course.printedPageToPdfIndex(clamped).coerceIn(0, session.pageCount - 1)
                    }
                }.padding(vertical = 9.dp),
                color = ReaderMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().background(ReaderBlack).navigationBarsPadding().padding(horizontal = 22.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReaderAction("上一页", ReaderWhite.copy(alpha = 0.78f), Modifier.weight(1f), canPrevious) { pageIndex -= 1 }
            ReaderAction("返回", ReaderYellow, Modifier.weight(1f), onClick = onBack)
            ReaderAction("下一页", ReaderBlue, Modifier.weight(1f), canNext) { pageIndex += 1 }
        }
    }
}

@Composable
private fun PdfReaderHeader(packTitle: String, activeWindow: IntRange?, printedPage: Int, zoomPercent: Int) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (activeWindow == null) "教材" else "本节教材", color = ReaderYellow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier.background(ReaderWhite.copy(alpha = 0.07f), RoundedCornerShape(12.dp)).padding(horizontal = 11.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("第 $printedPage 页 · $zoomPercent%", color = ReaderMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false)
            }
        }
        Text(packTitle, modifier = Modifier.fillMaxWidth(), color = ReaderWhite, fontSize = 20.sp, lineHeight = 27.sp, fontWeight = FontWeight.Medium)
        activeWindow?.let { range -> Text("只查看第 ${range.first}—${range.last} 页", color = ReaderBlue, fontSize = 12.sp) }
    }
}

@Composable
private fun ReaderAction(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val effective = if (enabled) color else color.copy(alpha = 0.22f)
    Box(modifier = modifier.heightIn(min = 45.dp).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, color = effective, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ReaderError(message: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(ReaderBlack).systemBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("教材不可用", color = ReaderWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Text(message, color = ReaderMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        Text("返回", modifier = Modifier.clickable(onClick = onBack).padding(14.dp), color = ReaderBlue, fontWeight = FontWeight.Bold)
    }
}

private class PdfRenderSession(file: java.io.File) : Closeable {
    private val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(descriptor)
    val pageCount: Int get() = renderer.pageCount

    fun render(index: Int, width: Int): Bitmap {
        require(index in 0 until pageCount) { "页码超出范围" }
        renderer.openPage(index).use { page ->
            val height = (width * (page.height.toFloat() / page.width.toFloat())).toInt().coerceIn(1, 7200)
            return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                bitmap.eraseColor(AndroidColor.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
        }
    }

    override fun close() {
        renderer.close()
        descriptor.close()
    }
}
