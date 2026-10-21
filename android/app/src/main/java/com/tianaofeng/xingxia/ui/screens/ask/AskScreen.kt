package com.tianaofeng.xingxia.ui.screens.ask

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CropPortrait
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tianaofeng.xingxia.core.AskMethod
import com.tianaofeng.xingxia.core.QuestionAnalysis
import com.tianaofeng.xingxia.core.QuestionClassifier
import com.tianaofeng.xingxia.core.QuestionKind
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.ui.components.ChipRow
import com.tianaofeng.xingxia.ui.components.CircleIconButton
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GlassCard
import com.tianaofeng.xingxia.ui.components.GradientIcon
import com.tianaofeng.xingxia.ui.components.Haptics
import com.tianaofeng.xingxia.ui.components.Screen
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.TagChip
import com.tianaofeng.xingxia.ui.components.TextAction
import com.tianaofeng.xingxia.ui.components.ToolIcon
import com.tianaofeng.xingxia.ui.components.pressable
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.nav.LocalNavigator
import com.tianaofeng.xingxia.ui.nav.Routes
import com.tianaofeng.xingxia.ui.screens.divination.DivinationSection
import com.tianaofeng.xingxia.ui.screens.tools.FavoriteToolsRow
import com.tianaofeng.xingxia.ui.screens.tools.ToolCategorySections
import com.tianaofeng.xingxia.ui.screens.tools.ToolKind
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.hex
import kotlinx.coroutines.delay

private val EXAMPLES = listOf("中午吃火锅还是烤肉？", "周末要不要去杭州？", "哪天搬家比较好？", "他是不是喜欢我？", "奶茶选哪一杯？")

/**
 * 「问一问」tab 根页：问题框 → 推荐方法；下面是收藏、占卜区和全部工具（原「占卜」「百宝」两个 tab 的入口都在这里）。
 * Android 没有单独的搜索 tab，右上角的搜索按名称、关键词和拼音首字母找全部工具、占卜、记录页和设置。
 */
@Composable
fun AskScreen() {
    val navigator = LocalNavigator.current
    val view = LocalView.current
    val focusManager = LocalFocusManager.current

    var draft by rememberSaveable { mutableStateOf("") }
    // 判断过的问题；问题框改成别的内容后结果收起，重新提交再判断
    var analyzedText by rememberSaveable { mutableStateOf("") }
    val analysis = remember(analyzedText) { analyzedText.takeIf { it.isNotEmpty() }?.let { QuestionClassifier.analyze(it) } }
    // 收起动画时结果已经是 null，留住最后一次的内容
    val lastAnalysis = remember { arrayOfNulls<QuestionAnalysis>(1) }
    if (analysis != null) lastAnalysis[0] = analysis

    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var focusSearch by remember { mutableStateOf(false) }

    fun onDraftChange(value: String) {
        draft = value
        val trimmed = value.trim()
        if (trimmed.isNotEmpty() && trimmed == analyzedText) return
        // 清空或改成了别的问题：上一题的结果和带到工具页的问题都作废
        if (Store.currentQuestion != null) Store.currentQuestion = null
        analyzedText = ""
    }

    fun submit(text: String) {
        val question = text.trim()
        if (question.isEmpty()) return
        focusManager.clearFocus()
        val result = QuestionClassifier.analyze(question)
        analyzedText = question
        Store.currentQuestion = question
        Haptics.selection()
        view.announceForAccessibility("${result.kind.title}。推荐${result.methods.joinToString("、") { it.label }}")
    }

    fun go(method: AskMethod, result: QuestionAnalysis) {
        Store.currentQuestion = analyzedText.ifEmpty { null }
        if (method == AskMethod.WHEEL && result.options.size >= 2) AskWheel.apply(result.options)
        AskHandoff.pendingTarotSpread = method.tarotSpreadKey
        navigator.open(method.route)
    }

    fun closeSearch() {
        searching = false
        query = ""
        focusManager.clearFocus()
    }

    BackHandler(enabled = searching) { closeSearch() }

    Screen(
        title = if (searching) "搜索" else "问一问",
        isRoot = true,
        spacing = 22.dp,
        actions = {
            if (!searching) {
                CircleIconButton(Icons.Rounded.Search, "搜索工具、占卜、记录和设置") {
                    searching = true
                    focusSearch = true
                }
            }
        },
    ) {
        if (searching) {
            val searchFocus = remember { FocusRequester() }
            val clearSearch: @Composable () -> Unit = { ClearButton("清空搜索") { query = "" } }
            LaunchedEffect(focusSearch) {
                if (focusSearch) {
                    runCatching { searchFocus.requestFocus() }
                    focusSearch = false
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AskTextField(
                    value = query,
                    placeholder = "工具、占卜、记录、设置",
                    onChange = { query = it },
                    imeAction = ImeAction.Search,
                    onIme = { focusManager.clearFocus() },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(searchFocus),
                    trailing = if (query.isEmpty()) null else clearSearch,
                )
                TextAction("取消") { closeSearch() }
            }
            AskSearchResults(query)
        } else {
            QuestionCard(
                draft = draft,
                hasResult = analysis != null,
                onChange = { onDraftChange(it) },
                onSubmit = { submit(it) },
                onExample = { example ->
                    draft = example
                    submit(example)
                },
            )

            AnimatedVisibility(
                visible = analysis != null,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
            ) {
                lastAnalysis[0]?.let { shown -> AskResultCard(shown) { go(it, shown) } }
            }

            FavoriteToolsRow()

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AskSectionTitle("占卜", "求签 · 塔罗 · 星座 · 黄历")
                DivinationSection()
            }

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AskSectionTitle("工具", "帮我决定 · 随机一下 · 聚会时光")
                ToolCategorySections()
            }
        }
    }
}

