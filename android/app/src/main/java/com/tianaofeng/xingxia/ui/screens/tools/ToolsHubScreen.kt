package com.tianaofeng.xingxia.ui.screens.tools

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tianaofeng.xingxia.data.AppSettings
import com.tianaofeng.xingxia.ui.components.CircleIconButton
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GlassCard
import com.tianaofeng.xingxia.ui.components.Haptics
import com.tianaofeng.xingxia.ui.components.Screen
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.SectionHeader
import com.tianaofeng.xingxia.ui.components.TextAction
import com.tianaofeng.xingxia.ui.components.ToolIcon
import com.tianaofeng.xingxia.ui.components.fadingHorizontalScroll
import com.tianaofeng.xingxia.ui.components.glass
import com.tianaofeng.xingxia.ui.components.pressable
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.nav.LocalNavigator
import com.tianaofeng.xingxia.ui.nav.Routes
import com.tianaofeng.xingxia.ui.theme.LocalPalette

@Composable
fun ToolScreen(kind: ToolKind) {
    when (kind) {
        ToolKind.FOOD -> FoodScreen()
        ToolKind.WHEEL -> WheelScreen()
        ToolKind.GROUPS -> GroupsScreen()
        ToolKind.NUMBER -> NumberScreen()
        ToolKind.COIN -> CoinScreen()
        ToolKind.DICE -> DiceScreen()
        ToolKind.TRUTH_DARE -> TruthDareScreen()
        ToolKind.RPS -> RpsScreen()
        ToolKind.LOTTERY -> LotteryScreen()
        ToolKind.FINGER -> FingerScreen()
        ToolKind.GENERATORS -> GeneratorsScreen()
        ToolKind.SCRATCH -> ScratchScreen()
    }
}

/** 收藏的工具横排一行（问一问顶部，与 iOS 的 FavoriteToolsRow 一致）。没有收藏时不占位置 */
@Composable
fun FavoriteToolsRow() {
    val navigator = LocalNavigator.current
    // 收藏里可能有答案之书（"answers"），它不在工具列表里
    val favorites = AppSettings.favoriteTools.mapNotNull { FavoriteTool.of(it) }
    if (favorites.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("我的收藏", icon = Icons.Rounded.Star) {
            TextAction("管理") { navigator.open(Routes.FAVORITES) }
        }
        Row(Modifier.fadingHorizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            for (tool in favorites) FavoritePill(tool)
        }
    }
}

/** 全部工具，按「帮我决定 / 随机一下 / 聚会时光」分区排成格子，长按可收藏到首页（原「百宝」tab，现在嵌在问一问里） */
@Composable
fun ToolCategorySections() {
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        for (category in ToolCategory.entries) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(category.label, icon = category.icon)
                val tools = ToolKind.inCategory(category)
                val paired = if (tools.size % 2 == 0) tools else tools.dropLast(1)
                for (row in paired.chunked(2)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        for (tool in row) ToolTile(tool, Modifier.weight(1f))
                    }
                }
                // 单数时最后一个做成通栏
                if (tools.size % 2 == 1) WideToolTile(tools.last())
            }
        }
        SecondaryText("长按工具或在工具页右上角点星标，可以收藏到首页", style = Fonts.caption)
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.toolClick(id: String, onOpen: () -> Unit): Modifier =
    this.combinedClickable(
        interactionSource = MutableInteractionSource(),
        indication = null,
        onLongClickLabel = if (id in AppSettings.favoriteTools) "取消收藏" else "收藏到首页",
        onLongClick = {
            AppSettings.toggleFavorite(id)
            Haptics.success()
        },
        onClick = onOpen,
    )

