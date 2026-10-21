package com.tianaofeng.xingxia.ui.screens.ask

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tianaofeng.xingxia.data.DecisionRecord
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.Haptics
import com.tianaofeng.xingxia.ui.components.ToolIcon
import com.tianaofeng.xingxia.ui.components.glass
import com.tianaofeng.xingxia.ui.components.isReduceMotion
import com.tianaofeng.xingxia.ui.components.pressable
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.screens.tools.ToolKind
import com.tianaofeng.xingxia.ui.screens.tools.toolInfo
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import kotlinx.coroutines.delay

/**
 * 会弹出浮层的工具：真的在替人拿主意的那些（与 iOS 的 DecisionPrompt.promptTools 一致）。
 * 猜拳、刮刮乐、抽奖、分组、随机数之类不问；求签、塔罗、掷筊在日记里单独记，也不问。
 */
val DecisionPromptTools: Set<String> = setOf(
    ToolKind.FOOD.id, ToolKind.WHEEL.id, ToolKind.FINGER.id, ToolKind.COIN.id, ToolKind.DICE.id, "answers", "nearby",
)

/**
 * 刚记下一个决定时，底部浮出「工具：结果」和「再想想 / 就这么办」，选择写进 DecisionRecord.adopted。
 * 8 秒后（读屏打开时 20 秒）或切换 tab、页面时自动收起（由 AppNavigation 收起），收起不算表态。
 * tabBarVisible：底栏可见时浮在底栏上方，工具页等没有底栏时贴近底部安全区。
 */
@Composable
fun DecisionPrompt(tabBarVisible: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val view = LocalView.current
    val latest = Store.lastDecision
    // 历史里已经删掉的（比如刚清空了历史）不再问
    val record = latest?.takeIf { r -> r.tool in DecisionPromptTools && Store.records.items.any { it.id == r.id } }
    var reduceMotion by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { reduceMotion = isReduceMotion() }

    LaunchedEffect(record?.id) {
        val id = record?.id ?: return@LaunchedEffect
        val talkBack = (context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager)?.isTouchExplorationEnabled == true
        if (talkBack) {
            // 等工具自己的结果播报念完再提示
            delay(1_500)
            view.announceForAccessibility("要照这个结果做吗？屏幕底部可以选「就这么办」或「再想想」")
        }
        delay(if (talkBack) 20_000L else 8_000L)
        if (Store.lastDecision?.id == id) Store.lastDecision = null
    }

    AnimatedContent(
        targetState = record,
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = if (tabBarVisible) 84.dp else 8.dp)
            .padding(horizontal = 16.dp)
            .widthIn(max = 560.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter,
        transitionSpec = {
            val enter = if (reduceMotion) fadeIn() else fadeIn() + slideInVertically { it / 2 }
            val exit = if (reduceMotion) fadeOut() else fadeOut() + slideOutVertically { it / 2 }
            (enter togetherWith exit).using(SizeTransform(clip = false))
        },
        contentKey = { it?.id },
        label = "decisionPrompt",
    ) { shown ->
        if (shown != null) PromptCard(shown) { adopted -> decide(shown, adopted, view) }
    }
}

private fun decide(record: DecisionRecord, adopted: Boolean, view: android.view.View) {
    Store.setAdopted(record.id, adopted)
    if (adopted) Haptics.success() else Haptics.selection()
    view.announceForAccessibility(if (adopted) "已记下：就这么办" else "已记下：再想想")
    Store.lastDecision = null
}

@Composable
private fun PromptCard(record: DecisionRecord, onDecide: (Boolean) -> Unit) {
    val palette = LocalPalette.current
    val (name, icon, colors) = toolInfo(record.tool)
    val summary = "$name：${record.result}"
    val tint = palette.backgroundBottom.copy(alpha = if (palette.isDark) 0.88f else 0.8f)
    // 字号调大时一行放不下两个按钮，改成两行卡片（对应 iOS 的 ViewThatFits）
    if (LocalDensity.current.fontScale >= 1.3f) {
        Column(
            Modifier
                .fillMaxWidth()
                .blockTouches()
                .glass(RoundedCornerShape(24.dp), tint = tint)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolIcon(icon, colors, 30.dp)
                Text(summary, style = Fonts.subheadline.weight(FontWeight.SemiBold), color = palette.text, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                PromptButtons(onDecide)
            }
        }
    } else {
        Row(
            Modifier
                .fillMaxWidth()
                .blockTouches()
                .glass(RoundedCornerShape(28.dp), tint = tint)
                .padding(start = 8.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ToolIcon(icon, colors, 30.dp)
            Text(
                summary,
                style = Fonts.subheadline.weight(FontWeight.SemiBold),
                color = palette.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { PromptButtons(onDecide) }
        }
    }
}

@Composable
private fun PromptButtons(onDecide: (Boolean) -> Unit) {
    val palette = LocalPalette.current
    Box(
        Modifier
            .pressable { onDecide(false) }
            .semantics { role = Role.Button }
            .clip(CircleShape)
            .background(palette.subtle(0.1f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text("再想想", style = Fonts.subheadline.weight(FontWeight.Medium), color = palette.text, maxLines = 1)
    }
    Box(
        Modifier
            .pressable { onDecide(true) }
            .semantics { role = Role.Button }
            .clip(CircleShape)
            .background(palette.accentBrush)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text("就这么办", style = Fonts.subheadline.weight(FontWeight.SemiBold), color = palette.onAccent, maxLines = 1)
    }
}

/** 卡片空白处的点按不要穿透到下面的页面 */
private fun Modifier.blockTouches(): Modifier = pointerInput(Unit) { detectTapGestures { } }
