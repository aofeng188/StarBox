package com.tianaofeng.xingxia.ui.screens.tools

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tianaofeng.xingxia.data.AppSettings
import com.tianaofeng.xingxia.ui.components.EmptyHint
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.Haptics
import com.tianaofeng.xingxia.ui.components.Screen
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.SectionHeader
import com.tianaofeng.xingxia.ui.components.ToolIcon
import com.tianaofeng.xingxia.ui.components.glass
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.nav.LocalNavigator
import com.tianaofeng.xingxia.ui.nav.Routes
import com.tianaofeng.xingxia.ui.screens.divination.GlassTextField
import com.tianaofeng.xingxia.ui.screens.records.ComingSoonScreen
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.hex

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
        else -> ComingSoonScreen(kind.label)
    }
}

@Composable
fun ToolsHubScreen() {
    var search by rememberSaveable { mutableStateOf("") }
    val favorites = AppSettings.favoriteTools.mapNotNull { ToolKind.of(it) }

    Screen(title = "百宝箱", isRoot = true, spacing = 22.dp) {
        GlassTextField(search, "搜索工具") { search = it }
        if (search.isBlank()) {
            if (favorites.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader("我的收藏", icon = Icons.Rounded.Star)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (tool in favorites) FavoritePill(tool)
                    }
                }
            }
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
        } else {
            val query = search.trim()
            val matches = ToolKind.entries.filter { query in it.label || query in it.subtitle || query in it.keywords }
            if (matches.isEmpty()) {
                EmptyHint(Icons.Rounded.Search, "没有找到「$query」", "试试「吃」「硬币」「骰子」等关键词")
            } else {
                for (row in matches.chunked(2)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        for (tool in row) ToolTile(tool, Modifier.weight(1f))
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        SecondaryText("长按工具可以收藏到首页", style = Fonts.caption)
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.toolClick(tool: ToolKind, onOpen: () -> Unit): Modifier =
    this.combinedClickable(
        interactionSource = MutableInteractionSource(),
        indication = null,
        onLongClick = {
            AppSettings.toggleFavorite(tool.id)
            Haptics.success()
        },
        onClick = onOpen,
    )

@Composable
private fun ToolTile(tool: ToolKind, modifier: Modifier) {
    val navigator = LocalNavigator.current
    val palette = LocalPalette.current
    val favorite = tool.id in AppSettings.favoriteTools
    Column(
        modifier
            .alpha(if (tool.available) 1f else 0.55f)
            .toolClick(tool) { navigator.open(Routes.tool(tool)) }
            .glass(RoundedCornerShape(28.dp), tint = tool.colors[0].copy(alpha = 0.08f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row {
            ToolIcon(tool.icon, tool.colors, 48.dp)
            Spacer(Modifier.weight(1f))
            if (favorite) Icon(Icons.Rounded.Star, "已收藏", Modifier.size(16.dp), tint = hex(0xFACC15))
            if (!tool.available) Icon(Icons.Rounded.Construction, "即将推出", Modifier.size(16.dp), tint = palette.secondaryText)
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(tool.label, style = Fonts.headline, maxLines = 1)
            SecondaryText(if (tool.available) tool.subtitle else "即将推出", style = Fonts.caption, maxLines = 1)
        }
    }
}

@Composable
private fun WideToolTile(tool: ToolKind) {
    val navigator = LocalNavigator.current
    val palette = LocalPalette.current
    Row(
        Modifier
            .alpha(if (tool.available) 1f else 0.55f)
            .toolClick(tool) { navigator.open(Routes.tool(tool)) }
            .glass(RoundedCornerShape(28.dp), tint = tool.colors[0].copy(alpha = 0.08f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ToolIcon(tool.icon, tool.colors, 48.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tool.label, style = Fonts.headline)
                if (tool.id in AppSettings.favoriteTools) Icon(Icons.Rounded.Star, null, Modifier.size(14.dp), tint = hex(0xFACC15))
            }
            SecondaryText(if (tool.available) tool.subtitle else "即将推出", style = Fonts.caption)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = palette.tertiaryText)
    }
}

@Composable
private fun FavoritePill(tool: ToolKind) {
    val navigator = LocalNavigator.current
    Row(
        Modifier
            .toolClick(tool) { navigator.open(Routes.tool(tool)) }
            .glass(CircleShape)
            .padding(start = 8.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ToolIcon(tool.icon, tool.colors, 36.dp)
        Text(tool.label, style = Fonts.subheadline.weight(FontWeight.SemiBold))
    }
}

/** 记一笔决策历史时用的小工具 */
@Composable
fun rememberToggle(initial: Boolean = false) = remember { mutableStateOf(initial) }

@Composable
internal fun CenterBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier, contentAlignment = Alignment.Center) { content() }
}
