@file:OptIn(ExperimentalLayoutApi::class)

package com.tianaofeng.xingxia.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.StarHalf
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tianaofeng.xingxia.core.NamedColor
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.toColor
import kotlin.math.roundToInt

// region 字体

/** 与 iOS 动态字体大致对应的字号 */
object Fonts {
    val largeTitle = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold)
    val title = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)
    val title2 = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold)
    val title3 = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    val headline = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontSize = 17.sp, lineHeight = 25.sp)
    val subheadline = TextStyle(fontSize = 15.sp, lineHeight = 21.sp)
    val footnote = TextStyle(fontSize = 13.sp)
    val caption = TextStyle(fontSize = 12.sp)
    val caption2 = TextStyle(fontSize = 11.sp)

    /** 宋体风格（签诗、标题等需要古典氛围的地方） */
    fun serif(size: Int, bold: Boolean = false) = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = size.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    )

    fun display(size: Int, weight: FontWeight = FontWeight.Bold) = TextStyle(fontSize = size.sp, fontWeight = weight)
}

fun TextStyle.weight(weight: FontWeight) = copy(fontWeight = weight)

// endregion

// region 玻璃卡片

/** 半透明「玻璃」底：透出背后的星空。 */
fun Modifier.glass(shape: Shape = RoundedCornerShape(28.dp), tint: Color? = null): Modifier = composed {
    val palette = LocalPalette.current
    var result = this.clip(shape).background(palette.glassFill)
    if (tint != null) result = result.background(tint)
    result.border(0.8.dp, palette.glassStroke, shape)
}

/** 按下时轻微缩放，而不是整块变灰 */
fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium), label = "press")
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interaction, indication = null, enabled = enabled) {
            Haptics.tap()
            onClick()
        }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    padding: Dp = 18.dp,
    tint: Color? = null,
    onClick: (() -> Unit)? = null,
    spacing: Dp = 12.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier)
            .glass(shape, tint)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}

// endregion

// region 文本与标题

@Composable
fun SecondaryText(text: String, style: TextStyle = Fonts.subheadline, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE, textAlign: TextAlign? = null) {
    Text(text, modifier, color = LocalPalette.current.secondaryText, style = style, maxLines = maxLines, overflow = TextOverflow.Ellipsis, textAlign = textAlign)
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null, icon: ImageVector? = null, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    val palette = LocalPalette.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (icon != null) GradientIcon(icon, size = 18.dp)
        Text(title, style = Fonts.headline, color = palette.text)
        if (subtitle != null) {
            Text(subtitle, style = Fonts.caption, color = palette.secondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        }
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun GradientText(text: String, style: TextStyle = Fonts.largeTitle, modifier: Modifier = Modifier, textAlign: TextAlign? = null) {
    val palette = LocalPalette.current
    Text(text, modifier, style = style.copy(brush = palette.accentBrush), textAlign = textAlign)
}

/** 用强调色渐变着色的图标 */
@Composable
fun GradientIcon(icon: ImageVector, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Icon(
        icon, null,
        modifier = modifier
            .size(size)
            .gradientTint(palette.accentBrush),
        tint = Color.White,
    )
}

/** 用渐变替换内容的颜色（保留内容的形状） */
fun Modifier.gradientTint(brush: Brush): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        drawRect(brush, blendMode = BlendMode.SrcAtop)
    }

// endregion

// region 分数环、星级、进度条

@Composable
fun ScoreRing(score: Int, size: Dp = 120.dp, lineWidth: Dp = 12.dp, label: String? = null, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(score) {
        progress.animateTo(score / 100f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessVeryLow))
    }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = lineWidth.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(palette.subtle(), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            rotate(-90f) {
                drawArc(
                    Brush.sweepGradient(listOf(palette.accent, palette.accentSecondary, palette.nebula[0], palette.accent)),
                    0f, 360f * progress.value, false, Offset(inset, inset), arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${(progress.value * 100).roundToInt()}",
                style = TextStyle(fontSize = (size.value * 0.32f).sp, fontWeight = FontWeight.Bold),
                color = palette.text,
            )
            if (label != null) {
                Text(label, style = TextStyle(fontSize = (size.value * 0.11f).sp, fontWeight = FontWeight.SemiBold), color = palette.secondaryText)
            }
        }
    }
}

