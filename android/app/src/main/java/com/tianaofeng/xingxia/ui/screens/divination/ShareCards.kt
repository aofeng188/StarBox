package com.tianaofeng.xingxia.ui.screens.divination

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tianaofeng.xingxia.core.DateText
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.core.SeededGenerator
import com.tianaofeng.xingxia.core.lunar
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.ui.components.BigActionButton
import com.tianaofeng.xingxia.ui.components.CircleIconButton
import com.tianaofeng.xingxia.ui.components.CosmicBackdrop
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GlassButton
import com.tianaofeng.xingxia.ui.components.LocalGlassBackdrop
import com.tianaofeng.xingxia.ui.components.Haptics
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.SegmentedPicker
import com.tianaofeng.xingxia.ui.components.gradientTint
import com.tianaofeng.xingxia.ui.components.rememberPowerSaveMode
import com.tianaofeng.xingxia.ui.components.shareImage
import com.tianaofeng.xingxia.ui.theme.AppTheme
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.ThemeManager
import com.tianaofeng.xingxia.ui.theme.ThemePalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// region 比例与样式

/** 分享卡比例。尺寸单位是 dp，导出时按 3 倍出图（1080 宽），与 iOS 的点尺寸一致 */
enum class ShareCardRatio(val label: String, val width: Int, val height: Int) {
    /** 朋友圈、小红书 */
    PORTRAIT("3:4 朋友圈", 360, 480),

    /** 故事、壁纸 */
    STORY("9:16 故事", 360, 640),
}

/** 分享卡一律用主题的深色版本，文字用白色系 */
data class ShareCardStyle(val ratio: ShareCardRatio, val theme: AppTheme) {
    val palette: ThemePalette get() = theme.dark
    val width: Dp get() = ratio.width.dp
    val height: Dp get() = ratio.height.dp

    /** 9:16 更高，内容可以排得舒展些 */
    val isTall: Boolean get() = ratio == ShareCardRatio.STORY
}

/** 导出倍数：360dp 宽 → 1080 像素 */
private const val EXPORT_SCALE = 3f

// endregion

// region 卡片外框

/**
 * 分享卡外框：主题深色渐变 + 星云色块 + 固定星点的背景，顶部标题，底部「公历日期 · 农历 · 星匣」页脚，内容放在中间。
 * 尺寸固定；字号由预览处固定的 fontScale 决定，不随系统字号变化，导出的图片每次都一样。
 */
@Composable
fun ShareCardFrame(style: ShareCardStyle, day: DayKey = DayKey.today, title: String, content: @Composable ColumnScope.() -> Unit) {
    val palette = style.palette
    // 卡片有自己的背景，会被导出成图片：里面的玻璃不去模糊弹层的星空
    CompositionLocalProvider(LocalPalette provides palette, LocalContentColor provides Color.White, LocalGlassBackdrop provides null) {
        Column(
            Modifier
                .requiredSize(style.width, style.height)
                .drawBehind { drawShareBackground(palette) }
                .padding(start = 24.dp, end = 24.dp, top = 26.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                title,
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp),
                color = Color.White.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(10.dp))
            Spacer(Modifier.weight(1f))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, content = content)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(10.dp))
            ShareCardFooter(day, palette)
        }
    }
}

@Composable
private fun ShareCardFooter(day: DayKey, palette: ThemePalette) {
    val lunar = day.lunar()
    val textStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.72f))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Color.White.copy(alpha = 0.18f)),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(String.format(Locale.ROOT, "%d.%02d.%02d", day.year, day.month, day.day) + " · " + DateText.weekday(day), style = textStyle, maxLines = 1)
                Text("农历${lunar.yearGanZhi.name}年 ${lunar.text}", style = textStyle, maxLines = 1)
            }
            Spacer(Modifier.width(8.dp))
            Row(
                Modifier.clearAndSetSemantics { contentDescription = "星匣" },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome, null,
                    Modifier
                        .size(13.dp)
                        .gradientTint(palette.accentBrush),
                    tint = Color.White,
                )
                Text("星匣", style = Fonts.serif(17, bold = true).copy(brush = palette.accentBrush))
            }
        }
    }
}

