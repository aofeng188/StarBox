package com.tianaofeng.xingxia.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tianaofeng.xingxia.MainActivity
import com.tianaofeng.xingxia.R
import com.tianaofeng.xingxia.core.AlmanacDay
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.core.FortuneCategory
import com.tianaofeng.xingxia.core.FortuneEngine
import com.tianaofeng.xingxia.core.HoroscopeEngine
import com.tianaofeng.xingxia.core.HoroscopePeriod
import com.tianaofeng.xingxia.core.ZodiacSign
import com.tianaofeng.xingxia.data.AppSettings
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.ui.theme.ThemeManager
import com.tianaofeng.xingxia.ui.theme.ThemePalette
import com.tianaofeng.xingxia.ui.theme.hex
import com.tianaofeng.xingxia.ui.theme.toColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** 刷新所有桌面小组件 */
object Widgets {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun updateAll(context: Context) {
        val app = context.applicationContext
        scope.launch {
            runCatching {
                FortuneWidget().updateAll(app)
                HoroscopeWidget().updateAll(app)
                CountdownWidget().updateAll(app)
            }
        }
    }
}

private val white = ColorProvider(Color.White)
private val dim = ColorProvider(Color.White.copy(alpha = 0.65f))

/** 小组件统一用当前主题的深色版，和 iOS 版的星空风格一致 */
private fun widgetPalette(): ThemePalette = ThemeManager.resolve(LocalDateTime.now(), Store.birth, false).first.dark

private fun openIntent(context: Context, link: String): Intent =
    Intent(Intent.ACTION_VIEW, Uri.parse(link), context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

@Composable
private fun WidgetFrame(context: Context, link: String, content: @Composable () -> Unit) {
    Box(
        GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .cornerRadius(22.dp)
            .padding(14.dp)
            .clickable(actionStartActivity(openIntent(context, link))),
    ) { content() }
}

@Composable
private fun Chip(label: String, color: Color) {
    Box(
        GlanceModifier
            .size(18.dp)
            .background(ColorProvider(color))
            .cornerRadius(9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = TextStyle(color = white, fontSize = 10.sp, fontWeight = FontWeight.Bold))
    }
}

// region 今日运势

class FortuneWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(120.dp, 120.dp), DpSize(260.dp, 120.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { FortuneContent(context) }
    }
}

class FortuneWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FortuneWidget()
}

@Composable
private fun FortuneContent(context: Context) {
    val day = DayKey.today
    val fortune = FortuneEngine.daily(Store.birth, day)
    val palette = widgetPalette()
    val accent = ColorProvider(palette.accent)
    val wide = LocalSize.current.width >= 240.dp
    WidgetFrame(context, "xingxia://fortune") {
        if (wide) {
            Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Column(GlanceModifier.width(86.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${fortune.score}", style = TextStyle(color = white, fontSize = 36.sp, fontWeight = FontWeight.Bold))
                    Text(fortune.levelName, style = TextStyle(color = accent, fontSize = 18.sp, fontWeight = FontWeight.Bold))
                    Spacer(GlanceModifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = fortune.score / 100f,
                        modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                        color = accent,
                        backgroundColor = ColorProvider(Color.White.copy(alpha = 0.15f)),
                    )
                }
                Spacer(GlanceModifier.width(12.dp))
                Column(GlanceModifier.defaultWeight()) {
                    val almanac = AlmanacDay(day)
                    Text("${day.month}月${day.day}日 · 农历${almanac.lunar.text}", style = TextStyle(color = dim, fontSize = 11.sp))
                    Spacer(GlanceModifier.height(4.dp))
                    Text(fortune.summary, style = TextStyle(color = white, fontSize = 13.sp, fontWeight = FontWeight.Medium), maxLines = 2)
                    Spacer(GlanceModifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Chip("宜", hex(0x34D399))
                        Spacer(GlanceModifier.width(4.dp))
                        Text(fortune.funYi.joinToString(" "), style = TextStyle(color = white, fontSize = 12.sp), maxLines = 1)
                    }
                    Spacer(GlanceModifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Chip("忌", hex(0xFB7185))
                        Spacer(GlanceModifier.width(4.dp))
                        Text(fortune.funJi.joinToString(" "), style = TextStyle(color = white, fontSize = 12.sp), maxLines = 1)
                    }
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        "幸运色 ${fortune.luckyColor.name} · 数字${fortune.luckyNumber} · ${fortune.luckyDirection}",
                        style = TextStyle(color = ColorProvider(fortune.luckyColor.toColor()), fontSize = 11.sp),
                        maxLines = 1,
                    )
                }
            }
        } else {
            Column(GlanceModifier.fillMaxSize()) {
                Text("今日运势", style = TextStyle(color = dim, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${fortune.score}", style = TextStyle(color = white, fontSize = 34.sp, fontWeight = FontWeight.Bold))
                    Spacer(GlanceModifier.width(6.dp))
                    Text(fortune.levelName, style = TextStyle(color = accent, fontSize = 20.sp, fontWeight = FontWeight.Bold))
                }
                LinearProgressIndicator(
                    progress = fortune.score / 100f,
                    modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                    color = accent,
                    backgroundColor = ColorProvider(Color.White.copy(alpha = 0.15f)),
                )
                Spacer(GlanceModifier.defaultWeight())
                Text("宜 " + fortune.funYi.take(2).joinToString(" "), style = TextStyle(color = white, fontSize = 12.sp), maxLines = 1)
                Text("忌 " + fortune.funJi.take(2).joinToString(" "), style = TextStyle(color = dim, fontSize = 12.sp), maxLines = 1)
            }
        }
    }
}