// region 问题框

@Composable
private fun QuestionCard(
    draft: String,
    hasResult: Boolean,
    onChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onExample: (String) -> Unit,
) {
    val palette = LocalPalette.current
    val focus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    var placeholderIndex by rememberSaveable { mutableIntStateOf(0) }
    val canSubmit = draft.isNotBlank()
    val currentDraft by rememberUpdatedState(draft)

    // 问题框空着时轮换示例
    LaunchedEffect(Unit) {
        while (true) {
            delay(3_500)
            if (currentDraft.isEmpty() && !focused) placeholderIndex = (placeholderIndex + 1) % EXAMPLES.size
        }
    }

    GlassCard(spacing = 14.dp) {
        Text(
            "你想决定什么？",
            style = Fonts.serif(24, bold = true),
            color = palette.text,
            modifier = Modifier.semantics { heading() },
        )
        AskTextField(
            value = draft,
            placeholder = EXAMPLES[placeholderIndex % EXAMPLES.size],
            onChange = onChange,
            imeAction = ImeAction.Go,
            onIme = { onSubmit(draft) },
            modifier = Modifier
                .focusRequester(focus)
                .onFocusChanged { focused = it.isFocused },
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (draft.isNotEmpty()) {
                        ClearButton("清空问题") {
                            onChange("")
                            runCatching { focus.requestFocus() }
                        }
                    }
                    Box(
                        Modifier
                            .padding(end = 6.dp)
                            .size(38.dp)
                            .alpha(if (canSubmit) 1f else 0.4f)
                            .pressable(enabled = canSubmit) { onSubmit(draft) }
                            .semantics { role = Role.Button }
                            .clip(CircleShape)
                            .background(palette.accentBrush),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.ArrowUpward, "问一问", Modifier.size(20.dp), tint = palette.onAccent)
                    }
                }
            },
        )
        if (!hasResult) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryText("试试这样问", style = Fonts.caption)
                ChipRow {
                    for (example in EXAMPLES) {
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .pressable { onExample(example) }
                                .semantics { role = Role.Button },
                        ) { TagChip(example) }
                    }
                }
            }
        }
    }
}

/** 玻璃底的输入框（与 GlassTextField 同一套颜色），可以带尾部按钮和键盘动作 */
@Composable
private fun AskTextField(
    value: String,
    placeholder: String,
    onChange: (String) -> Unit,
    imeAction: ImeAction,
    onIme: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val palette = LocalPalette.current
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text(placeholder, color = palette.tertiaryText, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingIcon = trailing,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onIme() }),
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = palette.glassFill,
            unfocusedContainerColor = palette.glassFill,
            focusedBorderColor = palette.accent,
            unfocusedBorderColor = palette.glassStroke,
            focusedTextColor = palette.text,
            unfocusedTextColor = palette.text,
            cursorColor = palette.accent,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun ClearButton(description: String, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Box(
        Modifier
            .size(36.dp)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Close, description, Modifier.size(18.dp), tint = palette.secondaryText)
    }
}

/** 问一问里的大分区标题 */
@Composable
private fun AskSectionTitle(title: String, subtitle: String) {
    val palette = LocalPalette.current
    Column(
        Modifier.semantics(mergeDescendants = true) { heading() },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(title, style = Fonts.title2, color = palette.text)
        SecondaryText(subtitle, style = Fonts.caption)
    }
}

// endregion

// region 推荐结果

