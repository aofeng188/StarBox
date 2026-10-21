package com.tianaofeng.xingxia.system

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.tianaofeng.xingxia.core.ZodiacSign
import com.tianaofeng.xingxia.data.CountdownItem
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.ReadableWidth
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.glass
import com.tianaofeng.xingxia.ui.components.pressable
import com.tianaofeng.xingxia.ui.theme.AppearanceMode
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.ThemeManager
import com.tianaofeng.xingxia.ui.theme.XingXiaTheme
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * 每个小组件实例的设置，按 appWidgetId 存在单独的 SharedPreferences 里。
 * 读的时候顺带订阅 [version]，小组件正在显示时改了设置也会跟着重组。
 * 缓存本身是普通 Map：首次加载可能发生在组合过程中，不能往快照状态里写。
 */
object WidgetConfig {
    private const val HOROSCOPE = "horoscope."
    private const val COUNTDOWN = "countdown."

    private val cache = ConcurrentHashMap<String, String>()
    private val version = mutableIntStateOf(0)
    @Volatile private var loaded = false

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences("widgets", Context.MODE_PRIVATE)

    @Synchronized
    private fun ensureLoaded(context: Context) {
        if (loaded) return
        for ((key, value) in prefs(context).all) if (value is String) cache[key] = value
        loaded = true
    }

    private fun read(context: Context, key: String): String? {
        ensureLoaded(context)
        // 只为订阅：设置变了 version 会加一，正在显示的小组件随之重组
        if (version.intValue < 0) return null
        return cache[key]
    }

    /** 只在点击、广播等组合之外的地方调用 */
    private fun write(context: Context, key: String, value: String?) {
        ensureLoaded(context)
        if (value == null) cache.remove(key) else cache[key] = value
        prefs(context).edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
        version.intValue++
    }

    /** 小组件里选定的星座；null 表示跟随当前档案 */
    fun horoscopeSign(context: Context, appWidgetId: Int): ZodiacSign? =
        read(context, HOROSCOPE + appWidgetId)?.let { name -> ZodiacSign.entries.firstOrNull { it.name == name } }

    fun setHoroscopeSign(context: Context, appWidgetId: Int, sign: ZodiacSign?) = write(context, HOROSCOPE + appWidgetId, sign?.name)

    /** 小组件里选定的倒数日 id；null 表示自动（置顶或最近的） */
    fun countdownId(context: Context, appWidgetId: Int): String? = read(context, COUNTDOWN + appWidgetId)

    fun setCountdownId(context: Context, appWidgetId: Int, id: String?) = write(context, COUNTDOWN + appWidgetId, id)

    /** 小组件被移除时清掉它的设置 */
    fun remove(context: Context, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            write(context, HOROSCOPE + id, null)
            write(context, COUNTDOWN + id, null)
        }
    }
}

/**
 * 小组件配置页的公共部分。添加小组件时由桌面打开，Android 9+ 长按小组件也能重新设置。
 * 不选直接返回也照常添加，使用默认设置。
 */
abstract class WidgetConfigActivity : ComponentActivity() {
    protected var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        appWidgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        setContent { Content() }
    }

    @Composable
    protected abstract fun Content()

    /** 选好后关闭；刷新在 onDestroy 里统一做 */
    protected fun done() = finish()

    override fun onDestroy() {
        // 不选直接返回也要刷新：带配置页的小组件添加时系统不会主动更新，否则会一直停在「加载中」
        if (isFinishing && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) Widgets.updateAll(this)
        super.onDestroy()
    }
}

class HoroscopeWidgetConfigActivity : WidgetConfigActivity() {
    @Composable
    override fun Content() {
        val current = WidgetConfig.horoscopeSign(this, appWidgetId)
        val own = Store.birth?.let { ZodiacSign.sunSign(it) }
        WidgetConfigScreen("星座运势小组件", "选择要显示的星座，之后长按小组件可以重新设置") {
            item {
                ConfigOption(
                    "跟随当前档案",
                    own?.let { "现在是${it.label}，切换档案后跟着变" } ?: "还没有档案时显示最近查看的星座",
                    current == null,
                ) {
                    WidgetConfig.setHoroscopeSign(this@HoroscopeWidgetConfigActivity, appWidgetId, null)
                    done()
                }
            }
            items(ZodiacSign.entries) { sign ->
                ConfigOption("${sign.glyph} ${sign.label}", sign.dateRange, current == sign) {
                    WidgetConfig.setHoroscopeSign(this@HoroscopeWidgetConfigActivity, appWidgetId, sign)
                    done()
                }
            }
        }
    }
}

class CountdownWidgetConfigActivity : WidgetConfigActivity() {
    @Composable
    override fun Content() {
        val current = WidgetConfig.countdownId(this, appWidgetId)
        // 含档案生日（开关打开时），和倒数日列表的顺序一致；档案生日的 id 是档案 id
        val countdowns = CountdownItem.sorted(CountdownItem.current())
        WidgetConfigScreen("倒数日小组件", "选择要显示的日子，之后长按小组件可以重新设置") {
            item {
                ConfigOption("自动", "优先显示置顶的，否则显示最近要到的", current == null || countdowns.none { it.id == current }) {
                    WidgetConfig.setCountdownId(this@CountdownWidgetConfigActivity, appWidgetId, null)
                    done()
                }
            }
            items(countdowns, key = { it.id }) { countdown ->
                val status = countdown.status()
                val detail = if (status.isToday) "${status.prefix}${status.suffix}" else "${status.prefix} ${status.days} ${status.suffix}"
                val source = if (countdown.isFromProfile) "来自档案" else countdown.kind.label
                ConfigOption("${countdown.emoji} ${countdown.title}", "$source · $detail", current == countdown.id) {
                    WidgetConfig.setCountdownId(this@CountdownWidgetConfigActivity, appWidgetId, countdown.id)
                    done()
                }
            }
            if (countdowns.isEmpty()) {
                item { SecondaryText("还没有倒数日。先在星匣的「记录 ▸ 倒数日」里添加，小组件会自动显示。", style = Fonts.caption) }
            }
        }
    }
}

@Composable
private fun WidgetConfigScreen(title: String, subtitle: String, content: LazyListScope.() -> Unit) {
    val dark = when (ThemeManager.appearance) {
        AppearanceMode.SYSTEM -> isSystemInDarkTheme()
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
    }
    val palette = ThemeManager.resolve(LocalDateTime.now(), Store.birth, false).first.palette(dark)
    // App 里单独选了深浅色时，状态栏图标跟着页面而不是系统设置走，否则会白字压白底
    val activity = LocalContext.current as? ComponentActivity
    SideEffect {
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    XingXiaTheme(palette) {
        CompositionLocalProvider(LocalContentColor provides palette.text) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(palette.backgroundTop, palette.backgroundBottom))),
                contentAlignment = Alignment.TopCenter,
            ) {
                LazyColumn(
                    Modifier
                        .widthIn(max = ReadableWidth)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .systemBarsPadding(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Column(Modifier.padding(bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(title, style = Fonts.title2, color = palette.text)
                            SecondaryText(subtitle, style = Fonts.footnote)
                        }
                    }
                    content()
                }
            }
        }
    }
}

@Composable
private fun ConfigOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
            .glass(RoundedCornerShape(18.dp), tint = if (selected) palette.accent.copy(alpha = 0.14f) else null)
            .semantics { this.selected = selected }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = Fonts.body, color = palette.text, maxLines = 1)
            SecondaryText(subtitle, style = Fonts.caption, maxLines = 1)
        }
        if (selected) Icon(Icons.Rounded.CheckCircle, "已选", tint = palette.accent)
    }
}
