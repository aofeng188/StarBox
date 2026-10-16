package com.tianaofeng.xingxia.ui.screens.divination

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tianaofeng.xingxia.core.DateText
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.data.TarotReading
import com.tianaofeng.xingxia.ui.components.EmptyHint
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GlassCard
import com.tianaofeng.xingxia.ui.components.Haptics
import com.tianaofeng.xingxia.ui.components.ReadableWidth
import com.tianaofeng.xingxia.ui.components.Screen
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.TextAction
import com.tianaofeng.xingxia.ui.components.glass
import com.tianaofeng.xingxia.ui.components.rememberToday
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import kotlinx.coroutines.delay
import java.time.Instant

private val TarotReading.instant: Instant get() = Instant.ofEpochMilli(date)

private val TarotReading.day: DayKey get() = DayKey.of(instant)

/** 「愚者（正位） · 女祭司（逆位）」 */
private fun cardsLine(entries: List<DrawnCard>): String =
    entries.joinToString(" · ") { "${it.card.name}（${if (it.reversed) "逆位" else "正位"}）" }

/** 塔罗手记：按时间倒序回看每次占卜的牌阵和解读（与 iOS 的 TarotJournalView 一致） */
@Composable
fun TarotJournalScreen() {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val view = LocalView.current
    val today = rememberToday()
    // 提到外面：看完一条解读回来，列表停在原来的位置
    val listScroll = rememberScrollState()
    var openedId by rememberSaveable { mutableStateOf<String?>(null) }
    // 刚删除的手记，几秒内可以撤销
    var deleted by remember { mutableStateOf<TarotReading?>(null) }
    val readings = Store.tarotReadings.items.sortedByDescending { it.date }

    LaunchedEffect(deleted) {
        if (deleted == null) return@LaunchedEffect
        // 读屏用户找到「撤销」需要更久
        val touchExploring = (context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager)?.isTouchExplorationEnabled == true
        delay(if (touchExploring) 10_000L else 5_000L)
        deleted = null
    }

    fun delete(reading: TarotReading) {
        Store.tarotReadings.update { list -> list.filter { it.id != reading.id } }
        Haptics.tap()
        if (openedId == reading.id) openedId = null
        deleted = reading
        view.announceForAccessibility("已删除这条塔罗手记，可以撤销")
    }

    fun undoDelete() {
        val snapshot = deleted ?: return
        deleted = null
        if (Store.tarotReadings.items.none { it.id == snapshot.id }) {
            Store.tarotReadings.update { list -> (list + snapshot).sortedBy { it.date } }
        }
        Haptics.success()
        view.announceForAccessibility("已恢复")
    }

    val opened = openedId?.let { id -> readings.firstOrNull { it.id == id } }
    // 打开的那条已经不在了（比如被删除），回到列表
    LaunchedEffect(openedId, opened == null) {
        if (openedId != null && opened == null) openedId = null
    }
    if (opened != null) {
        TarotReadingDetail(opened, onBack = { openedId = null })
        return
    }

    Box(Modifier.fillMaxSize()) {
        Screen(title = "塔罗手记", scrollState = listScroll) {
            if (readings.isEmpty()) {
                EmptyHint(Icons.Rounded.Book, "还没有塔罗手记", "完成一次塔罗占卜、翻开所有牌后，牌阵和解读会自动记在这里")
            } else {
                SecondaryText("共 ${readings.size} 次占卜 · 点按重看解读，长按可删除", style = Fonts.caption)
                // readings 已按时间倒序，groupBy 保留先后顺序
                val grouped = readings.groupBy { it.day }
                for ((day, items) in grouped) {
                    Text(
                        if (day == today) "今天" else "${DateText.full(day)} ${DateText.weekday(day)}",
                        style = Fonts.footnote.weight(FontWeight.SemiBold),
                        color = palette.secondaryText,
                        modifier = Modifier
                            .padding(start = 4.dp, top = 4.dp)
                            .semantics { heading() },
                    )
                    GlassCard(cornerRadius = 22.dp, padding = 8.dp, spacing = 0.dp) {
                        items.forEachIndexed { index, reading ->
                            if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 10.dp), color = palette.subtle())
                            // 删掉一条后，下面几行的菜单状态不串位
                            key(reading.id) {
                                TarotJournalRow(reading, onOpen = { openedId = reading.id }, onDelete = { delete(reading) })
                            }
                        }
                    }
                }
            }
        }

        if (deleted != null) {
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                    .widthIn(max = ReadableWidth)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    // 压在页面内容上，底色不透明一些才读得清
                    .background(palette.backgroundBottom.copy(alpha = 0.94f))
                    .glass(RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Rounded.Delete, null, Modifier.size(18.dp), tint = palette.secondaryText)
                Text("已删除一条塔罗手记", style = Fonts.subheadline, color = palette.text, modifier = Modifier.weight(1f))
                TextAction("撤销") { undoDelete() }
            }
        }
    }
}

