package com.tianaofeng.xingxia.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tianaofeng.xingxia.data.AppSettings
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.hex
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** 触感反馈。用当前窗口的 View 触发，遵循系统的触感设置。 */
object Haptics {
    var view: View? = null

    private fun perform(constant: Int) {
        if (!AppSettings.hapticsEnabled) return
        view?.performHapticFeedback(constant)
    }

    fun selection() = perform(HapticFeedbackConstants.CLOCK_TICK)
    fun tap() = perform(HapticFeedbackConstants.KEYBOARD_TAP)
    fun impact() = perform(HapticFeedbackConstants.CONTEXT_CLICK)
    fun heavy() = perform(HapticFeedbackConstants.LONG_PRESS)

    fun success() = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY,
    )

    fun error() = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS,
    )
}

/** 把当前 View 交给 Haptics 使用 */
@Composable
fun HapticsHost() {
    val view = LocalView.current
    DisposableEffect(view) {
        Haptics.view = view
        onDispose { if (Haptics.view === view) Haptics.view = null }
    }
}

/**
 * 摇一摇：加速度超过阈值时回调（受「摇一摇」设置控制）。
 * 只在页面处于前台（RESUMED）时监听：切到别的页面、退到后台、锁屏都会停止。
 */
@Composable
fun OnShake(enabled: Boolean = true, onShake: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val callback by rememberUpdatedState(onShake)
    val active = enabled && AppSettings.shakeEnabled
    DisposableEffect(active, lifecycleOwner) {
        if (!active) return@DisposableEffect onDispose { }
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var last = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = Triple(event.values[0], event.values[1], event.values[2])
                val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
                val now = System.currentTimeMillis()
                if (g > 2.2f && now - last > 700) {
                    last = now
                    callback()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        var registered = false
        fun register() {
            if (!registered && sensor != null) {
                registered = manager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME) == true
            }
        }
        fun unregister() {
            if (registered) {
                manager?.unregisterListener(listener)
                registered = false
            }
        }
        // 添加观察者时会补发到当前状态为止的事件，已经 RESUMED 的页面会立刻注册
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> register()
                Lifecycle.Event.ON_PAUSE -> unregister()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            unregister()
        }
    }
}

/** 庆祝用的彩带/星屑粒子。每次 trigger 变化时从上方中央炸开一次。 */
@Composable
fun Confetti(trigger: Int, modifier: Modifier = Modifier, count: Int = 90) {
    val palette = LocalPalette.current
    class Particle(val angle: Double, val speed: Double, val spin: Double, val w: Float, val h: Float, val color: Int, val shape: Int, val wobble: Double)

    var particles by remember { mutableStateOf<List<Particle>>(emptyList()) }
    var t by remember { mutableStateOf(0.0) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        // 系统关闭了动画时不放彩带（与 iOS 的「减弱动态效果」一致）
        if (isReduceMotion()) {
            particles = emptyList()
            return@LaunchedEffect
        }
        particles = List(count) {
            Particle(
                angle = Random.nextDouble(-PI * 0.95, -PI * 0.05),
                speed = Random.nextDouble(380.0, 900.0),
                spin = Random.nextDouble(-8.0, 8.0),
                w = Random.nextDouble(5.0, 10.0).toFloat(),
                h = Random.nextDouble(8.0, 14.0).toFloat(),
                color = Random.nextInt(20),
                shape = Random.nextInt(3),
                wobble = Random.nextDouble(0.0, 6.0),
            )
        }
        val start = withFrameNanos { it }
        while (isActive) {
            val now = withFrameNanos { it }
            t = (now - start) / 1e9
            if (t > 3.2) break
        }
        particles = emptyList()
    }
    if (particles.isEmpty()) return
    val colors = palette.spectrum + listOf(Color.White, hex(0xFFD166))
    Canvas(modifier.fillMaxSize()) {
        val scale = density
        val center = Offset(size.width * 0.5f, size.height * 0.35f)
        val alpha = max(0.0, 1 - t / 3.2).toFloat()
        for (p in particles) {
            val vx = cos(p.angle) * p.speed * scale
            val vy = sin(p.angle) * p.speed * scale
            val drag = 1 - exp(-t * 1.6)
            val x = center.x + vx / 1.6 * drag + sin(t * 6 + p.wobble) * 10 * t * scale
            val y = center.y + vy / 1.6 * drag + 260 * scale * t * t * 0.5
            val color = colors[p.color % colors.size].copy(alpha = alpha)
            val w = p.w * density
            val h = p.h * density
            translate(x.toFloat(), y.toFloat()) {
                rotate(Math.toDegrees(p.spin * t).toFloat(), Offset.Zero) {
                    when (p.shape) {
                        0 -> drawRect(color, Offset(-w / 2, -h / 2), Size(w, h))
                        1 -> drawOval(color, Offset(-w / 2, -h / 2), Size(w, h))
                        else -> drawPath(starPath(w * 0.7f), color)
                    }
                }
            }
        }
    }
}

/** 以原点为中心的五角星 */
fun starPath(radius: Float, points: Int = 5, innerRatio: Float = 0.45f): Path = Path().apply {
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) radius else radius * innerRatio
        val a = -PI / 2 + i * PI / points
        val x = (cos(a) * r).toFloat()
        val y = (sin(a) * r).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
