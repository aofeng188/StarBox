package com.tianaofeng.xingxia.ui.components

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.tianaofeng.xingxia.core.DayKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.time.Duration
import java.time.LocalDateTime

// region 分享与剪贴板

/** 调起系统分享面板分享一段文字 */
fun shareText(context: Context, text: String, subject: String? = null) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        if (subject != null) {
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TITLE, subject)
        }
    }
    val chooser = Intent.createChooser(send, subject ?: "分享")
    if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(chooser) }
}

/**
 * 调起系统分享面板分享一张图片（分享卡等）。
 * 图片以 PNG 写到缓存目录的 share/ 下，经 FileProvider（authority 为「包名.fileprovider」）临时授权给接收方读取。
 * 顺带清掉 10 分钟前留下的分享图片，缓存不会越积越多。
 */
fun shareImage(context: Context, bitmap: Bitmap, fileName: String) {
    val dir = File(context.cacheDir, "share")
    if (!dir.exists() && !dir.mkdirs()) return
    val expired = System.currentTimeMillis() - 10 * 60 * 1000L
    dir.listFiles()?.forEach { if (it.lastModified() < expired) it.delete() }
    // 文件名里不能有路径分隔符等字符；没有扩展名时补上 .png
    var name = fileName.replace(Regex("[\\\\/:*?\"<>|\\s]"), "_").ifBlank { "xingxia" }
    if (!name.endsWith(".png", ignoreCase = true)) name += ".png"
    val file = File(dir, name)
    val written = runCatching {
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }.getOrDefault(false)
    if (!written) return
    val uri = runCatching { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file) }.getOrNull() ?: return
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        // 带上 ClipData，分享面板的预览和接收方才拿得到读权限
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, "分享图片")
    if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(chooser) }
}

/**
 * 拷贝文字到剪贴板。sensitive 为 true 时（密码等）标记为敏感内容，
 * 并在 60 秒后清除——只在剪贴板里仍是这一条时才清，不会误删之后拷贝的内容。
 */
fun copyText(context: Context, text: String, sensitive: Boolean = false) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    val clip = ClipData.newPlainText("星匣", text)
    if (sensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // 标记为敏感内容，系统不会在剪贴板预览里显示
        clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
    }
    clipboard.setPrimaryClip(clip)
    if (sensitive) SensitiveClipboard.schedule(context, clipboard)
    Haptics.success()
}

/**
 * 敏感内容的过期清除（对应 iOS 的 expirationDate）。
 * Android 10 起 App 在后台读不到剪贴板，无法确认内容是否还是那一条，
 * 这时先不清，等 App 重新获得焦点时（[MainActivity.onWindowFocusChanged]）再检查。
 */
object SensitiveClipboard {
    private const val LIFETIME_MS = 60_000L
    private val handler = Handler(Looper.getMainLooper())

    /** 拷贝时剪贴板记录的时间戳，用来判断剪贴板里是否仍是这一条 */
    private var timestamp = 0L
    private var expiresAt = 0L
    private var pending = false

    fun schedule(context: Context, clipboard: ClipboardManager) {
        // 刚拷贝完 App 在前台，能读到系统给这条记录打的时间戳
        timestamp = runCatching { clipboard.primaryClipDescription?.timestamp }.getOrNull() ?: 0L
        expiresAt = System.currentTimeMillis() + LIFETIME_MS
        pending = true
        val app = context.applicationContext
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ clearIfExpired(app, hasFocus = false) }, LIFETIME_MS)
    }

    /** 到期后剪贴板里仍是这一条就清除。hasFocus 表示调用时 App 窗口有焦点（读得到剪贴板） */
    fun clearIfExpired(context: Context, hasFocus: Boolean) {
        if (!pending || System.currentTimeMillis() < expiresAt) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val description = runCatching { clipboard.primaryClipDescription }.getOrNull()
        if (description == null) {
            // 有焦点仍读不到：剪贴板已经空了；没焦点：读不到，等回到前台再说
            if (hasFocus) pending = false
            return
        }
        pending = false
        if (timestamp == 0L || description.timestamp != timestamp) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip()
        } else {
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }
}

// endregion

// region 系统状态

/** 系统省电模式是否开启（随系统设置实时变化） */
@Composable
fun rememberPowerSaveMode(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    var saving by remember(manager) { mutableStateOf(manager?.isPowerSaveMode == true) }
    DisposableEffect(context, manager) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                saving = manager?.isPowerSaveMode == true
            }
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        // 注册期间可能错过了变化，补读一次
        saving = manager?.isPowerSaveMode == true
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return saving
}

/** 当前日期：过零点、App 回到前台时自动刷新 */
@Composable
fun rememberToday(): DayKey {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var today by remember { mutableStateOf(DayKey.today) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                today = DayKey.today
                val now = LocalDateTime.now()
                val midnight = now.toLocalDate().plusDays(1).atStartOfDay()
                delay(Duration.between(now, midnight).toMillis() + 1_000L)
            }
        }
    }
    return today
}

/**
 * 系统是否关闭了动画（无障碍里的「移除动画」或动画时长缩放为 0），对应 iOS 的「减弱动态效果」。
 * 读的是 Compose 放进协程上下文的动画时长缩放，系统设置改了会跟着变；在 LaunchedEffect、produceState 里调用。
 */
fun CoroutineScope.isReduceMotion(): Boolean = (coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f) == 0f

/** 让 DayKey 能放进 rememberSaveable */
val DayKeySaver: Saver<DayKey, Int> = Saver(save = { it.rawValue }, restore = { DayKey.fromRaw(it) })

// endregion