/** 星云色块：颜色序号、圆心（按宽高比例）、半径（按宽度比例）、不透明度 */
private class ShareBlob(val index: Int, val x: Float, val y: Float, val radius: Float, val alpha: Float)

private val SHARE_BLOBS = listOf(
    ShareBlob(0, 0.12f, 0.1f, 0.8f, 0.55f),
    ShareBlob(3, 0.92f, 0.06f, 0.45f, 0.32f),
    ShareBlob(1, 0.95f, 0.5f, 0.65f, 0.38f),
    ShareBlob(2, 0.1f, 0.95f, 0.8f, 0.45f),
)

/** 分享卡的星点：半径单位是 dp，tint 为 -1 时用主题的星色 */
private class ShareStar(val x: Float, val y: Float, val radius: Float, val brightness: Float, val tint: Int, val sparkle: Boolean)

/** 固定种子生成（与 iOS 同一个种子和取数顺序），每次导出都一样 */
private val SHARE_STARS: List<ShareStar> by lazy {
    val rng = SeededGenerator(20_261_016uL)
    List(90) {
        val big = rng.chance(0.1)
        val x = rng.unit().toFloat()
        val y = rng.unit().toFloat()
        val radius = (if (big) rng.double(1.4, 2.2) else rng.double(0.4, 1.1)).toFloat()
        val brightness = rng.double(0.35, 0.95).toFloat()
        val tint = if (rng.chance(0.25)) rng.intBelow(4) else -1
        val sparkle = big && rng.chance(0.6)
        ShareStar(x, y, radius, brightness, tint, sparkle)
    }
}

/** 背景：主题深色渐变 + 星云色块 + 固定星点，底部压暗一点让页脚更清楚 */
private fun DrawScope.drawShareBackground(palette: ThemePalette) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(palette.backgroundTop, palette.backgroundBottom)))
    for (blob in SHARE_BLOBS) {
        val color = palette.nebula[blob.index % palette.nebula.size]
        val center = Offset(w * blob.x, h * blob.y)
        val radius = w * blob.radius
        drawCircle(
            Brush.radialGradient(listOf(color.copy(alpha = blob.alpha), color.copy(alpha = blob.alpha * 0.3f), Color.Transparent), center, radius),
            radius,
            center,
        )
    }
    val unit = density
    for (star in SHARE_STARS) {
        val point = Offset(star.x * w, star.y * h)
        val color = if (star.tint >= 0) palette.nebula[star.tint % palette.nebula.size] else palette.star
        val r = star.radius * unit
        if (star.radius > 1.3f) {
            drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.35f * star.brightness), Color.Transparent), point, r * 4), r * 4, point)
        }
        drawCircle(color.copy(alpha = star.brightness), r, point)
        if (star.sparkle) {
            val len = r * 4.5f
            val cross = color.copy(alpha = 0.45f * star.brightness)
            drawLine(cross, Offset(point.x - len, point.y), Offset(point.x + len, point.y), 0.6f * unit)
            drawLine(cross, Offset(point.x, point.y - len), Offset(point.x, point.y + len), 0.6f * unit)
        }
    }
    drawRect(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.22f)))
}

// endregion

// region 卡片里常用的静态元素