@Composable
private fun AskResultCard(analysis: QuestionAnalysis, onPick: (AskMethod) -> Unit) {
    val palette = LocalPalette.current
    GlassCard(spacing = 14.dp) {
        Row(
            Modifier.semantics(mergeDescendants = true) { },
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GradientIcon(analysis.kind.icon, size = 28.dp)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("「${analysis.kind.title}」，推荐这样决定", style = Fonts.headline, color = palette.text)
                SecondaryText(analysis.kind.hint, style = Fonts.caption)
            }
        }

        if (analysis.options.isNotEmpty()) {
            ChipRow(Modifier.clearAndSetSemantics { contentDescription = "选项：${analysis.options.joinToString("、")}" }) {
                for (option in analysis.options) TagChip(option)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            analysis.methods.forEachIndexed { index, method ->
                val reason = method.reason(analysis)
                val recommended = index == 0
                Row(
                    Modifier
                        .fillMaxWidth()
                        .pressable { onPick(method) }
                        // 读屏读「推荐：方法，理由」；点按动作来自前面的 pressable
                        .clearAndSetSemantics {
                            contentDescription = (if (recommended) "推荐：" else "") + "${method.label}，$reason"
                            role = Role.Button
                        }
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (recommended) palette.accent.copy(alpha = if (palette.isDark) 0.16f else 0.12f) else palette.subtle(0.05f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ToolIcon(method.icon, palette.identity(method.colors), 40.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                method.label,
                                style = Fonts.headline,
                                color = palette.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (recommended) TagChip("推荐", icon = Icons.Rounded.ThumbUp)
                        }
                        SecondaryText(reason, style = Fonts.caption)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = palette.tertiaryText)
                }
            }
        }
    }
}

// endregion

// region 类型与方法的展示（文案与 iOS 的 AskView.swift 一致）

val QuestionKind.title: String
    get() = when (this) {
        QuestionKind.YES_NO -> "是非题"
        QuestionKind.CHOICE -> "多选一"
        QuestionKind.FOOD -> "吃什么"
        QuestionKind.TIMING -> "挑时机"
        QuestionKind.RELATIONSHIP -> "感情事"
        QuestionKind.GENERAL -> "随便问问"
    }

val QuestionKind.hint: String
    get() = when (this) {
        QuestionKind.YES_NO -> "答案只有「是」或「否」，交给运气最干脆"
        QuestionKind.CHOICE -> "在几个选项里挑一个"
        QuestionKind.FOOD -> "吃饭的事，交给美食库和转盘"
        QuestionKind.TIMING -> "关于什么时候，看看签文和黄历"
        QuestionKind.RELATIONSHIP -> "关于两个人，听听塔罗和缘分怎么说"
        QuestionKind.GENERAL -> "没想好怎么问？先翻一页答案之书"
    }

val QuestionKind.icon: ImageVector
    get() = when (this) {
        QuestionKind.YES_NO -> Icons.Rounded.CheckCircle
        QuestionKind.CHOICE -> Icons.Rounded.FormatListBulleted
        QuestionKind.FOOD -> Icons.Rounded.Restaurant
        QuestionKind.TIMING -> Icons.Rounded.CalendarMonth
        QuestionKind.RELATIONSHIP -> Icons.Rounded.Favorite
        QuestionKind.GENERAL -> Icons.Rounded.AutoAwesome
    }

/** 方法名（iOS 的 AskMethod.name；Kotlin 枚举自带 name，这里叫 label） */
val AskMethod.label: String
    get() = when (this) {
        AskMethod.COIN -> ToolKind.COIN.label
        AskMethod.ANSWERS -> "答案之书"
        AskMethod.TAROT_SINGLE -> "塔罗 · 单张指引"
        AskMethod.TAROT_TIME -> "塔罗 · 时间之流"
        AskMethod.TAROT_LOVE -> "塔罗 · 情感关系"
        AskMethod.WHEEL -> ToolKind.WHEEL.label
        AskMethod.FOOD -> ToolKind.FOOD.label
        AskMethod.FINGER -> ToolKind.FINGER.label
        AskMethod.STICK -> "求签"
        AskMethod.AUSPICIOUS -> "择吉日"
        AskMethod.COMPATIBILITY -> "缘分配对"
        AskMethod.DICE -> ToolKind.DICE.label
    }

