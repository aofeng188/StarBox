package com.tianaofeng.xingxia.ui.screens.ask

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tianaofeng.xingxia.data.Prefs
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.data.WheelOption
import com.tianaofeng.xingxia.data.WheelTemplate
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GradientIcon
import com.tianaofeng.xingxia.ui.components.glass
import com.tianaofeng.xingxia.ui.components.pressable
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.theme.LocalPalette

/** 工具页、占卜页顶部的「你在问：……」。从问一问带着问题进来时显示，点 × 清掉（与 iOS 的 QuestionBanner 一致） */
@Composable
fun QuestionBanner(modifier: Modifier = Modifier) {
    val question = Store.currentQuestion?.takeIf { it.isNotEmpty() } ?: return
    val palette = LocalPalette.current
    Row(
        modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(20.dp))
            .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GradientIcon(Icons.Rounded.QuestionAnswer, size = 18.dp)
        Column(
            Modifier
                .weight(1f)
                .clearAndSetSemantics { contentDescription = "你在问：$question" },
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text("你在问", style = Fonts.caption2, color = palette.secondaryText)
            Text(question, style = Fonts.subheadline.weight(FontWeight.SemiBold), color = palette.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier
                .size(40.dp)
                .pressable { Store.currentQuestion = null },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Close, "清除问题", Modifier.size(18.dp), tint = palette.secondaryText)
        }
    }
}

/**
 * 页面自带问题框时（硬币、塔罗、答案之书）：进来时把问一问的问题预填进去，不再显示横幅。
 * 预填过的页面，之后改问题会同步回 Store.currentQuestion（清空则置 null），记进历史的问题和框里一致。
 * 与 iOS 的 prefillsQuestion 一致：每次进入页面（不是每次重组）看一次要不要预填。
 */
@Composable
fun PrefillQuestion(text: String, onPrefill: (String) -> Unit) {
    var prefilled by rememberSaveable { mutableStateOf(false) }
    // 这次进入页面后上一次看到的问题框内容；null 表示刚进入，相当于 iOS 的 onAppear
    var lastSeen by remember { mutableStateOf<String?>(null) }
    val prefill by rememberUpdatedState(onPrefill)
    LaunchedEffect(text) {
        val previous = lastSeen
        lastSeen = text
        if (previous == null) {
            val question = Store.currentQuestion
            if (!prefilled && text.isEmpty() && !question.isNullOrEmpty()) {
                prefill(question)
                prefilled = true
            }
            return@LaunchedEffect
        }
        if (!prefilled) return@LaunchedEffect
        val value = text.trim().ifEmpty { null }
        if (Store.currentQuestion != value) Store.currentQuestion = value
    }
}

/** 问一问带到工具页的一次性数据 */
object AskHandoff {
    /** 问一问推荐的塔罗牌阵（TarotSpread 的 key：single / time / love），塔罗页出现时取走 */
    var pendingTarotSpread: String? = null

    fun takeTarotSpread(): String? = pendingTarotSpread.also { pendingTarotSpread = null }
}

/** 问一问拆出的选项写进专用的「问一问」转盘（只建一个，之后每次覆盖它的选项），并把转盘页切到它 */
object AskWheel {
    private const val TEMPLATE_KEY = "ask.wheelTemplate"
    const val TEMPLATE_NAME = "问一问"

    fun apply(options: List<String>) {
        if (options.size < 2) return
        val storedId = Prefs.string(TEMPLATE_KEY, "")
        val items = options.map { WheelOption(title = it) }
        val existing = Store.wheels.items.firstOrNull { it.id == storedId.value }
        val id = if (existing != null) {
            Store.wheels.update { list -> list.map { if (it.id == existing.id) it.copy(options = items) else it } }
            existing.id
        } else {
            val created = WheelTemplate(name = TEMPLATE_NAME, options = items)
            Store.wheels.add(created)
            storedId.value = created.id
            created.id
        }
        // 转盘页按这个键选当前转盘
        Prefs.string("wheel.lastTemplate", "").value = id
    }
}
