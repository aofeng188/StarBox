package com.tianaofeng.xingxia.ui.screens.divination

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tianaofeng.xingxia.core.Compatibility
import com.tianaofeng.xingxia.core.DailyFortune
import com.tianaofeng.xingxia.core.EarthlyBranch
import com.tianaofeng.xingxia.core.FortuneCategory
import com.tianaofeng.xingxia.core.FortuneStick
import com.tianaofeng.xingxia.core.ZodiacSign
import com.tianaofeng.xingxia.ui.components.Flow
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GradientIcon
import com.tianaofeng.xingxia.ui.components.GradientText
import com.tianaofeng.xingxia.ui.components.StarRatingView
import com.tianaofeng.xingxia.ui.screens.common.ZodiacBadge
import com.tianaofeng.xingxia.ui.screens.common.icon
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.hex
import com.tianaofeng.xingxia.ui.theme.toColor

// 各页分享卡的内容，放在 ShareCardFrame 里；版式与 iOS 的分享卡一致

private val white92 = Color.White.copy(alpha = 0.92f)
private val white70 = Color.White.copy(alpha = 0.7f)
private val white60 = Color.White.copy(alpha = 0.6f)

// region 运势

/** 运势分享卡：分数环、等级、星级、摘要、四项指标和幸运项。name 为档案名字，「我」或空时不署名 */
@Composable
fun FortuneShareContent(fortune: DailyFortune, name: String?, style: ShareCardStyle) {
    val tall = style.isTall
    val signature = name?.takeIf { it.isNotBlank() && it != "我" }?.let { "—— 来自 $it 的分享" }
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (tall) 18.dp else 14.dp),
    ) {
        if (tall) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ShareScoreRing(fortune.score, 140.dp, 13.dp, "运势指数")
                GradientText(fortune.levelName, style = Fonts.display(40, FontWeight.Black))
                StarRatingView(fortune.stars, size = 16.dp)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                ShareScoreRing(fortune.score, 112.dp, 11.dp, "运势指数")
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradientText(fortune.levelName, style = Fonts.display(36, FontWeight.Black))
                    StarRatingView(fortune.stars, size = 15.dp)
                    if (signature != null) {
                        Text(signature, style = TextStyle(fontSize = 11.sp, color = white60), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        Text(
            fortune.summary,
            style = TextStyle(fontSize = if (tall) 16.sp else 14.sp, lineHeight = if (tall) 24.sp else 21.sp, color = white92, textAlign = TextAlign.Center),
            maxLines = if (tall) 4 else 3,
            overflow = TextOverflow.Ellipsis,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (category in FortuneCategory.entries) {
                val value = fortune.score(category)
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                        .padding(vertical = 9.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    GradientIcon(category.icon, 13.dp)
                    Text(category.shortName, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = white70), maxLines = 1)
                    Text("$value", style = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White))
                    ShareMeter(value)
                }
            }
        }

        Flow(spacing = 6.dp, horizontalAlignment = Alignment.CenterHorizontally) {
            ShareChip("幸运色 ${fortune.luckyColor.name}", fortune.luckyColor.toColor())
            ShareChip("幸运数字 ${fortune.luckyNumber}")
            ShareChip("喜神 ${fortune.luckyDirection}")
            if (tall) {
                ShareChip("吉时 ${fortune.luckyHour.hourName}")
                ShareChip("幸运物 ${fortune.luckyItem}")
                ShareChip("贵人 ${fortune.noblemanSign.label}")
            }
        }

        if (tall && signature != null) {
            Text(signature, style = TextStyle(fontSize = 12.sp, color = white60), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// endregion

// region 签诗

/** 签诗分享卡：签号、签题、竖排签诗（从右往左）、等级红印章、解曰和所求。forName 为家人朋友的名字，confirmed 为已掷筊确认 */
@Composable
fun StickShareContent(stick: FortuneStick, wish: String, forName: String?, confirmed: Boolean, style: ShareCardStyle) {
    val tall = style.isTall
    val charSize = if (tall) 24 else 17
    val gold = hex(0xFFD166)
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (tall) 16.dp else 10.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(if (tall) 6.dp else 3.dp)) {
            if (forName != null) {
                Text("为 $forName 求得", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = white70), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                stick.numberText,
                style = Fonts.serif(if (tall) 32 else 26, bold = true).copy(
                    letterSpacing = if (tall) 6.sp else 4.sp,
                    lineHeight = if (tall) 38.sp else 31.sp,
                    color = Color.White,
                ),
                maxLines = 1,
            )
            Text(
                "「${stick.title}」",
                style = Fonts.serif(if (tall) 17 else 14).copy(lineHeight = if (tall) 22.sp else 18.sp, color = Color.White.copy(alpha = 0.85f)),
                maxLines = 1,
            )
        }

        Box {
            Row(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(hex(0xFFF6E0, 0.12f), gold.copy(alpha = 0.05f))))
                    .border(1.dp, gold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(vertical = if (tall) 18.dp else 12.dp, horizontal = if (tall) 26.dp else 20.dp)
                    .clearAndSetSemantics {
                        contentDescription = stick.poem.joinToString("，") + "，" + stick.levelDisplayName + if (confirmed) "，已掷筊确认" else ""
                    },
                horizontalArrangement = Arrangement.spacedBy(if (tall) 18.dp else 14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                // 每个字固定行高，导出的图每次一样
                val charStyle = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = charSize.sp,
                    lineHeight = (charSize * 1.18f).sp,
                    color = white92.copy(alpha = 0.95f),
                    textAlign = TextAlign.Center,
                )
                for (line in stick.poem.reversed()) {
                    Column(Modifier.width((charSize * 1.25f).dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        for (char in line) Text(char.toString(), style = charStyle)
                    }
                }
            }
            StickLevelSeal(
                stick,
                if (tall) 64 else 54,
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = if (tall) 34.dp else 30.dp, y = if (tall) (-24).dp else (-20).dp),
            )
            if (confirmed) {
                JiaobeiSeal(
                    if (tall) 13 else 11,
                    Modifier
                        .align(Alignment.TopStart)
                        .offset(x = if (tall) (-26).dp else (-22).dp, y = if (tall) (-14).dp else (-12).dp)
                        .graphicsLayer { rotationZ = -10f },
                )
            }
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(if (tall) 10.dp else 7.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("解曰", style = Fonts.serif(if (tall) 15 else 13, bold = true).copy(color = gold))
                Text(
                    stick.meaning,
                    style = TextStyle(fontSize = if (tall) 14.sp else 12.sp, lineHeight = if (tall) 21.sp else 18.sp, color = white92),
                    maxLines = if (tall) 4 else 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (wish != "综合") ShareChip("所求 · $wish")
                ShareChip(stick.blessing)
            }
        }
    }
}

/** 等级红印章：旋转、半透明 */
@Composable
private fun StickLevelSeal(stick: FortuneStick, size: Int, modifier: Modifier = Modifier) {
    val red = hex(0xF05A4F)
    val shape = RoundedCornerShape((size * 0.14f).dp)
    Box(
        modifier
            .graphicsLayer {
                rotationZ = -12f
                alpha = 0.88f
            }
            .size(size.dp)
            .clip(shape)
            .background(red.copy(alpha = 0.12f))
            .border((size * 0.045f).dp, red, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (stick.level == "下") "下签" else stick.level, style = Fonts.serif((size * 0.32f).toInt(), bold = true).copy(color = red), maxLines = 1)
    }
}

// endregion

// region 塔罗

/** 塔罗解读分享卡：牌阵名、问题、每张牌的牌名和正逆、综合解读一句话 */
@Composable
internal fun TarotShareContent(spread: TarotSpread, question: String, entries: List<DrawnCard>, style: ShareCardStyle) {
    val tall = style.isTall
    val palette = LocalPalette.current
    val asked = question.trim()
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (tall) 18.dp else 12.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "${spread.label} · ${entries.size} 张",
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp, brush = palette.accentBrush),
            )
            if (asked.isNotEmpty()) {
                Text(
                    "「$asked」",
                    style = Fonts.serif(if (tall) 18 else 15).copy(color = Color.White, textAlign = TextAlign.Center),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        val single = entries.singleOrNull()
        if (single != null && !tall) {
            TarotShareSingleRow(spread, single)
        } else {
            val width = if (entries.size == 1) 136 else if (tall) 92 else 78
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                entries.forEachIndexed { slot, entry ->
                    Column(
                        Modifier.width(maxOf(width, 90).dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        TarotShareCardArt(entry, width)
                        Text(spread.position(slot), style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = white60), maxLines = 1)
                        Text(entry.card.name, style = Fonts.serif(14, bold = true).copy(color = Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        TarotShareOrientation(entry)
                    }
                }
            }
        }

        Text(
            tarotSummaryText(entries),
            style = TextStyle(fontSize = if (tall) 15.sp else 13.sp, lineHeight = if (tall) 22.sp else 19.sp, color = white92, textAlign = TextAlign.Center),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (tall) {
            Text(
                "塔罗是一面映照内心的镜子，答案最终在你自己手中。",
                style = TextStyle(fontSize = 11.sp, color = Color.White.copy(alpha = 0.55f), textAlign = TextAlign.Center),
            )
        }
    }
}

/** 单张牌、3:4：牌在左，牌名、正逆、关键词和释义在右 */
@Composable
private fun TarotShareSingleRow(spread: TarotSpread, entry: DrawnCard) {
    val card = entry.card
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        TarotShareCardArt(entry, 108)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(spread.position(0), style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = white60))
            Text(card.name, style = Fonts.serif(20, bold = true).copy(color = Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis)
            TarotShareOrientation(entry)
            val keywords = card.keywordsFor(entry.reversed)
            if (!keywords.isNullOrEmpty()) {
                Text(
                    keywords.take(3).joinToString(" · "),
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = tarotAccent(card)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                if (entry.reversed) card.reversed else card.upright,
                style = TextStyle(fontSize = 12.sp, lineHeight = 17.sp, color = Color.White.copy(alpha = 0.85f)),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TarotShareCardArt(entry: DrawnCard, width: Int) {
    TarotCardFace(
        entry.card,
        Modifier
            .size(width.dp, (width * 1.62f).dp)
            .graphicsLayer { rotationZ = if (entry.reversed) 180f else 0f }
            .clearAndSetSemantics { },
    )
}

@Composable
private fun TarotShareOrientation(entry: DrawnCard) {
    val palette = LocalPalette.current
    Text(
        if (entry.reversed) "逆位" else "正位",
        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (entry.reversed) palette.negative else palette.positive),
    )
}

// endregion

// region 答案之书

/** 答案之书分享卡：问题（有的话）和答案，金色书页 */
@Composable
fun AnswerShareContent(question: String, answer: String, style: ShareCardStyle) {
    val tall = style.isTall
    val asked = question.trim()
    val brown = hex(0x4A2C0A)
    val ochre = hex(0xC08A2E)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(if (tall) 22.dp else 14.dp)) {
        if (asked.isNotEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("我的问题", style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp, color = white60))
                Text(
                    "「$asked」",
                    style = Fonts.serif(if (tall) 18 else 15).copy(color = Color.White, textAlign = TextAlign.Center),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        val width = if (tall) 270 else 240
        val height = if (tall) (if (asked.isEmpty()) 380 else 330) else (if (asked.isEmpty()) 300 else 250)
        val pageShape = RoundedCornerShape(14.dp)
        Box(
            Modifier
                .size(width.dp, height.dp)
                .drawBehind {
                    // 书页周围的金色光晕
                    val glow = hex(0xFFD166, 0.4f)
                    drawCircle(
                        Brush.radialGradient(listOf(glow, Color.Transparent), center, size.maxDimension * 0.75f),
                        size.maxDimension * 0.75f,
                        center,
                    )
                }
                .clip(pageShape)
                .background(Brush.verticalGradient(listOf(hex(0xFFF8E7), hex(0xF3E3C3))))
                .border(3.dp, Brush.linearGradient(listOf(hex(0xF5D78E), ochre), Offset.Zero, Offset.Infinite), pageShape),
            contentAlignment = Alignment.Center,
        ) {
            // 内框：跟书页一样大，内缩 12dp
            Box(
                Modifier
                    .matchParentSize()
                    .padding(12.dp)
                    .border(1.dp, ochre.copy(alpha = 0.45f), RoundedCornerShape(9.dp)),
            )
            Column(
                Modifier.padding(horizontal = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text("✦", style = Fonts.title3.copy(color = ochre))
                Text(
                    answer,
                    style = Fonts.serif(if (answer.length > 9) 22 else 26, bold = true).copy(color = brown, textAlign = TextAlign.Center, lineHeight = if (answer.length > 9) 32.sp else 36.sp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("✦", style = Fonts.title3.copy(color = ochre))
            }
            Text(
                "THE BOOK OF ANSWERS",
                style = TextStyle(fontFamily = FontFamily.Serif, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp, color = ochre.copy(alpha = 0.8f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 22.dp)
                    .clearAndSetSemantics { },
            )
        }
    }
}

// endregion

// region 缘分配对

/** 缘分配对分享卡里的一方：星座、生肖（选星座时没有）、名字、头像（选星座时为空，画星座徽章） */
data class ShareSide(val sign: ZodiacSign, val animal: EarthlyBranch?, val title: String, val avatar: String)

/** 缘分配对结果页顶栏的分享按钮（缘分配对页在 profile/ProfileScreens.kt，由那边放进 Screen 的 actions） */
@Composable
fun CompatibilityShareButton(left: ShareSide, right: ShareSide, result: Compatibility) {
    ShareCardButton("分享配对", "match") { style ->
        ShareCardFrame(style, title = "星匣 · 缘分配对") {
            CompatibilityShareContent(left, right, result, style)
        }
    }
}

/** 缘分配对分享卡：两人的头像和名字、星座和生肖、配对分数和一句话结论 */
@Composable
fun CompatibilityShareContent(left: ShareSide, right: ShareSide, result: Compatibility, style: ShareCardStyle) {
    val tall = style.isTall
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (tall) 18.dp else 14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            ShareSideView(left, tall, Modifier.weight(1f))
            Box(
                Modifier
                    .width(44.dp)
                    .padding(top = if (tall) 24.dp else 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                GradientIcon(Icons.Rounded.Favorite, if (tall) 26.dp else 22.dp)
            }
            ShareSideView(right, tall, Modifier.weight(1f))
        }

        if (tall) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ShareScoreRing(result.score, 130.dp, 12.dp, "契合度")
                GradientText(result.title, style = Fonts.display(32, FontWeight.Black))
                Text("${left.sign.label} × ${right.sign.label} · ${result.aspect.label}", style = TextStyle(fontSize = 12.sp, color = Color.White.copy(alpha = 0.65f)))
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                ShareScoreRing(result.score, 96.dp, 10.dp, "契合度")
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GradientText(result.title, style = Fonts.display(28, FontWeight.Black))
                    Text("${left.sign.label} × ${right.sign.label}", style = TextStyle(fontSize = 12.sp, color = Color.White.copy(alpha = 0.65f)))
                    Text(result.aspect.label, style = TextStyle(fontSize = 12.sp, color = Color.White.copy(alpha = 0.65f)))
                }
            }
        }

        Text(
            firstSentence(result.summary),
            style = TextStyle(fontSize = if (tall) 16.sp else 14.sp, lineHeight = if (tall) 24.sp else 21.sp, color = white92, textAlign = TextAlign.Center),
            maxLines = if (tall) 4 else 3,
            overflow = TextOverflow.Ellipsis,
        )
        // 9:16 再加一句生肖或元素的说明
        if (tall) {
            Text(
                result.animalNote ?: result.elementNote,
                style = TextStyle(fontSize = 13.sp, lineHeight = 19.sp, color = white70, textAlign = TextAlign.Center),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ShareSideView(side: ShareSide, tall: Boolean, modifier: Modifier) {
    val palette = LocalPalette.current
    val size = if (tall) 72 else 60
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // 头像是装饰，读屏读名字和星座
        if (side.avatar.isEmpty()) {
            ZodiacBadge(side.sign, size.dp, Modifier.clearAndSetSemantics { })
        } else {
            Box(
                Modifier
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(palette.accent.copy(alpha = 0.22f))
                    .border(1.dp, palette.accent.copy(alpha = 0.5f), CircleShape)
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.Center,
            ) {
                Text(side.avatar, style = TextStyle(fontSize = (size * 0.55f).sp))
            }
        }
        Text(side.title, style = TextStyle(fontSize = if (tall) 18.sp else 16.sp, fontWeight = FontWeight.Bold, color = Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            side.animal?.let { "${side.sign.label} · 属${it.animal}" } ?: side.sign.dateRange,
            style = TextStyle(fontSize = 11.sp, color = Color.White.copy(alpha = 0.65f)),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 取摘要的第一句当结论 */
private fun firstSentence(text: String): String {
    val end = text.indexOfFirst { it in "。！？" }
    return if (end < 0) text else text.substring(0, end + 1)
}

// endregion