// endregion

// region 星座运势

class HoroscopeWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(120.dp, 120.dp), DpSize(260.dp, 120.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { HoroscopeContent(context) }
    }
}

class HoroscopeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HoroscopeWidget()
}

@Composable
private fun HoroscopeContent(context: Context) {
    val sign = Store.birth?.let { ZodiacSign.sunSign(it) } ?: ZodiacSign.entries.getOrElse(AppSettings.lastViewedSign) { ZodiacSign.ARIES }
    val h = HoroscopeEngine.horoscope(sign, HoroscopePeriod.TODAY)
    val palette = widgetPalette()
    val accent = ColorProvider(palette.accent)
    val wide = LocalSize.current.width >= 240.dp
    WidgetFrame(context, "xingxia://horoscope") {
        if (wide) {
            Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Column(GlanceModifier.width(80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(sign.glyph, style = TextStyle(color = accent, fontSize = 40.sp))
                    Text(sign.label, style = TextStyle(color = white, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                    Text("${h.score} 分", style = TextStyle(color = dim, fontSize = 12.sp))
                }
                Spacer(GlanceModifier.width(10.dp))
                Column(GlanceModifier.defaultWeight()) {
                    Text(h.comment, style = TextStyle(color = white, fontSize = 13.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                    Spacer(GlanceModifier.height(6.dp))
                    for (category in FortuneCategory.entries) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(category.shortName, style = TextStyle(color = dim, fontSize = 11.sp), modifier = GlanceModifier.width(30.dp))
                            LinearProgressIndicator(
                                progress = h.score(category) / 100f,
                                modifier = GlanceModifier.defaultWeight().height(4.dp),
                                color = accent,
                                backgroundColor = ColorProvider(Color.White.copy(alpha = 0.15f)),
                            )
                        }
                        Spacer(GlanceModifier.height(3.dp))
                    }
                    Text("速配 ${h.matchSign.label} · 幸运色 ${h.luckyColor.name}", style = TextStyle(color = dim, fontSize = 11.sp), maxLines = 1)
                }
            }
        } else {
            Column(GlanceModifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(sign.glyph, style = TextStyle(color = accent, fontSize = 28.sp))
                    Spacer(GlanceModifier.defaultWeight())
                    Text("${h.score}", style = TextStyle(color = white, fontSize = 22.sp, fontWeight = FontWeight.Bold))
                }
                Text(sign.label, style = TextStyle(color = white, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.defaultWeight())
                Text(h.comment, style = TextStyle(color = dim, fontSize = 12.sp), maxLines = 2)
            }
        }
    }
}

// endregion

// region 倒数日

class CountdownWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { CountdownContent(context) }
    }
}

class CountdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CountdownWidget()
}

@Composable
private fun CountdownContent(context: Context) {
    val palette = widgetPalette()
    val countdowns = Store.countdowns.items
    // 优先显示置顶的；否则显示最近要到的
    val chosen = countdowns.firstOrNull { it.pinned }
        ?: countdowns.filter { !it.status().isPast }.minByOrNull { it.status().days }
        ?: countdowns.firstOrNull()
    WidgetFrame(context, "xingxia://countdowns") {
        if (chosen == null) {
            Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⏳", style = TextStyle(fontSize = 28.sp))
                Text("在星匣里添加倒数日", style = TextStyle(color = dim, fontSize = 12.sp))
            }
        } else {
            val status = chosen.status()
            val color = ColorProvider(palette.color(chosen.colorIndex))
            Column(GlanceModifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(chosen.emoji, style = TextStyle(fontSize = 18.sp))
                    Spacer(GlanceModifier.width(4.dp))
                    Text(chosen.title, style = TextStyle(color = white, fontSize = 14.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                }
                Spacer(GlanceModifier.defaultWeight())
                Text(status.prefix, style = TextStyle(color = dim, fontSize = 12.sp))
                if (status.isToday && chosen.kindValue.key != "anniversary") {
                    Text(status.suffix, style = TextStyle(color = color, fontSize = 30.sp, fontWeight = FontWeight.Bold))
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${status.days}", style = TextStyle(color = color, fontSize = 38.sp, fontWeight = FontWeight.Bold))
                        Spacer(GlanceModifier.width(3.dp))
                        Text(status.suffix, style = TextStyle(color = dim, fontSize = 12.sp))
                    }
                }
                Text(status.caption, style = TextStyle(color = dim, fontSize = 11.sp), maxLines = 1)
            }
        }
    }
}

// endregion
