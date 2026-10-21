package com.tianaofeng.xingxia.ui.screens.tools

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tianaofeng.xingxia.core.Astronomy
import com.tianaofeng.xingxia.core.ContentLibrary
import com.tianaofeng.xingxia.core.DateText
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.core.Dish
import com.tianaofeng.xingxia.core.TrueRandom
import com.tianaofeng.xingxia.data.BlockedFood
import com.tianaofeng.xingxia.data.FoodLog
import com.tianaofeng.xingxia.data.FoodMenu
import com.tianaofeng.xingxia.data.FoodMenuItem
import com.tianaofeng.xingxia.ui.components.SoundEffect
import com.tianaofeng.xingxia.ui.components.SoundEngine
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import com.tianaofeng.xingxia.data.Prefs
import com.tianaofeng.xingxia.data.Roster
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.data.WheelOption
import com.tianaofeng.xingxia.data.WheelTemplate
import com.tianaofeng.xingxia.ui.components.BigActionButton
import com.tianaofeng.xingxia.ui.components.ChipRow
import com.tianaofeng.xingxia.ui.components.CircleIconButton
import com.tianaofeng.xingxia.ui.components.Confetti
import com.tianaofeng.xingxia.ui.components.EmptyHint
import com.tianaofeng.xingxia.ui.components.Flow
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GlassButton
import com.tianaofeng.xingxia.ui.components.GlassCard
import com.tianaofeng.xingxia.ui.components.GradientText
import com.tianaofeng.xingxia.ui.components.Haptics
import com.tianaofeng.xingxia.ui.components.MultiChipPicker
import com.tianaofeng.xingxia.ui.components.OnShake
import com.tianaofeng.xingxia.ui.components.Screen
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.SectionHeader
import com.tianaofeng.xingxia.ui.components.SegmentedPicker
import com.tianaofeng.xingxia.ui.components.SelectChip
import com.tianaofeng.xingxia.ui.components.StepperRow
import com.tianaofeng.xingxia.ui.components.TextAction
import com.tianaofeng.xingxia.ui.components.ToggleRow
import com.tianaofeng.xingxia.ui.components.fadingHorizontalScroll
import com.tianaofeng.xingxia.ui.components.pressable
import com.tianaofeng.xingxia.ui.components.copyText
import com.tianaofeng.xingxia.ui.components.shareText
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.screens.ask.QuestionBanner
import com.tianaofeng.xingxia.ui.screens.divination.GlassTextField
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.hex
import com.tianaofeng.xingxia.ui.theme.readableForeground
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** 「老虎机」式快速滚动后减速停下 */
private suspend fun <T> roll(pool: List<T>, final: T, steps: Int = 20, update: (T) -> Unit) {
    var delayMs = 30.0
    repeat(steps) {
        update(TrueRandom.pick(pool))
        SoundEngine.play(SoundEffect.TICK, 0.45f)
        Haptics.selection()
        delay(delayMs.toLong())
        delayMs *= 1.13
    }
    update(final)
}

