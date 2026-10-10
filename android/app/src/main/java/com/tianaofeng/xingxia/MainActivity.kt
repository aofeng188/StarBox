package com.tianaofeng.xingxia

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
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
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.ui.components.CosmicBackground
import com.tianaofeng.xingxia.ui.components.HapticsHost
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
    }
}

class MainActivity : ComponentActivity() {
    private val pendingLink = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) pendingLink.value = Routes.fromUri(intent?.data)
        setContent { XingXiaRoot(pendingLink) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Routes.fromUri(intent.data)?.let { pendingLink.value = it }
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

private fun isPowerSave(context: Context): Boolean =
    (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode == true

@Composable
fun XingXiaRoot(pendingLink: MutableState<String?>) {
    val context = LocalContext.current
    val now = rememberMinuteClock()
    val systemDark = isSystemInDarkTheme()
    val (theme, _) = ThemeManager.resolve(now, Store.birth, isPowerSave(context))
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