@Composable
fun StarRatingView(stars: Double, size: Dp = 14.dp, color: Color? = null) {
    val tint = color ?: LocalPalette.current.accent
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (index in 0 until 5) {
            val value = stars - index
            val icon = when {
                value >= 1 -> Icons.Rounded.Star
                value >= 0.5 -> Icons.Rounded.StarHalf
                else -> Icons.Rounded.StarBorder
            }
            Icon(icon, null, Modifier.size(size), tint = tint)
        }
    }
}

@Composable
fun MeterBar(value: Int, modifier: Modifier = Modifier, color: Color? = null, height: Dp = 6.dp) {
    val palette = LocalPalette.current
    val shown = remember { Animatable(0f) }
    LaunchedEffect(value) { shown.animateTo(value / 100f, tween(900)) }
    val start = color ?: palette.accent
    val end = (color ?: palette.accentSecondary).copy(alpha = 0.7f)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(palette.subtle()),
    ) {
        Box(
            Modifier
                .fillMaxWidth(shown.value.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(start, end))),
        )
    }
}

// endregion

// region 标签

@Composable
fun TagChip(text: String, color: Color? = null, icon: ImageVector? = null) {
    val tint = color ?: LocalPalette.current.accent
    Row(
        Modifier
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(12.dp), tint = tint)
        Text(text, style = Fonts.caption.weight(FontWeight.Medium), color = tint, maxLines = 1)
    }
}

/** 颜色标签：色点 + 色名。文字用主色，避免浅色（如月白）在浅底上看不清。 */
@Composable
fun ColorDotChip(named: NamedColor) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .clip(CircleShape)
            .background(palette.subtle(0.07f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        ColorDot(named.toColor(), 10.dp)
        Text(named.name, style = Fonts.caption.weight(FontWeight.Medium), color = palette.text, maxLines = 1)
    }
}

@Composable
fun ColorDot(color: Color, size: Dp, glow: Boolean = false) {
    val palette = LocalPalette.current
    Box(
        Modifier
            .size(size)
            .then(if (glow) Modifier.drawBehind { drawCircle(color.copy(alpha = 0.45f), this.size.minDimension) } else Modifier)
            .clip(CircleShape)
            .background(color)
            .border(0.5.dp, palette.subtle(0.25f), CircleShape),
    )
}

@Composable
fun Flow(spacing: Dp = 8.dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(spacing), verticalArrangement = Arrangement.spacedBy(spacing)) {
        content()
    }
}

// endregion

// region 图标

/** 带渐变底的圆形图标 */
@Composable
fun GlowIcon(icon: ImageVector, color: Color, secondary: Color? = null, size: Dp = 44.dp) {
    Box(
        Modifier
            .size(size)
            .drawBehind { drawCircle(color.copy(alpha = 0.3f), this.size.minDimension * 0.62f) }
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(color, secondary ?: color.copy(alpha = 0.6f)))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(size * 0.48f), tint = Color.White)
    }
}

/** 带渐变底的圆角方形图标（工具格子用） */
@Composable
fun ToolIcon(icon: ImageVector?, colors: List<Color>, size: Dp = 52.dp, emoji: String? = null) {
    val shape = RoundedCornerShape(size * 0.3f)
    Box(
        Modifier
            .size(size)
            .clip(shape)
            .background(Brush.linearGradient(colors))
            .border(0.8.dp, Color.White.copy(alpha = 0.35f), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (emoji != null) {
            Text(emoji, style = TextStyle(fontSize = (size.value * 0.5f).sp))
        } else if (icon != null) {
            Icon(icon, null, Modifier.size(size * 0.46f), tint = Color.White)
        }
    }
}

// endregion

// region 空状态、按钮、选择器

@Composable
fun EmptyHint(icon: ImageVector, title: String, message: String? = null, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, Modifier.size(36.dp), tint = palette.secondaryText)
        Text(title, style = Fonts.headline, color = palette.text)
        if (message != null) SecondaryText(message, textAlign = TextAlign.Center)
    }
}