@Composable
private fun RollingResult(emoji: String, title: String, subtitle: String, rolling: Boolean, revealCount: Int) {
    val pop = remember { Animatable(1f) }
    LaunchedEffect(revealCount) {
        if (revealCount == 0) return@LaunchedEffect
        pop.animateTo(1.18f, tween(150))
        pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
    }
    Column(
        Modifier.graphicsLayer {
            val s = pop.value * if (rolling) 0.96f else 1f
            scaleX = s
            scaleY = s
            alpha = if (rolling) 0.85f else 1f
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(emoji, style = TextStyle(fontSize = 76.sp))
        Text(title, style = Fonts.display(32), textAlign = TextAlign.Center, maxLines = 2)
        SecondaryText(subtitle, maxLines = 1)
    }
}

// region 今天吃什么

private enum class MealTime(val code: String, val label: String) {
    BREAKFAST("b", "早餐"), LUNCH("l", "午餐"), DINNER("d", "晚餐"), NIGHT("n", "夜宵"), ANY("*", "不限");

    companion object {
        val current: MealTime
            get() = when (LocalTime.now().hour) {
                in 5..9 -> BREAKFAST
                in 10..14 -> LUNCH
                in 15..20 -> DINNER
                else -> NIGHT
            }
    }
}

private val TASTES = listOf("辣", "清淡", "酸爽", "甜口", "咸鲜", "浓郁")

/** 智能去重：最近吃过的降低权重 */
private fun foodWeight(name: String, logs: List<FoodLog>, smart: Boolean): Double {
    if (!smart) return 1.0
    val last = logs.lastOrNull { it.name == name } ?: return 1.0
    return when (DayKey.of(Instant.ofEpochMilli(last.date)).daysTo(DayKey.today)) {
        in Int.MIN_VALUE..0 -> 0.05
        in 1..2 -> 0.2
        in 3..6 -> 0.5
        else -> 1.0
    }
}

/** 一个可被抽中的候选：内置菜品或自定义菜单里的一项 */
private data class FoodCandidate(val emoji: String, val title: String, val subtitle: String, val weight: Double, val source: String)

private enum class FoodSource(val label: String) { LIBRARY("美食库"), MENUS("我的菜单") }

@Composable
fun FoodScreen() {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    var smart by Prefs.bool("food.smartDedupe", true)
    var source by remember { mutableStateOf(FoodSource.LIBRARY) }
    var menuId by Prefs.string("food.menu", "")
    var meal by remember { mutableStateOf(MealTime.current) }
    var cuisines by remember { mutableStateOf(setOf<String>()) }
    var tastes by remember { mutableStateOf(setOf<String>()) }
    var maxPrice by remember { mutableIntStateOf(3) }
    var display by remember { mutableStateOf<FoodCandidate?>(null) }
    var result by remember { mutableStateOf<FoodCandidate?>(null) }
    var rolling by remember { mutableStateOf(false) }
    var revealCount by remember { mutableIntStateOf(0) }
    var confetti by remember { mutableIntStateOf(0) }
    // 当前结果是否已点过「就吃这个」，防止连点重复记录
    var accepted by remember { mutableStateOf(false) }
    var showBlocked by remember { mutableStateOf(false) }
    var editingMenuId by remember { mutableStateOf<String?>(null) }

    val allCuisines = remember { ContentLibrary.foods.map { it.cuisine }.distinct() }
    val menus = Store.foodMenus.items.sortedBy { it.createdAt }
    val menu = menus.firstOrNull { it.id == menuId } ?: menus.firstOrNull()
    val pool: List<FoodCandidate> = when (source) {
        FoodSource.LIBRARY -> {
            val blockedNames = Store.blockedFoods.items.map { it.name }.toSet()
            val logs = Store.foodLogs.items
            ContentLibrary.foods.filter { dish ->
                (meal == MealTime.ANY || meal.code in dish.meals) &&
                    (cuisines.isEmpty() || dish.cuisine in cuisines) &&
                    (tastes.isEmpty() || dish.tastes.any { it in tastes }) &&
                    dish.price <= maxPrice &&
                    dish.name !in blockedNames
            }.map { FoodCandidate(it.emoji, it.name, it.detail, foodWeight(it.name, logs, smart), "library") }
        }
        FoodSource.MENUS -> menu?.items.orEmpty().filter { it.weight > 0 }.map {
            FoodCandidate(menu?.icon ?: "🍱", it.name, it.note.ifEmpty { menu?.name ?: "" }, it.weight, "menu")
        }
    }

    fun pick() {
        if (pool.isEmpty() || rolling) return
        val index = TrueRandom.weightedIndex(pool.map { it.weight }) ?: 0
        val final = pool[index.coerceIn(0, pool.size - 1)]
        rolling = true
        result = null
        accepted = false
        SoundEngine.play(SoundEffect.WHOOSH, 0.6f)
        scope.launch {
            roll(pool, final) { display = it }
            rolling = false
            result = final
            display = null
            revealCount++
            SoundEngine.play(SoundEffect.SUCCESS)
            Haptics.success()
            Store.log(ToolKind.FOOD.id, if (source == FoodSource.LIBRARY) meal.label else menu?.name ?: source.label, final.title)
        }
    }

    OnShake(enabled = !showBlocked && editingMenuId == null) { pick() }

    if (showBlocked) {
        BlockedFoodsScreen { showBlocked = false }
        return
    }
    editingMenuId?.let { id ->
        val editing = Store.foodMenus.items.firstOrNull { it.id == id }
        if (editing != null) {
            FoodMenuEditor(editing) { editingMenuId = null }
            return
        }
    }

    Box(Modifier.fillMaxSize()) {
        // 顶栏右侧还有收藏星标，黑名单用图标按钮（与 iOS 工具栏一致），数量放进读屏描述
        Screen(title = "今天吃什么", actions = {
            CircleIconButton(Icons.Rounded.PanTool, "黑名单（${Store.blockedFoods.items.size}）") { showBlocked = true }
        }) {
            QuestionBanner()
            SegmentedPicker(FoodSource.entries, source, { it.label }) {
                source = it
                result = null
                display = null
            }
            when (source) {
                FoodSource.LIBRARY -> {
                    ChipRow { for (item in MealTime.entries) SelectChip(item.label, meal == item) { meal = item } }
                    MultiChipPicker(allCuisines, cuisines, "全部菜系") { cuisines = it }
                    MultiChipPicker(TASTES, tastes, "任意口味") { tastes = it }
                    SegmentedPicker(listOf(1, 2, 3), maxPrice, { listOf("¥ 实惠", "¥¥ 以内", "不限价位")[it - 1] }) { maxPrice = it }
                }
                FoodSource.MENUS -> ChipRow {
                    for (item in menus) SelectChip("${item.icon} ${item.name} · ${item.items.size}", item.id == menu?.id) {
                        menuId = item.id
                        result = null
                    }
                    if (menu != null) CircleIconButton(Icons.Rounded.Edit, "编辑菜单", size = 34.dp) { editingMenuId = menu.id }
                    CircleIconButton(Icons.Rounded.Add, "新建菜单", size = 34.dp) {
                        val created = FoodMenu(name = "新菜单 ${menus.size + 1}")
                        Store.foodMenus.add(created)
                        menuId = created.id
                        editingMenuId = created.id
                    }
                }
            }

            GlassCard(Modifier.heightIn(min = 260.dp), padding = 24.dp, horizontalAlignment = Alignment.CenterHorizontally, spacing = 18.dp) {
                val shown = display ?: result
                if (shown != null) {
                    RollingResult(shown.emoji, shown.title, shown.subtitle, rolling, revealCount)
                } else {
                    Text("🍽️", style = TextStyle(fontSize = 70.sp))
                    Text("今天吃点什么好呢？", style = Fonts.title2)
                    SecondaryText(
                        when {
                            source == FoodSource.LIBRARY -> "从 ${pool.size} 道菜里帮你挑一道"
                            pool.isEmpty() -> "先在菜单里添加几家常去的店"
                            else -> "从「${menu?.name}」的 ${pool.size} 个选项里挑"
                        },
                    )
                }
                val r = result
                if (r != null && !rolling) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (accepted) {
                            GlassButton("已记录", Icons.Rounded.Check, enabled = false) {}
                        } else {
                            GlassButton("就吃这个", Icons.Rounded.CheckCircle) {
                                if (accepted) return@GlassButton
                                accepted = true
                                Store.foodLogs.update { (it + FoodLog(r.title, r.source)).takeLast(500) }
                                confetti++
                                SoundEngine.play(SoundEffect.SPARKLE)
                                Haptics.success()
                            }
                        }
                        if (r.source == "library") {
                            GlassButton("不想吃", Icons.Rounded.ThumbDown) {
                                Store.blockedFoods.add(BlockedFood(r.title))
                                Haptics.error()
                                result = null
                            }
                        }
                    }
                }
            }

            BigActionButton(if (result == null) "帮我选！" else "换一个", Icons.Rounded.Casino, enabled = !rolling && pool.isNotEmpty()) { pick() }
            SecondaryText("摇一摇手机也可以", style = Fonts.caption, modifier = Modifier.align(Alignment.CenterHorizontally))

            if (source == FoodSource.LIBRARY) {
                GlassCard(padding = 16.dp) {
                    ToggleRow("智能去重", smart, "最近几天吃过的会降低被抽中的概率") { smart = it }
                }
            }

            val logs = Store.foodLogs.items
            if (logs.isNotEmpty()) {
                GlassCard(spacing = 10.dp) {
                    val today = DayKey.today
                    val thisMonth = logs.filter {
                        val d = DayKey.of(Instant.ofEpochMilli(it.date))
                        d.year == today.year && d.month == today.month
                    }
                    SectionHeader("最近吃过", subtitle = "本月 ${thisMonth.size} 餐", icon = Icons.Rounded.History)
                    for (log in logs.takeLast(6).reversed()) {
                        Row {
                            Text(log.name, style = Fonts.subheadline, modifier = Modifier.weight(1f))
                            SecondaryText(DateText.relative(Instant.ofEpochMilli(log.date)), style = Fonts.caption)
                        }
                    }
                    val top = thisMonth.groupingBy { it.name }.eachCount().maxByOrNull { it.value }
                    if (top != null && top.value > 1) {
                        Text("这个月吃得最多的是「${top.key}」，一共 ${top.value} 次", style = Fonts.caption, color = palette.accent)
                    }
                }
            }
        }
        Confetti(confetti)
    }
}

