package com.tianaofeng.xingxia

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.tianaofeng.xingxia.data.AppSettings
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.ui.components.CosmicBackground
import com.tianaofeng.xingxia.ui.components.HapticsHost
import com.tianaofeng.xingxia.ui.components.SensitiveClipboard
import com.tianaofeng.xingxia.ui.components.SoundEngine
import com.tianaofeng.xingxia.ui.components.rememberPowerSaveMode
import com.tianaofeng.xingxia.system.ReminderScheduler
import com.tianaofeng.xingxia.system.SkyReminders
import com.tianaofeng.xingxia.system.Widgets
import com.tianaofeng.xingxia.ui.nav.AppNavigation
import com.tianaofeng.xingxia.ui.nav.Routes
import com.tianaofeng.xingxia.ui.theme.AppearanceMode
import com.tianaofeng.xingxia.ui.theme.ThemeManager
import com.tianaofeng.xingxia.ui.theme.XingXiaTheme
import kotlinx.coroutines.delay
import java.time.LocalDateTime

class XingXiaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        AppSettings.migrateIfNeeded()
        SoundEngine.prepare(this)
        val app = this
        Store.onDataChanged = {
            Widgets.updateAll(app)
            ReminderScheduler.reschedule(app)
        }
        ReminderScheduler.reschedule(this)
        SkyReminders.reschedule(this)
        ReminderScheduler.scheduleDayChange(this)
    }
}

class MainActivity : ComponentActivity() {
    private val pendingLink = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // 从小组件、通知、快捷方式点进来的那次不弹每日仪式（Store.markDeepLink）
        if (savedInstanceState == null) pendingLink.value = Routes.fromUri(intent?.data)?.also { Store.markDeepLink() }
        setContent { XingXiaRoot(pendingLink) }
    }

    override fun onStop() {
        super.onStop()
        // 退到后台时收起「就这么办 / 再想想」浮层（收起不算表态）
        Store.lastDecision = null
        // 离开 App 时刷新小组件（主题、档案、日期、心情都可能变了）
        Widgets.updateAll(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Routes.fromUri(intent.data)?.let {
            Store.markDeepLink()
            pendingLink.value = it
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // 拷贝的密码在后台到期时读不到剪贴板，回到前台再检查一次
        if (hasFocus) SensitiveClipboard.clearIfExpired(this, hasFocus = true)
    }
}

/** 当前时间，每分钟刷新一次（「按时段切换」等主题规则依赖时间） */
@Composable
fun rememberMinuteClock(): LocalDateTime {
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            delay(60_000L - System.currentTimeMillis() % 60_000L)
            value = LocalDateTime.now()
        }
    }
    return now
}

@Composable
fun XingXiaRoot(pendingLink: MutableState<String?>) {
    val context = LocalContext.current
    val now = rememberMinuteClock()
    val systemDark = isSystemInDarkTheme()
    val (theme, _) = ThemeManager.resolve(now, Store.birth, rememberPowerSaveMode())
    val dark = when (ThemeManager.appearance) {
        AppearanceMode.SYSTEM -> systemDark
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
    }
    val palette = theme.palette(dark)

    val activity = context as? ComponentActivity
    SideEffect {
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    XingXiaTheme(palette) {
        CompositionLocalProvider(LocalContentColor provides palette.text) {
            HapticsHost()
            Box(Modifier.fillMaxSize()) {
                CosmicBackground()
                AppNavigation(pendingLink)
            }
        }
    }
}