/** 大号主按钮（强调色渐变胶囊） */
@Composable
fun BigActionButton(title: String, icon: ImageVector? = null, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = if (enabled) 1f else 0.45f)
            .pressable(enabled, onClick)
            .clip(CircleShape)
            .background(palette.accentBrush)
            .padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp), tint = Color.White)
            Spacer(Modifier.width(8.dp))
        }
        Text(title, style = Fonts.headline, color = Color.White)
    }
}

/** 次要按钮（玻璃胶囊） */
@Composable
fun GlassButton(title: String, icon: ImageVector? = null, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        modifier
            .graphicsLayer(alpha = if (enabled) 1f else 0.45f)
            .pressable(enabled, onClick)
            .glass(CircleShape)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp), tint = palette.accent)
            if (title.isNotEmpty()) Spacer(Modifier.width(6.dp))
        }
        if (title.isNotEmpty()) Text(title, style = Fonts.subheadline.weight(FontWeight.Medium), color = palette.text)
    }
}

/** 分段选择器（对应 iOS 的 segmented Picker） */
@Composable
fun <T> SegmentedPicker(options: List<T>, selected: T, label: (T) -> String, modifier: Modifier = Modifier, onSelect: (T) -> Unit) {
    val palette = LocalPalette.current
    Row(
        modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(14.dp))
            .padding(3.dp),
    ) {
        for (option in options) {
            val isOn = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (isOn) palette.accent.copy(alpha = if (palette.isDark) 0.35f else 0.18f) else Color.Transparent)
                    .clickable {
                        if (!isOn) {
                            Haptics.selection()
                            onSelect(option)
                        }
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = Fonts.footnote.weight(if (isOn) FontWeight.SemiBold else FontWeight.Normal),
                    color = palette.text,
                    maxLines = 1,
                )
            }
        }
    }
}

/** 单个可选胶囊 */
@Composable
fun SelectChip(text: String, selected: Boolean, icon: ImageVector? = null, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .clip(CircleShape)
            .then(if (selected) Modifier.background(palette.accentBrush) else Modifier.background(palette.subtle(0.07f)))
            .clickable {
                Haptics.selection()
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(14.dp), tint = if (selected) Color.White else palette.text)
        Text(
            text,
            style = Fonts.subheadline.weight(if (selected) FontWeight.SemiBold else FontWeight.Normal),
            color = if (selected) Color.White else palette.text,
            maxLines = 1,
        )
    }
}

/** 横向滚动的一排胶囊 */
@Composable
fun ChipRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** 一排可多选的筛选标签；空集合表示「全部」 */
@Composable
fun MultiChipPicker(options: List<String>, selection: Set<String>, allLabel: String, onChange: (Set<String>) -> Unit) {
    ChipRow {
        SelectChip(allLabel, selection.isEmpty()) { onChange(emptySet()) }
        for (option in options) {
            SelectChip(option, option in selection) {
                onChange(if (option in selection) selection - option else selection + option)
            }
        }
    }
}

/** 带说明的开关行 */
@Composable
fun ToggleRow(title: String, checked: Boolean, subtitle: String? = null, onChange: (Boolean) -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = Fonts.body, color = palette.text)
            if (subtitle != null) SecondaryText(subtitle, style = Fonts.caption)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = palette.accent, checkedThumbColor = Color.White),
        )
    }
}

/** 加减步进器 */
@Composable
fun StepperRow(title: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    val palette = LocalPalette.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = Fonts.body, color = palette.text, modifier = Modifier.weight(1f))
        Row(
            Modifier
                .clip(CircleShape)
                .background(palette.subtle(0.08f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepperButton("−", enabled = value > range.first) { onChange(value - 1) }
            Box(Modifier.width(1.dp).height(18.dp).background(palette.subtle(0.15f)))
            StepperButton("+", enabled = value < range.last) { onChange(value + 1) }
        }
    }
}

@Composable
private fun StepperButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Box(
        Modifier
            .widthIn(min = 46.dp)
            .clickable(enabled = enabled) {
                Haptics.selection()
                onClick()
            }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = Fonts.title3, color = if (enabled) palette.text else palette.tertiaryText)
    }
}

// endregion

fun TextUnit.scaled(factor: Float): TextUnit = (value * factor).sp