@Composable
private fun FoodMenuEditor(menu: FoodMenu, onDone: () -> Unit) {
    val palette = LocalPalette.current
    var newItem by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    val icons = listOf("🍱", "🍜", "🍔", "🍕", "🍣", "🥗", "🍲", "🥟", "🍛", "🧋", "🏢", "🏠", "🎉", "💼")

    fun save(transform: (FoodMenu) -> FoodMenu) {
        Store.foodMenus.update { list -> list.map { if (it.id == menu.id) transform(it) else it } }
    }

    Screen(title = "编辑菜单", onBack = onDone, actions = { TextAction("完成", onClick = onDone) }) {
        GlassCard {
            GlassTextField(menu.name, "菜单名称") { name -> save { it.copy(name = name) } }
            Row(Modifier.fadingHorizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (icon in icons) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (menu.icon == icon) palette.accent.copy(alpha = 0.3f) else Color.Transparent)
                            .clickable { save { it.copy(icon = icon) } },
                        contentAlignment = Alignment.Center,
                    ) { Text(icon, style = Fonts.title2) }
                }
            }
        }
        GlassCard(spacing = 10.dp) {
            SectionHeader("选项", subtitle = "权重越高越容易被抽中；0 表示暂时不参与")
            for (item in menu.items) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassTextField(item.name, "选项", modifier = Modifier.weight(1f)) { name ->
                        save { m -> m.copy(items = m.items.map { if (it.id == item.id) it.copy(name = name) else it }) }
                    }
                    Text("×${item.weight.toInt()}", style = Fonts.caption, color = palette.secondaryText)
                    CircleIconButton(Icons.Rounded.Add, "加权", size = 32.dp, enabled = item.weight < 10) {
                        save { m -> m.copy(items = m.items.map { if (it.id == item.id) it.copy(weight = it.weight + 1) else it }) }
                    }
                    CircleIconButton(Icons.Rounded.Delete, "删除", size = 32.dp) {
                        save { m -> m.copy(items = m.items.filter { it.id != item.id }) }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassTextField(newItem, "添加选项，如「楼下兰州拉面」", modifier = Modifier.weight(1f)) { newItem = it }
                CircleIconButton(Icons.Rounded.Add, "添加", enabled = newItem.isNotBlank()) {
                    val name = newItem.trim()
                    save { it.copy(items = it.items + FoodMenuItem(name = name)) }
                    newItem = ""
                }
            }
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (confirmDelete) {
                GlassButton("确定删除「${menu.name}」", Icons.Rounded.Delete) {
                    Store.foodMenus.update { list -> list.filter { it.id != menu.id } }
                    onDone()
                }
            } else {
                GlassButton("删除这个菜单", Icons.Rounded.Delete) { confirmDelete = true }
            }
        }
    }
}

