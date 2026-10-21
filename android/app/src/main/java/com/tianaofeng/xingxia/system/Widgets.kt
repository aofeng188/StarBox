package com.tianaofeng.xingxia.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
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
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tianaofeng.xingxia.MainActivity
import com.tianaofeng.xingxia.R
import com.tianaofeng.xingxia.core.AlmanacDay
import com.tianaofeng.xingxia.core.CountdownKind
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.core.FortuneCategory
import com.tianaofeng.xingxia.core.FortuneEngine
import com.tianaofeng.xingxia.core.HoroscopeEngine
import com.tianaofeng.xingxia.core.HoroscopePeriod
import com.tianaofeng.xingxia.core.TrueRandom
import com.tianaofeng.xingxia.core.ZodiacSign
import com.tianaofeng.xingxia.data.AppSettings
import com.tianaofeng.xingxia.data.CountdownItem
import com.tianaofeng.xingxia.data.Prefs
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.ui.screens.tools.ToolKind
import com.tianaofeng.xingxia.ui.theme.ThemeManager
import com.tianaofeng.xingxia.ui.theme.ThemePalette
import com.tianaofeng.xingxia.ui.theme.hex
import com.tianaofeng.xingxia.ui.theme.readableForeground
import com.tianaofeng.xingxia.ui.theme.toColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/** 刷新所有桌面小组件 */
object Widgets {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** onFinished 在刷新结束后调用（BroadcastReceiver 用 goAsync 等待它） */
    fun updateAll(context: Context, onFinished: () -> Unit = {}) {
        val app = context.applicationContext
        scope.launch {
            try {
                runCatching {
                    FortuneWidget().updateAll(app)
                    HoroscopeWidget().updateAll(app)
                    CountdownWidget().updateAll(app)
                    MoodWidget().updateAll(app)
                    CoinWidget().updateAll(app)
                }
            } finally {
                onFinished()
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
        // 亮色底上用深字（与 iOS 小组件的宜忌徽章一致）
        Text(label, style = TextStyle(color = ColorProvider(color.readableForeground), fontSize = 10.sp, fontWeight = FontWeight.Bold))
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
                        // 幸运色可能很深（如墨色），提亮后再当文字色
                        style = TextStyle(color = ColorProvider(palette.ink(fortune.luckyColor.toColor())), fontSize = 11.sp),
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
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent { HoroscopeContent(context, WidgetConfig.horoscopeSign(context, appWidgetId)) }
    }
}

class HoroscopeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HoroscopeWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        WidgetConfig.remove(context, appWidgetIds)
    }
}

/** configured 为小组件里选定的星座；没选时跟随当前档案（没有档案则用最近查看的星座） */
@Composable
private fun HoroscopeContent(context: Context, configured: ZodiacSign?) {
    val sign = configured
        ?: Store.birth?.let { ZodiacSign.sunSign(it) }
        ?: ZodiacSign.entries.getOrElse(AppSettings.lastViewedSign) { ZodiacSign.ARIES }
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
                    Text("速配 ${h.matchSign.label} · 星座幸运色 ${h.luckyColor.name}", style = TextStyle(color = dim, fontSize = 11.sp), maxLines = 1)
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
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent { CountdownContent(context, WidgetConfig.countdownId(context, appWidgetId)) }
    }
}

class CountdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CountdownWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        WidgetConfig.remove(context, appWidgetIds)
    }
}

/**
 * configuredId 为小组件里选定的倒数日（档案生日用档案 id）；没选或已删除时显示置顶的，再退回最近要到的。
 * 含档案生日（App 里关掉了就不含）。
 */