@Composable
private fun ToolTile(tool: ToolKind, modifier: Modifier) {
    val navigator = LocalNavigator.current
    val palette = LocalPalette.current
    val favorite = tool.id in AppSettings.favoriteTools
    val colors = palette.identity(tool.colors)
    Column(
        modifier
            .alpha(if (tool.available) 1f else 0.55f)
            .toolClick(tool.id) { navigator.open(Routes.tool(tool)) }
            .glass(RoundedCornerShape(28.dp), tint = colors[0].copy(alpha = 0.08f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row {
            ToolIcon(tool.icon, colors, 48.dp)
            Spacer(Modifier.weight(1f))
            if (favorite) Icon(Icons.Rounded.Star, "已收藏", Modifier.size(16.dp), tint = palette.gold)
            if (!tool.available) Icon(Icons.Rounded.Construction, "即将推出", Modifier.size(16.dp), tint = palette.secondaryText)
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(tool.label, style = Fonts.headline, maxLines = 1)
            TileSubtitle(if (tool.available) tool.subtitle else "即将推出", colors[0], maxLines = 1)
        }
    }
}

@Composable
private fun WideToolTile(tool: ToolKind) {
    val navigator = LocalNavigator.current
    val palette = LocalPalette.current
    val colors = palette.identity(tool.colors)
    Row(
        Modifier
            .alpha(if (tool.available) 1f else 0.55f)
            .toolClick(tool.id) { navigator.open(Routes.tool(tool)) }
            .glass(RoundedCornerShape(28.dp), tint = colors[0].copy(alpha = 0.08f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ToolIcon(tool.icon, colors, 48.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tool.label, style = Fonts.headline)
                if (tool.id in AppSettings.favoriteTools) Icon(Icons.Rounded.Star, null, Modifier.size(14.dp), tint = palette.gold)
            }
            TileSubtitle(if (tool.available) tool.subtitle else "即将推出", colors[0])
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = palette.tertiaryText)
    }
}

/** 格子副标题：浅色模式下用工具色的深色版（比格子的浅色底深两档），深色模式照旧用次要文字色 */
@Composable
private fun TileSubtitle(text: String, color: Color, maxLines: Int = Int.MAX_VALUE) {
    val palette = LocalPalette.current
    Text(
        text,
        style = Fonts.caption,
        color = if (palette.isDark) palette.secondaryText else lerp(color, Color.Black, 0.5f),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun FavoritePill(tool: FavoriteTool) {
    val navigator = LocalNavigator.current
    val palette = LocalPalette.current
    Row(
        Modifier
            .toolClick(tool.id) { navigator.open(tool.route) }
            .glass(CircleShape)
            .padding(start = 8.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ToolIcon(tool.icon, palette.identity(tool.colors), 36.dp)
        Text(tool.label, style = Fonts.subheadline.weight(FontWeight.SemiBold))
    }
}

/** 管理首页「快捷工具」：调整顺序、移除、从全部工具里添加（与 iOS 的 FavoriteToolsEditor 一致） */
@Composable
fun FavoriteToolsScreen() {
    val palette = LocalPalette.current
    val view = LocalView.current
    val stored = AppSettings.favoriteTools
    val favorites = stored.mapNotNull { FavoriteTool.of(it) }
    val others = FavoriteTool.all.filter { it.id !in stored }

    // 收藏列表里可能存着已下线工具的 id：按显示出来的这部分排序，认不出的放回末尾
    fun move(from: Int, to: Int) {
        val shown = stored.filter { FavoriteTool.of(it) != null }.toMutableList()
        val unknown = stored.filter { FavoriteTool.of(it) == null }
        if (from !in shown.indices || to !in shown.indices) return
        shown.add(to, shown.removeAt(from))
        AppSettings.favoriteTools = shown + unknown
        Haptics.selection()
    }

    Screen(title = "快捷工具") {
        SecondaryText("首页「快捷工具」按这个顺序显示，用箭头调整。在工具页右上角点星标也能收藏或取消。", style = Fonts.footnote)
        GlassCard(padding = 8.dp, spacing = 0.dp) {
            SectionHeader("已收藏", icon = Icons.Rounded.Star, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
            if (favorites.isEmpty()) {
                SecondaryText("还没有收藏，从下面挑几个常用的吧", modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
            }
            favorites.forEachIndexed { index, tool ->
                if (index > 0) HorizontalDivider(color = palette.subtle(0.08f))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FavoriteToolLabel(tool, Modifier.weight(1f))
                    CircleIconButton(Icons.Rounded.KeyboardArrowUp, "上移${tool.label}", size = 34.dp, enabled = index > 0) { move(index, index - 1) }
                    CircleIconButton(Icons.Rounded.KeyboardArrowDown, "下移${tool.label}", size = 34.dp, enabled = index < favorites.lastIndex) {
                        move(index, index + 1)
                    }
                    CircleIconButton(Icons.Rounded.RemoveCircleOutline, "移除${tool.label}", size = 34.dp) {
                        AppSettings.toggleFavorite(tool.id)
                        Haptics.selection()
                    }
                }
            }
        }
        if (others.isNotEmpty()) {
            GlassCard(padding = 8.dp, spacing = 0.dp) {
                SectionHeader("可以添加", icon = Icons.Rounded.AddCircle, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
                others.forEachIndexed { index, tool ->
                    if (index > 0) HorizontalDivider(color = palette.subtle(0.08f))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .pressable {
                                AppSettings.toggleFavorite(tool.id)
                                Haptics.selection()
                                // 这一行会挪到「已收藏」里，读屏焦点跟不过去，补一句播报（与 iOS 一致）
                                view.announceForAccessibility("已添加${tool.label}")
                            }
                            // 读屏直接读「添加 xx」；点按动作来自前面的 pressable
                            .clearAndSetSemantics {
                                contentDescription = "添加${tool.label}"
                                role = Role.Button
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        FavoriteToolLabel(tool, Modifier.weight(1f))
                        Icon(Icons.Rounded.AddCircle, null, Modifier.size(24.dp), tint = palette.ink(palette.accent))
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteToolLabel(tool: FavoriteTool, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolIcon(tool.icon, palette.identity(tool.colors), 36.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(tool.label, style = Fonts.body, maxLines = 1)
            SecondaryText(tool.subtitle, style = Fonts.caption, maxLines = 1)
        }
    }
}

/** 记一笔决策历史时用的小工具 */
@Composable
fun rememberToggle(initial: Boolean = false) = remember { mutableStateOf(initial) }

@Composable
internal fun CenterBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier, contentAlignment = Alignment.Center) { content() }
}