@Composable
private fun BlockedFoodsScreen(onDone: () -> Unit) {
    Screen(title = "黑名单", onBack = onDone) {
        val blocked = Store.blockedFoods.items
        if (blocked.isEmpty()) {
            EmptyHint(Icons.Rounded.Block, "黑名单是空的", "抽到不想吃的菜时，点「不想吃」即可加入")
        } else {
            GlassCard(padding = 8.dp, spacing = 0.dp) {
                blocked.reversed().forEachIndexed { i, item ->
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.name, style = Fonts.body, modifier = Modifier.weight(1f))
                        CircleIconButton(Icons.Rounded.Delete, "移出黑名单", size = 34.dp) {
                            Store.blockedFoods.update { list -> list.filter { it.name != item.name } }
                        }
                    }
                    if (i < blocked.size - 1) HorizontalDivider(color = LocalPalette.current.subtle(0.08f))
                }
            }
            SecondaryText("黑名单里的菜不会被抽中。", style = Fonts.caption)
        }
    }
}

// endregion

// region 幸运大转盘

/** 先快后慢（ease-out），最后一段有「快停下又走了一格」的悬念 */
private val WheelEasing = Easing { 1f - (1f - it).pow(4.2f) }

private fun wheelRanges(options: List<WheelOption>): List<ClosedFloatingPointRange<Double>> {
    val total = max(0.0001, options.sumOf { it.weight })
    var start = 0.0
    return options.map {
        val sweep = it.weight / total * 360
        val range = start..(start + sweep)
        start += sweep
        range
    }
}

/** 当前指针（正上方）指向的扇区 */
private fun wheelIndex(rotation: Double, options: List<WheelOption>): Int {
    val pointer = Astronomy.normalize(-rotation)
    return wheelRanges(options).indexOfFirst { pointer in it }.coerceAtLeast(0)
}

private fun wheelColor(index: Int, count: Int, colors: List<Color>): Color {
    var i = index % colors.size
    // 首尾相邻时避免同色
    if (index == count - 1 && count > 1 && i == 0) i = (i + 1) % colors.size
    return colors[i]
}