/** 手记的一行：点按重看解读，长按弹出「删除」；读屏可直接用自定义操作删除 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TarotJournalRow(reading: TarotReading, onOpen: () -> Unit, onDelete: () -> Unit) {
    val palette = LocalPalette.current
    val spread = TarotSpread.of(reading.spread)
    val entries = remember(reading.cards) { decodeTarotCards(reading.cards) }
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .combinedClickable(
                    onClickLabel = "重看解读",
                    onLongClickLabel = "删除",
                    onLongClick = { menu = true },
                    onClick = onOpen,
                )
                .semantics { customActions = listOf(CustomAccessibilityAction("删除") { onDelete(); true }) }
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(palette.accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) { Icon(spread.icon, null, Modifier.size(18.dp), tint = palette.ink(palette.accent)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(spread.label, style = Fonts.headline, color = palette.text, modifier = Modifier.weight(1f))
                    SecondaryText(DateText.time(reading.instant), style = Fonts.caption)
                }
                if (reading.question.isNotEmpty()) {
                    Text("「${reading.question}」", style = Fonts.subheadline, color = palette.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (entries.isNotEmpty()) SecondaryText(cardsLine(entries), style = Fonts.caption)
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text("删除", color = palette.negative) },
                leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = palette.negative) },
                onClick = {
                    menu = false
                    onDelete()
                },
            )
        }
    }
}

/** 重看某一次占卜：牌面全部翻开，释义与当时的塔罗页一致 */
@Composable
private fun TarotReadingDetail(reading: TarotReading, onBack: () -> Unit) {
    val spread = TarotSpread.of(reading.spread)
    val entries = remember(reading.cards) { decodeTarotCards(reading.cards) }
    val day = reading.day
    Screen(
        title = spread.label,
        onBack = onBack,
        horizontalAlignment = Alignment.CenterHorizontally,
        spacing = 18.dp,
        actions = {
            if (entries.isNotEmpty()) {
                ShareCardButton("分享解读", "tarot") { style ->
                    ShareCardFrame(style, day, "星匣 · 塔罗占卜") {
                        TarotShareContent(spread, reading.question, entries, style)
                    }
                }
            }
        },
    ) {
        Column(
            Modifier.padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SecondaryText("${DateText.full(day)} ${DateText.weekday(day)} ${DateText.time(reading.instant)}", style = Fonts.caption)
            if (reading.question.isNotEmpty()) {
                SecondaryText("「${reading.question}」", style = Fonts.serif(18), textAlign = TextAlign.Center)
            }
        }

        if (entries.isEmpty()) {
            EmptyHint(Icons.Rounded.Book, "这次的牌没能读出来", "记录里的牌在当前牌库中找不到")
        } else {
            TarotDetailCards(spread, entries)
            entries.forEachIndexed { slot, entry -> TarotInterpretation(spread, slot, entry, reading.question) }
            TarotSummary(entries)
        }
    }
}

/** 详情页一排全部翻开的牌和牌位名 */
@Composable
private fun TarotDetailCards(spread: TarotSpread, entries: List<DrawnCard>) {
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val single = entries.size == 1
        val gap = 12.dp
        // 多张牌按可用宽度收窄，窄屏也不挤出屏幕
        val width = if (single) 160.dp else minOf(100.dp, (maxWidth - gap * (entries.size - 1)) / entries.size)
        Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.Top) {
            entries.forEachIndexed { slot, entry ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlippableTarotCard(entry, true, Modifier.size(width, width * 1.62f))
                    SecondaryText(
                        spread.position(slot),
                        style = Fonts.caption.weight(FontWeight.SemiBold),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = width + gap),
                    )
                }
            }
        }
    }
}