@Composable
private fun CountdownContent(context: Context, configuredId: String?) {
    val palette = widgetPalette()
    val items = CountdownItem.current()
    // 纪念日的天数是「已经过了几天」，不参与「最近」的比较（与记录页一致）
    val chosen = items.firstOrNull { it.id == configuredId }
        ?: items.firstOrNull { it.pinned }
        ?: CountdownItem.nearest(items)?.first
        ?: items.firstOrNull()
    WidgetFrame(context, "xingxia://countdowns") {
        if (chosen == null) {
            Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⏳", style = TextStyle(fontSize = 28.sp))
                Text("在星匣里添加倒数日", style = TextStyle(color = dim, fontSize = 12.sp))
            }
        } else {
            val status = chosen.status()
            val color = ColorProvider(palette.inkColor(chosen.colorIndex))
            Column(GlanceModifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(chosen.emoji, style = TextStyle(fontSize = 18.sp))
                    Spacer(GlanceModifier.width(4.dp))
                    Text(chosen.title, style = TextStyle(color = white, fontSize = 14.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                }
                Spacer(GlanceModifier.defaultWeight())
                Text(status.prefix, style = TextStyle(color = dim, fontSize = 12.sp))
                if (status.isToday && chosen.kind != CountdownKind.ANNIVERSARY) {
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

// region 心情（可点）

/** 小组件里的五个心情，和 App 的心情打卡一一对应（1…5，与 iOS 的 MoodWidget 一致） */
private class WidgetMood(val value: Int, val emoji: String, val name: String, val color: Color)

private val widgetMoods = listOf(
    WidgetMood(1, "😣", "很糟", hex(0x8B95A7)),
    WidgetMood(2, "😔", "低落", hex(0x7C9CF5)),
    WidgetMood(3, "😐", "一般", hex(0x5ECFD6)),
    WidgetMood(4, "🙂", "不错", hex(0x7EE08A)),
    WidgetMood(5, "🤩", "超棒", hex(0xFFC94D)),
)

private val MoodValueKey = ActionParameters.Key<Int>("mood")

class MoodWidget : GlanceAppWidget() {
    // 按实际尺寸排版：桌面格子窄时表情按钮跟着缩小，不被裁掉
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { MoodContent(context) }
    }
}

class MoodWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MoodWidget()
}

/**
 * 点表情记下今天的心情，今天已有就更新。小组件和 App 在同一个进程里，
 * Application.onCreate 先于任何 receiver 运行，Store 此时已经初始化。
 */
class LogMoodAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val mood = parameters[MoodValueKey]?.coerceIn(1, 5) ?: return
        withContext(Dispatchers.Main) { Store.setMood(mood) }
        MoodWidget().updateAll(context)
    }
}

@Composable
private fun MoodContent(context: Context) {
    val today = DayKey.today.rawValue
    val mood = Store.moods.items.lastOrNull { it.dayKey == today }?.mood
    val chosen = widgetMoods.firstOrNull { it.value == mood }
    val widgetSize = LocalSize.current
    val wide = widgetSize.width >= 240.dp
    // 一行五个（带名称）或 3 + 2 两行，按钮按宽高缩放：边距 14×2、按钮间距 6、标题一行
    val wideFace = minOf((widgetSize.width - 28.dp) / 5, widgetSize.height - 66.dp).coerceIn(30.dp, 46.dp)
    val smallFace = minOf((widgetSize.width - 40.dp) / 3, (widgetSize.height - 56.dp) / 2).coerceIn(24.dp, 36.dp)
    WidgetFrame(context, "xingxia://mood") {
        Column(GlanceModifier.fillMaxSize()) {
            Text(
                chosen?.let { "今天${it.name}" } ?: "今天心情如何？",
                style = TextStyle(color = dim, fontSize = 12.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
            Spacer(GlanceModifier.defaultWeight())
            if (wide) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    for (face in widgetMoods) {
                        Box(GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) {
                            MoodButton(face, mood, wideFace, showsName = true)
                        }
                    }
                }
            } else {
                // 小尺寸分两行，按钮大一些好点
                Column(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        widgetMoods.take(3).forEachIndexed { index, face ->
                            if (index > 0) Spacer(GlanceModifier.width(6.dp))
                            MoodButton(face, mood, smallFace, showsName = false)
                        }
                    }
                    Spacer(GlanceModifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        widgetMoods.drop(3).forEachIndexed { index, face ->
                            if (index > 0) Spacer(GlanceModifier.width(6.dp))
                            MoodButton(face, mood, smallFace, showsName = false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodButton(face: WidgetMood, selected: Int?, size: Dp, showsName: Boolean) {
    val isSelected = selected == face.value
    Column(
        GlanceModifier
            .clickable(actionRunCallback<LogMoodAction>(actionParametersOf(MoodValueKey to face.value)))
            .semantics { contentDescription = if (isSelected) "${face.name}，已选" else "记录心情：${face.name}" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            GlanceModifier
                .size(size)
                .background(ColorProvider(face.color.copy(alpha = if (isSelected) 0.35f else 0f)))
                .cornerRadius(size / 2),
            contentAlignment = Alignment.Center,
        ) {
            Text(face.emoji, style = TextStyle(fontSize = (size.value * 0.55f).sp))
        }
        if (showsName) {
            Text(face.name, style = TextStyle(color = if (isSelected) white else dim, fontSize = 11.sp), maxLines = 1)
        }
    }
}

// endregion

// region 抛硬币（可点）

/** 小组件里最近一次抛硬币：哪天、当天第几次、正反（按天计次，零点回到「点一下」） */
private val coinDay by lazy { Prefs.int("widget.coin.day", 0) }
private val coinCount by lazy { Prefs.int("widget.coin.count", 0) }
private val coinHeads by lazy { Prefs.bool("widget.coin.heads", true) }

class CoinWidget : GlanceAppWidget() {
    // 按实际尺寸排版：小组件矮时硬币跟着缩小，不被裁掉
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { CoinContent(context) }
    }
}

class CoinWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CoinWidget()
}

/** 抛一次：结果记进小组件和决策历史（tool "coin"，标题为空，和 App 里不带问题抛硬币时一样） */
class FlipCoinAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withContext(Dispatchers.Main) {
            val heads = TrueRandom.bool()
            val today = DayKey.today.rawValue
            coinCount.value = if (coinDay.value == today) coinCount.value + 1 else 1
            coinDay.value = today
            coinHeads.value = heads
            // 小组件里抛的不带问一问里的问题，也不在 App 里弹「就这么办」浮层
            val question = Store.currentQuestion
            val prompt = Store.lastDecision
            Store.currentQuestion = null
            Store.log(ToolKind.COIN.id, "", if (heads) "正面" else "反面")
            Store.currentQuestion = question
            Store.lastDecision = prompt
        }
        CoinWidget().updateAll(context)
    }
}

@Composable
private fun CoinContent(context: Context) {
    val flipped = coinDay.value == DayKey.today.rawValue && coinCount.value > 0
    val heads = coinHeads.value
    val ink = ColorProvider(hex(0x7A5A0E))
    // 边距 14×2，上面一行标题、下面一行结果
    val widgetSize = LocalSize.current
    val coinSize = minOf(widgetSize.width - 28.dp, widgetSize.height - 70.dp).coerceIn(40.dp, 68.dp)
    WidgetFrame(context, "xingxia://tool/coin") {
        Column(GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("抛硬币", style = TextStyle(color = dim, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                Spacer(GlanceModifier.defaultWeight())
                if (flipped) Text("第 ${coinCount.value} 次", style = TextStyle(color = dim, fontSize = 11.sp))
            }
            Spacer(GlanceModifier.defaultWeight())
            // 和 App 里的硬币一样：正面太阳，反面月亮；还没抛时显示问号
            Box(
                GlanceModifier
                    .size(coinSize)
                    .background(ColorProvider(hex(0xF2C94C)))
                    .cornerRadius(coinSize / 2)
                    .clickable(actionRunCallback<FlipCoinAction>())
                    .semantics { contentDescription = if (!flipped) "抛硬币" else if (heads) "抛硬币，这次是正面" else "抛硬币，这次是反面" },
                contentAlignment = Alignment.Center,
            ) {
                if (flipped) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (heads) "🌞" else "🌙", style = TextStyle(fontSize = 18.sp))
                        Text(if (heads) "正" else "反", style = TextStyle(color = ink, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                    }
                } else {
                    Text("?", style = TextStyle(color = ink, fontSize = 30.sp, fontWeight = FontWeight.Bold))
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            Text(
                if (!flipped) "点硬币抛一次" else if (heads) "正面" else "反面",
                style = if (flipped) TextStyle(color = white, fontSize = 16.sp, fontWeight = FontWeight.Bold) else TextStyle(color = dim, fontSize = 12.sp),
                maxLines = 1,
            )
        }
    }
}

// endregion