/** 分享卡里的分数环：静态绘制（页面上的 ScoreRing 带出现动画，导出时会画成 0） */
@Composable
fun ShareScoreRing(score: Int, diameter: Dp = 120.dp, lineWidth: Dp = 11.dp, label: String? = null) {
    val palette = LocalPalette.current
    val progress = score.coerceIn(0, 100) / 100f
    Box(
        Modifier
            .size(diameter)
            .clearAndSetSemantics { contentDescription = "${label ?: "分数"} $score 分" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = lineWidth.toPx()
            val inset = stroke / 2
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(Color.White.copy(alpha = 0.12f), 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
            rotate(-90f) {
                drawArc(
                    Brush.sweepGradient(listOf(palette.accent, palette.accentSecondary, palette.nebula[0], palette.accent)),
                    0f, 360f * progress, false, Offset(inset, inset), arc,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$score", style = TextStyle(fontSize = (diameter.value * 0.32f).sp, fontWeight = FontWeight.Bold, color = Color.White))
            if (label != null) {
                Text(label, style = TextStyle(fontSize = (diameter.value * 0.11f).sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.65f)))
            }
        }
    }
}

/** 分享卡里的静态进度条 */
@Composable
fun ShareMeter(value: Int, modifier: Modifier = Modifier, height: Dp = 4.dp) {
    val palette = LocalPalette.current
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(value.coerceIn(0, 100) / 100f)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(palette.accentBrush),
        )
    }
}

/** 分享卡里的小标签（白字半透明底），可带一个色点 */
@Composable
fun ShareChip(text: String, dot: Color? = null) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (dot != null) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dot),
            )
        }
        Text(text, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.9f)), maxLines = 1)
    }
}

// endregion

// region 分享按钮与预览

/**
 * 顶栏的分享按钮：点开预览，可切换 3:4 / 9:16，用系统分享面板分享图片，或存到相册（Android 10 起）。
 * 主题取当前主题的深色版本，卡片内容由 card 按传入的样式画出来。tag 用在导出的文件名里（只用英文字母）。
 */
@Composable
fun ShareCardButton(title: String, tag: String, card: @Composable (ShareCardStyle) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    CircleIconButton(Icons.Rounded.Share, title) { open = true }
    if (open) ShareCardDialog(title, tag, onDismiss = { open = false }, card = card)
}

/** 分享卡预览。open 由调用方持有时用这个（例如需要据此暂停摇一摇） */
@Composable
fun ShareCardButton(title: String, tag: String, open: MutableState<Boolean>, card: @Composable (ShareCardStyle) -> Unit) {
    CircleIconButton(Icons.Rounded.Share, title) { open.value = true }
    if (open.value) ShareCardDialog(title, tag, onDismiss = { open.value = false }, card = card)
}