@Composable
fun WheelScreen() {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    var lastTemplateId by Prefs.string("wheel.lastTemplate", "")
    val templates = Store.wheels.items.sortedBy { it.createdAt }
    val template = templates.firstOrNull { it.id == lastTemplateId } ?: templates.firstOrNull()
    val view = LocalView.current
    val eliminated = remember { mutableStateListOf<String>() }
    // 淘汰模式下刚转到、还在 1.2 秒展示期内的选项
    var pendingElimination by remember { mutableStateOf<String?>(null) }
    val segments = template?.options.orEmpty().filter { it.weight > 0 && it.id !in eliminated }
    // 去掉待淘汰项后还能转的个数；「恢复全部」只算当前转盘里已淘汰或待淘汰的（与 iOS 一致）
    val spinnableCount = segments.count { it.id != pendingElimination }
    val eliminatedCount = template?.options.orEmpty().count { it.id in eliminated || it.id == pendingElimination }
    val angle = remember { Animatable(0f) }
    var spinning by remember { mutableStateOf(false) }
    var resultTitle by remember { mutableStateOf<String?>(null) }
    var confetti by remember { mutableIntStateOf(0) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(segments) {
        var lastIndex = -1
        snapshotFlow { angle.value }.collect { value ->
            val index = wheelIndex(value.toDouble(), segments)
            if (index != lastIndex) {
                lastIndex = index
                if (spinning) {
                    SoundEngine.play(SoundEffect.TICK, 0.55f)
                    Haptics.selection()
                }
            }
        }
    }

    fun eliminate(id: String) {
        if (id !in eliminated) eliminated.add(id)
    }

    fun restoreAll() {
        eliminated.clear()
        pendingElimination = null
    }

    // 关掉淘汰模式后「恢复全部」不再显示，已移出的选项要放回盘面
    LaunchedEffect(template?.removeAfterSpin) {
        if (template?.removeAfterSpin != true) restoreAll()
    }
    // 换转盘（含在编辑器里删掉当前转盘）时，上一个转盘的结果和淘汰状态一并清掉
    LaunchedEffect(template?.id) {
        restoreAll()
        resultTitle = null
    }

    fun spin() {
        if (spinning) return
        val spun = template
        // 先确认去掉待淘汰项后还能转，再让它立刻出局（免得被重复抽中），落点按移出后的扇区计算
        val options = spun?.options.orEmpty().filter { it.weight > 0 && it.id !in eliminated && it.id != pendingElimination }
        if (options.size < 2) return
        pendingElimination?.let {
            eliminate(it)
            pendingElimination = null
        }
        val index = TrueRandom.weightedIndex(options.map { it.weight }) ?: return
        val range = wheelRanges(options)[index]
        val width = range.endInclusive - range.start
        val landing = range.start + width * 0.15 + TrueRandom.double() * width * 0.7
        val turns = floor(1300.0 / 400) + 3
        val currentMod = Astronomy.normalize(angle.value.toDouble())
        val desiredMod = Astronomy.normalize(360 - landing)
        val delta = Astronomy.normalize(desiredMod - currentMod) + turns * 360
        val target = (angle.value + delta).toFloat()
        val duration = min(7.0, max(3.2, delta / 1300 * 1.6))
        resultTitle = null
        spinning = true
        SoundEngine.play(SoundEffect.WHOOSH, 0.5f)
        Haptics.impact()
        scope.launch {
            angle.animateTo(target, tween((duration * 1000).toInt(), easing = WheelEasing))
            spinning = false
            val picked = options[index]
            resultTitle = picked.title
            SoundEngine.play(SoundEffect.SUCCESS)
            Haptics.success()
            confetti++
            view.announceForAccessibility("转到了：${picked.title}")
            Store.log(ToolKind.WHEEL.id, spun?.name ?: "", picked.title)
            if (spun?.removeAfterSpin == true) {
                pendingElimination = picked.id
                delay(1200)
                if (pendingElimination == picked.id) {
                    eliminate(picked.id)
                    pendingElimination = null
                }
            }
        }
    }

    fun create() {
        val new = WheelTemplate(name = "新转盘", options = listOf("选项一", "选项二", "选项三").map { WheelOption(title = it) })
        Store.wheels.add(new)
        lastTemplateId = new.id
        editingId = new.id
    }

    OnShake(enabled = editingId == null) { spin() }

    val editing = editingId?.let { id -> Store.wheels.items.firstOrNull { it.id == id } }
    if (editing != null) {
        WheelEditor(editing) { editingId = null }
        return
    }

    Box(Modifier.fillMaxSize()) {
        Screen(title = "幸运大转盘", horizontalAlignment = Alignment.CenterHorizontally, actions = {
            Box {
                CircleIconButton(Icons.Rounded.Edit, "编辑", enabled = !spinning) { menuOpen = true }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (template != null) {
                        DropdownMenuItem(text = { Text("编辑当前转盘") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = {
                            menuOpen = false
                            editingId = template.id
                        })
                    }
                    DropdownMenuItem(text = { Text("新建转盘") }, leadingIcon = { Icon(Icons.Rounded.Add, null) }, onClick = {
                        menuOpen = false
                        create()
                    })
                }
            }
        }) {
            QuestionBanner()
            if (template == null) {
                EmptyHint(Icons.Rounded.Casino, "还没有转盘", "点右上角新建一个吧")
                return@Screen
            }
            ChipRow {
                for (item in templates) {
                    SelectChip(item.name, item.id == template.id) {
                        // 转动中不能换转盘，否则结果和停下的位置对不上
                        if (spinning) return@SelectChip
                        lastTemplateId = item.id
                        restoreAll()
                        resultTitle = null
                    }
                }
            }
            Box(Modifier.size(340.dp), contentAlignment = Alignment.Center) {
                WheelCanvas(segments, angle.value, palette.spectrum, spinning)
                // 指针（金色物件，向主题强调色靠一点）
                val pointer = palette.identity(listOf(hex(0xFFF3C4), hex(0xF5B83D)))
                val pointerEdge = palette.identity(hex(0x8A6A2A))
                Canvas(
                    Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-6).dp)
                        .size(30.dp, 38.dp),
                ) {
                    val path = Path().apply {
                        moveTo(size.width / 2, size.height)
                        lineTo(0f, 0f)
                        lineTo(size.width, 0f)
                        close()
                    }
                    drawPath(path, Brush.verticalGradient(pointer))
                    drawPath(path, pointerEdge, style = Stroke(1.5.dp.toPx()))
                }
                // 中心按钮
                Box(
                    Modifier
                        .size(84.dp)
                        .pressable(!spinning && spinnableCount >= 2) { spin() }
                        .clip(CircleShape)
                        .background(palette.accentBrush),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("GO", style = Fonts.display(26, FontWeight.Black), color = palette.onAccent)
                }
            }
            Box(Modifier.heightIn(min = 70.dp), contentAlignment = Alignment.Center) {
                val r = resultTitle
                if (r != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SecondaryText("转到了")
                        GradientText(r, style = Fonts.display(34, FontWeight.Black), textAlign = TextAlign.Center)
                    }
                } else {
                    SecondaryText(if (spinning) "转呀转…" else "点中间的按钮，或摇一摇")
                }
            }
            if (template.removeAfterSpin) {
                GlassCard(padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SecondaryText("淘汰模式：转到的选项会暂时移出", style = Fonts.caption, modifier = Modifier.weight(1f))
                        if (eliminatedCount > 0) {
                            TextAction("恢复全部（$eliminatedCount）", enabled = !spinning) { restoreAll() }
                        }
                    }
                }
            }
            GlassCard(spacing = 8.dp) {
                SectionHeader("选项", subtitle = "${segments.size} 个 · 按权重分配面积", icon = Icons.Rounded.FormatListBulleted)
                val total = max(0.0001, segments.sumOf { it.weight })
                segments.forEachIndexed { i, option ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(wheelColor(i, segments.size, palette.spectrum)),
                        )
                        Text(option.title, style = Fonts.subheadline, modifier = Modifier.weight(1f))
                        SecondaryText("%.0f%%".format(option.weight / total * 100), style = Fonts.caption)
                    }
                }
            }
        }
        Confetti(confetti)
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun WheelCanvas(segments: List<WheelOption>, rotation: Float, colors: List<Color>, lightsOn: Boolean) {
    val measurer = rememberTextMeasurer()
    // 金边和跑马灯是「物件」，保留金色，但向主题强调色靠一点；深色底盘不动
    val palette = LocalPalette.current
    val rim = palette.identity(listOf(hex(0xF5D78E), hex(0xB8862E)))
    val bulbOn = palette.identity(hex(0xFFF3C4))
    val bulbOff = palette.identity(hex(0x8A6A2A))
    Canvas(Modifier.fillMaxSize()) {
        val radius = size.minDimension / 2
        val center = this.center
        // 外圈
        drawCircle(Brush.verticalGradient(listOf(hex(0x2A2340), hex(0x120E22))), radius)
        drawCircle(Brush.verticalGradient(rim), radius - 2.dp.toPx(), style = Stroke(4.dp.toPx()))
        val r = radius * 0.86f
        val total = max(0.0001, segments.sumOf { it.weight })
        rotate(rotation, center) {
            var start = -90f
            segments.forEachIndexed { index, segment ->
                val sweep = (segment.weight / total * 360).toFloat()
                val color = wheelColor(index, segments.size, colors)
                val mid = start + sweep / 2
                val endPoint = Offset(center.x + cos(Math.toRadians(mid.toDouble())).toFloat() * r, center.y + sin(Math.toRadians(mid.toDouble())).toFloat() * r)
                drawArc(
                    Brush.linearGradient(listOf(color, color.copy(alpha = 0.75f)), center, endPoint),
                    start, sweep, true, Offset(center.x - r, center.y - r), Size(r * 2, r * 2),
                )
                drawArc(Color.White.copy(alpha = 0.35f), start, sweep, true, Offset(center.x - r, center.y - r), Size(r * 2, r * 2), style = Stroke(1.dp.toPx()))

                // 文字沿半径方向；转到左半边时翻转，保证不倒着读
                val flip = cos(Math.toRadians((mid + rotation).toDouble())) < 0
                val band = r * 0.57f
                val fontSize = min(18f, max(9f, (band * sweep * Math.PI.toFloat() / 180 * 0.6f) / density))
                val title = if (segment.title.length > 10) segment.title.take(9) + "…" else segment.title
                // 扇区外缘半透明、压在深色底盘上还有暗角，换深字的亮度分界取 0.3（与 iOS 的 labelColor(on:) 一致）
                val label = if (color.luminance() > 0.3f) hex(0x14101F) else Color.White
                val layout = measurer.measure(AnnotatedString(title), TextStyle(fontSize = fontSize.sp, fontWeight = FontWeight.Bold, color = label))
                val textScale = min(1f, (r * 0.56f) / max(1f, layout.size.width.toFloat()))
                rotate(mid, center) {
                    translate(center.x + band, center.y) {
                        rotate(if (flip) 180f else 0f, Offset.Zero) {
                            scale(textScale, textScale, Offset.Zero) {
                                drawText(layout, topLeft = Offset(-layout.size.width / 2f, -layout.size.height / 2f))
                            }
                        }
                    }
                }
                start += sweep
            }
            drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.25f)), center, r), r)
        }
        // 跑马灯
        for (i in 0 until 24) {
            val a = i / 24.0 * 2 * Math.PI
            val on = if (lightsOn) (i + (rotation / 15).toInt()) % 2 == 0 else i % 2 == 0
            val p = Offset(center.x + (cos(a) * radius * 0.93).toFloat(), center.y + (sin(a) * radius * 0.93).toFloat())
            drawCircle(if (on) bulbOn else bulbOff, size.minDimension * 0.014f, p)
        }
    }
}