val AskMethod.route: String
    get() = when (this) {
        AskMethod.COIN -> Routes.tool(ToolKind.COIN)
        AskMethod.ANSWERS -> Routes.ANSWERS
        AskMethod.TAROT_SINGLE, AskMethod.TAROT_TIME, AskMethod.TAROT_LOVE -> Routes.TAROT
        AskMethod.WHEEL -> Routes.tool(ToolKind.WHEEL)
        AskMethod.FOOD -> Routes.tool(ToolKind.FOOD)
        AskMethod.FINGER -> Routes.tool(ToolKind.FINGER)
        AskMethod.STICK -> Routes.STICK
        AskMethod.AUSPICIOUS -> Routes.AUSPICIOUS
        AskMethod.COMPATIBILITY -> Routes.COMPATIBILITY
        AskMethod.DICE -> Routes.tool(ToolKind.DICE)
    }

/** 塔罗类方法预选的牌阵（TarotSpread 的 key） */
val AskMethod.tarotSpreadKey: String?
    get() = when (this) {
        AskMethod.TAROT_SINGLE -> "single"
        AskMethod.TAROT_TIME -> "time"
        AskMethod.TAROT_LOVE -> "love"
        else -> null
    }

val AskMethod.icon: ImageVector
    get() = when (this) {
        AskMethod.COIN -> ToolKind.COIN.icon
        AskMethod.ANSWERS -> Icons.Rounded.MenuBook
        AskMethod.TAROT_SINGLE -> Icons.Rounded.CropPortrait
        AskMethod.TAROT_TIME -> Icons.Rounded.Schedule
        AskMethod.TAROT_LOVE -> Icons.Rounded.Favorite
        AskMethod.WHEEL -> ToolKind.WHEEL.icon
        AskMethod.FOOD -> ToolKind.FOOD.icon
        AskMethod.FINGER -> ToolKind.FINGER.icon
        AskMethod.STICK -> Icons.Rounded.LocalFireDepartment
        AskMethod.AUSPICIOUS -> Icons.Rounded.EventAvailable
        AskMethod.COMPATIBILITY -> Icons.Rounded.Favorite
        AskMethod.DICE -> ToolKind.DICE.icon
    }

val AskMethod.colors: List<Color>
    get() = when (this) {
        AskMethod.COIN -> ToolKind.COIN.colors
        AskMethod.ANSWERS -> listOf(hex(0xF5B83D), hex(0xB45309))
        AskMethod.TAROT_SINGLE, AskMethod.TAROT_TIME, AskMethod.TAROT_LOVE -> listOf(hex(0x8B5CF6), hex(0x3B82F6))
        AskMethod.WHEEL -> ToolKind.WHEEL.colors
        AskMethod.FOOD -> ToolKind.FOOD.colors
        AskMethod.FINGER -> ToolKind.FINGER.colors
        AskMethod.STICK -> listOf(hex(0xFF8A3D), hex(0xE11D48))
        AskMethod.AUSPICIOUS -> listOf(hex(0xEF4444), hex(0xF59E0B))
        AskMethod.COMPATIBILITY -> listOf(hex(0xFB7185), hex(0xF43F5E))
        AskMethod.DICE -> ToolKind.DICE.colors
    }

/** 推荐理由，一句话 */
fun AskMethod.reason(analysis: QuestionAnalysis): String {
    val options = analysis.options
    return when (this) {
        AskMethod.COIN -> when {
            options.size == 2 -> "正面「${options[0]}」，反面「${options[1]}」，抛一次最干脆"
            analysis.kind == QuestionKind.YES_NO -> "最快的办法：正面就做，反面就算"
            else -> "拿不准时，让硬币替你迈出第一步"
        }
        AskMethod.ANSWERS -> if (analysis.kind == QuestionKind.RELATIONSHIP) "心里其实有答案时，翻一页印证一下" else "默念问题翻开一页，听听书怎么说"
        AskMethod.TAROT_SINGLE -> "想听点理由时，抽一张牌看看提示"
        AskMethod.TAROT_TIME -> "三张牌看过去、现在和未来，找准时机"
        AskMethod.TAROT_LOVE -> "三张牌看你、对方和这段关系的走向"
        AskMethod.WHEEL -> if (options.size >= 2) "把这 ${options.size} 个选项放上转盘，转到哪个就是哪个" else "把候选写上转盘，转到哪个算哪个"
        AskMethod.FOOD -> "按时段和口味，从美食库里挑一道"
        AskMethod.FINGER -> "大家各按一根手指，随机选出一个"
        AskMethod.STICK -> if (analysis.kind == QuestionKind.TIMING) "先求一支签，看看最近的时运" else "求一支签，看看今天的指引"
        AskMethod.AUSPICIOUS -> "从黄历里挑出最近适合的好日子"
        AskMethod.COMPATIBILITY -> "看看你们的星座和生肖合不合"
        AskMethod.DICE -> if (options.size in 3..6) "给 ${options.size} 个选项编号，掷出几点选几号" else "掷个骰子，把选择交给点数"
    }
}

// endregion