@Composable
fun ShareCardDialog(
    title: String,
    tag: String,
    onDismiss: () -> Unit,
    initialRatio: ShareCardRatio = ShareCardRatio.PORTRAIT,
    card: @Composable (ShareCardStyle) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val powerSave = rememberPowerSaveMode()
    // 与 App 当前用的主题一致（含省电模式下的「黑洞」）
    val theme = remember(powerSave) { ThemeManager.resolve(LocalDateTime.now(), Store.birth, powerSave).first }
    var ratio by rememberSaveable { mutableStateOf(initialRatio) }
    val style = ShareCardStyle(ratio, theme)
    val layer = rememberGraphicsLayer()
    var busy by remember { mutableStateOf(false) }
    /** 已经存过的比例（换比例后可以再存） */
    var savedRatio by remember { mutableStateOf<ShareCardRatio?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val canSave = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /** 取预览里录好的那张；比例刚切换、还没画到新尺寸时等一帧再取 */
    suspend fun capture(expected: ShareCardStyle): Bitmap? {
        val wantWidth = (expected.ratio.width * EXPORT_SCALE).toInt()
        val wantHeight = (expected.ratio.height * EXPORT_SCALE).toInt()
        repeat(3) {
            val bitmap = runCatching { layer.toImageBitmap().asAndroidBitmap() }.getOrNull()
            if (bitmap != null && bitmap.width == wantWidth && bitmap.height == wantHeight) {
                // 硬件位图不能直接压缩成文件，转成普通位图
                return if (bitmap.config == Bitmap.Config.HARDWARE) bitmap.copy(Bitmap.Config.ARGB_8888, false) else bitmap
            }
            withFrameNanos { }
        }
        return null
    }

    fun fileName(expected: ShareCardStyle): String {
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        return "xingxia-$tag-$stamp-${if (expected.isTall) "9x16" else "3x4"}.png"
    }

    fun share() {
        if (busy) return
        busy = true
        val expected = style
        scope.launch {
            val bitmap = capture(expected)
            busy = false
            if (bitmap == null) {
                message = "图片生成失败，请稍后再试"
                Haptics.error()
                return@launch
            }
            message = null
            shareImage(context, bitmap, fileName(expected))
        }
    }

    fun save() {
        if (busy || !canSave) return
        busy = true
        val expected = style
        scope.launch {
            val bitmap = capture(expected)
            val ok = bitmap != null && withContext(Dispatchers.IO) { saveToGallery(context, bitmap, fileName(expected)) }
            busy = false
            if (ok) {
                message = null
                savedRatio = expected.ratio
                Haptics.success()
                view.announceForAccessibility("已存到相册")
            } else {
                message = "没能存到相册，请稍后再试"
                Haptics.error()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val palette = LocalPalette.current
        // 全屏弹层自己的星空背景，里面的玻璃模糊它（不是被盖住的主窗口）
        CosmicBackdrop {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    CircleIconButton(Icons.Rounded.Close, "关闭", Modifier.align(Alignment.CenterStart), onClick = onDismiss)
                    Text(
                        title,
                        style = Fonts.headline,
                        color = palette.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 56.dp)
                            .semantics { heading() },
                    )
                }
                SegmentedPicker(ShareCardRatio.entries, ratio, { it.label }) {
                    ratio = it
                    message = null
                }
                BoxWithConstraints(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    ShareCardPreview(style, layer, maxWidth, maxHeight, card)
                }
                message?.let {
                    SecondaryText(it, style = Fonts.footnote, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                BigActionButton(if (busy) "生成中…" else "分享", Icons.Rounded.Share, enabled = !busy) { share() }
                if (canSave) {
                    val saved = savedRatio == ratio
                    GlassButton(
                        if (saved) "已存到相册" else "存到相册",
                        if (saved) Icons.Rounded.Check else Icons.Rounded.Download,
                        enabled = !busy && !saved,
                    ) { save() }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

/**
 * 预览：卡片按导出尺寸（360dp × 3 倍密度）排版，录进 layer，再整体缩小显示。
 * 预览和导出的是同一份绘制，读屏也能读到卡片里的文字。
 */
@Composable
private fun ShareCardPreview(style: ShareCardStyle, layer: GraphicsLayer, maxWidth: Dp, maxHeight: Dp, card: @Composable (ShareCardStyle) -> Unit) {
    val density = LocalDensity.current
    val cardWidth = style.ratio.width * EXPORT_SCALE
    val cardHeight = style.ratio.height * EXPORT_SCALE
    val availableWidth = with(density) { maxWidth.toPx() }
    val availableHeight = with(density) { maxHeight.toPx() }
    val scale = minOf(1f, availableWidth / cardWidth, availableHeight / cardHeight).coerceAtLeast(0.05f)
    val shownWidth = with(density) { (cardWidth * scale).toDp() }
    val shownHeight = with(density) { (cardHeight * scale).toDp() }
    Box(
        Modifier
            .size(shownWidth, shownHeight)
            .clip(RoundedCornerShape(20.dp)),
    ) {
        // 固定密度和字号缩放：导出的图片总是 1080 宽，字号不随系统设置变化
        CompositionLocalProvider(LocalDensity provides Density(EXPORT_SCALE, 1f)) {
            Box(
                Modifier
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                    .drawWithContent {
                        layer.record { this@drawWithContent.drawContent() }
                        drawLayer(layer)
                    },
            ) {
                card(style)
            }
        }
    }
}

/** 存到系统相册的「图片/星匣」里。Android 10 起不需要存储权限，更早的系统不支持 */
private fun saveToGallery(context: Context, bitmap: Bitmap, displayName: String): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/星匣")
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }
    val uri = runCatching { resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) }.getOrNull() ?: return false
    val written = runCatching {
        resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } == true
    }.getOrDefault(false)
    if (!written) {
        runCatching { resolver.delete(uri, null, null) }
        return false
    }
    values.clear()
    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
    runCatching { resolver.update(uri, values, null, null) }
    return true
}

// endregion