@Composable
private fun WheelEditor(template: WheelTemplate, onDone: () -> Unit) {
    val palette = LocalPalette.current
    var newOption by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    fun save(transform: (WheelTemplate) -> WheelTemplate) {
        Store.wheels.update { list -> list.map { if (it.id == template.id) transform(it) else it } }
    }

    Screen(title = "编辑转盘", onBack = onDone, actions = { TextAction("完成", onClick = onDone) }) {
        GlassCard {
            GlassTextField(template.name, "转盘名称") { name -> save { it.copy(name = name) } }
            ToggleRow("淘汰模式", template.removeAfterSpin, "转到的选项会暂时移出") { on -> save { it.copy(removeAfterSpin = on) } }
        }
        GlassCard(spacing = 10.dp) {
            SectionHeader("选项", subtitle = "权重决定扇区大小和被转到的概率")
            template.options.forEach { option ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassTextField(option.title, "选项", modifier = Modifier.weight(1f)) { title ->
                        save { t -> t.copy(options = t.options.map { if (it.id == option.id) it.copy(title = title) else it }) }
                    }
                    Text("×${option.weight.toInt()}", style = Fonts.caption, color = palette.secondaryText)
                    CircleIconButton(Icons.Rounded.Add, "加权", size = 32.dp, enabled = option.weight < 10) {
                        save { t -> t.copy(options = t.options.map { if (it.id == option.id) it.copy(weight = it.weight + 1) else it }) }
                    }
                    CircleIconButton(Icons.Rounded.Delete, "删除", size = 32.dp) {
                        save { t -> t.copy(options = t.options.filter { it.id != option.id }) }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassTextField(newOption, "添加选项", modifier = Modifier.weight(1f)) { newOption = it }
                CircleIconButton(Icons.Rounded.Add, "添加", enabled = newOption.isNotBlank()) {
                    val title = newOption.trim()
                    save { it.copy(options = it.options + WheelOption(title = title)) }
                    newOption = ""
                }
            }
            SecondaryText("点 + 增加权重；权重越大，扇区越大。", style = Fonts.caption)
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (confirmDelete) {
                GlassButton("确定删除「${template.name}」", Icons.Rounded.Delete) {
                    Store.wheels.update { list -> list.filter { it.id != template.id } }
                    onDone()
                }
            } else {
                GlassButton("删除这个转盘", Icons.Rounded.Delete) { confirmDelete = true }
            }
        }
    }
}

// endregion

// region 随机分组

private enum class GroupMode(val label: String) { GROUPS("随机分组"), ORDER("随机排序"), PICK("随机点名") }

@Composable
fun GroupsScreen() {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by Prefs.string("groups.text", "小明\n小红\n小刚\n小美\n阿杰\n小雨\n大壮\n朵朵")
    var mode by remember { mutableStateOf(GroupMode.GROUPS) }
    var groupCount by remember { mutableIntStateOf(2) }
    var bySize by remember { mutableStateOf(false) }
    var groupSize by remember { mutableIntStateOf(3) }
    var pickCount by remember { mutableIntStateOf(1) }
    var output by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var revealed by remember { mutableIntStateOf(0) }
    var rosterMenu by remember { mutableStateOf(false) }
    var revealJob by remember { mutableStateOf<Job?>(null) }

    val names = text.split('\n', ',', '，', '、', ' ', ';', '；').map { it.trim() }.filter { it.isNotEmpty() }

    fun describe(mode: GroupMode, output: List<List<String>>): String = when (mode) {
        GroupMode.GROUPS -> output.mapIndexed { i, g -> "第${i + 1}组：${g.joinToString("、")}" }.joinToString("\n")
        GroupMode.ORDER -> output.firstOrNull().orEmpty().mapIndexed { i, n -> "${i + 1}. $n" }.joinToString("\n")
        GroupMode.PICK -> "抽中：" + output.firstOrNull().orEmpty().joinToString("、")
    }
    val resultText = describe(mode, output)

    fun run() {
        if (names.size < 2) return
        val shuffled = TrueRandom.shuffled(names)
        val result = when (mode) {
            GroupMode.GROUPS -> {
                val count = if (bySize) (shuffled.size + groupSize - 1) / maxOf(1, groupSize) else minOf(groupCount, shuffled.size)
                val groups = List(maxOf(1, count)) { mutableListOf<String>() }
                shuffled.forEachIndexed { i, name -> groups[i % groups.size].add(name) }
                groups
            }
            GroupMode.ORDER -> listOf(shuffled)
            GroupMode.PICK -> listOf(shuffled.take(pickCount))
        }
        output = result
        // 在这里记录：和上次结果完全相同时也算一次
        Store.log(ToolKind.GROUPS.id, mode.label, describe(mode, result).replace("\n", "；"))
        revealed = 0
        val total = if (mode == GroupMode.GROUPS) result.size else result.firstOrNull()?.size ?: 0
        revealJob?.cancel()
        revealJob = scope.launch {
            for (i in 1..maxOf(1, total)) {
                delay(if (mode == GroupMode.ORDER) 90 else 220)
                revealed = i
                SoundEngine.play(SoundEffect.POP, 0.5f)
                Haptics.selection()
            }
            Haptics.success()
        }
    }

    OnShake { run() }

    Screen(title = "随机分组") {
        SegmentedPicker(GroupMode.entries, mode, { it.label }) {
            mode = it
            revealJob?.cancel()
            output = emptyList()
        }
        GlassCard(spacing = 10.dp) {
            SectionHeader("名单", subtitle = "${names.size} 人", icon = Icons.Rounded.Groups) {
                Box {
                    CircleIconButton(Icons.Rounded.FormatListBulleted, "名单管理", size = 34.dp) { rosterMenu = true }
                    DropdownMenu(expanded = rosterMenu, onDismissRequest = { rosterMenu = false }) {
                        for (roster in Store.rosters.items) {
                            DropdownMenuItem(text = { Text("载入「${roster.name}」（${roster.members.size}人）") }, onClick = {
                                text = roster.members.joinToString("\n")
                                rosterMenu = false
                            })
                        }
                        DropdownMenuItem(text = { Text("保存当前名单") }, onClick = {
                            Store.rosters.add(Roster(name = "名单 ${Store.rosters.items.size + 1}", members = names))
                            rosterMenu = false
                            Haptics.success()
                        })
                        for (roster in Store.rosters.items) {
                            DropdownMenuItem(text = { Text("删除「${roster.name}」") }, onClick = {
                                Store.rosters.update { list -> list.filter { it.id != roster.id } }
                                rosterMenu = false
                            })
                        }
                    }
                }
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                minLines = 4,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = palette.subtle(0.05f),
                    unfocusedContainerColor = palette.subtle(0.05f),
                    focusedBorderColor = palette.accent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = palette.accent,
                ),
            )
            SecondaryText("每行一个名字，也可以用逗号或空格分隔", style = Fonts.caption)
        }
        GlassCard {
            when (mode) {
                GroupMode.GROUPS -> {
                    ToggleRow("按每组人数分", bySize) { bySize = it }
                    if (bySize) {
                        StepperRow("每组 $groupSize 人", groupSize, 1..maxOf(1, names.size)) { groupSize = it }
                    } else {
                        StepperRow("分成 $groupCount 组", groupCount, 2..maxOf(2, minOf(20, names.size))) { groupCount = it }
                    }
                }
                GroupMode.ORDER -> SecondaryText("把名单随机打乱排一个顺序（发言顺序、值日表…）")
                GroupMode.PICK -> StepperRow("抽 $pickCount 人", pickCount, 1..maxOf(1, names.size)) { pickCount = it }
            }
        }
        BigActionButton("开始", Icons.Rounded.Shuffle, enabled = names.size >= 2) { run() }

        if (output.isNotEmpty()) {
            GlassCard {
                SectionHeader("结果", icon = Icons.Rounded.Verified) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircleIconButton(Icons.Rounded.ContentCopy, "拷贝结果", size = 34.dp) { copyText(context, resultText) }
                        CircleIconButton(Icons.Rounded.Share, "分享结果", size = 34.dp) {
                            shareText(context, "星匣 · ${mode.label}\n$resultText", mode.label + "结果")
                        }
                    }
                }
                when (mode) {
                    GroupMode.GROUPS -> output.forEachIndexed { index, group ->
                        if (index < revealed) {
                            val color = palette.color(index)
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(color.copy(alpha = 0.08f))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("第 ${index + 1} 组 · ${group.size} 人", style = Fonts.subheadline.weight(FontWeight.Bold), color = palette.inkColor(index))
                                Flow(spacing = 6.dp) {
                                    for (name in group) {
                                        Text(
                                            name,
                                            style = Fonts.subheadline,
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(color.copy(alpha = 0.18f))
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    else -> output.firstOrNull().orEmpty().forEachIndexed { index, name ->
                        if (index < revealed) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(palette.color(index)),
                                    contentAlignment = Alignment.Center,
                                ) { Text("${index + 1}", style = Fonts.headline, color = palette.color(index).readableForeground) }
                                Text(name, style = if (mode == GroupMode.PICK) Fonts.title3 else Fonts.body)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.size(1.dp))
    }
}

// endregion
