package com.tianaofeng.xingxia.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tianaofeng.xingxia.data.AppSettings
import com.tianaofeng.xingxia.ui.nav.LocalNavigator
import com.tianaofeng.xingxia.ui.theme.LocalPalette

/** 平板、折叠屏、横屏下正文的最大宽度（与 iOS 的 readableWidth 一致） */
val ReadableWidth = 640.dp

/**
 * 当前页面可以收藏到首页「快捷工具」时的收藏 id，由路由提供（与 iOS 的 favoriteToggle 一样统一在路由处加）。
 * 页面主界面（没有自定义返回的 Screen）的顶栏右侧会显示收藏星标；页面内的编辑器等子界面不显示。
 */
val LocalFavoriteId = staticCompositionLocalOf<String?> { null }

/** 悬浮底栏占的高度（不含系统导航栏），由 AppNavigation 量出来提供；还没量到时是 0 */
val LocalTabBarHeight = compositionLocalOf { 0.dp }

/** 根页面最底部要留的空白：最后一张卡片能完整滚到悬浮底栏上方，大字号下底栏变高也够 */
@Composable
fun rootBottomPadding(): Dp = maxOf(96.dp, LocalTabBarHeight.current + 20.dp)

/**
 * 页面骨架：透明顶栏（返回 + 标题 + 操作）+ 可滚动内容。
 * 背景星空由根布局统一绘制。正文限宽居中；不滚动的页面也避让底部导航栏。
 */
@Composable
fun Screen(
    title: String,
    isRoot: Boolean = false,
    scrollable: Boolean = true,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    spacing: Dp = 16.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    /** 需要在代码里滚动（比如滚回顶部）时传入；默认由页面自己记住 */
    scrollState: ScrollState? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val navigator = LocalNavigator.current
    val back = onBack ?: { navigator.back() }
    if (onBack != null) BackHandler { onBack() }
    val ownScroll = rememberScrollState()
    val favoriteId = if (isRoot || onBack != null) null else LocalFavoriteId.current
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
    ) {
        TopBar(title, isRoot, back, favoriteId, actions)
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter,
        ) {
            val body = Modifier
                .widthIn(max = ReadableWidth)
                .fillMaxWidth()
                .fillMaxHeight()
                .then(if (scrollable) Modifier.verticalScroll(scrollState ?: ownScroll) else Modifier.navigationBarsPadding())
                .padding(horizontal = 16.dp)
            Column(body, verticalArrangement = Arrangement.spacedBy(spacing), horizontalAlignment = horizontalAlignment) {
                content()
                if (scrollable) {
                    // 根页面底部要给悬浮底栏留出空间
                    Spacer(Modifier.height(if (isRoot) rootBottomPadding() else 24.dp))
                    Spacer(Modifier.navigationBarsPadding())
                }
            }
        }
    }
}

@Composable
private fun TopBar(title: String, isRoot: Boolean, onBack: () -> Unit, favoriteId: String?, actions: @Composable RowScope.() -> Unit) {
    val palette = LocalPalette.current
    if (isRoot) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = Fonts.largeTitle, color = palette.text, modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
        }
    } else {
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 12.dp),
        ) {
            CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "返回", Modifier.align(Alignment.CenterStart), onClick = onBack)
            Text(
                title,
                style = Fonts.headline,
                color = palette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 96.dp),
            )
            Row(
                Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                actions()
                if (favoriteId != null) FavoriteToggleButton(favoriteId)
            }
        }
    }
}

/** 收藏到首页「快捷工具」/ 取消收藏的星标按钮（工具页顶栏；答案之书传 "answers"） */
@Composable
fun FavoriteToggleButton(id: String, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val favorite = id in AppSettings.favoriteTools
    Box(
        modifier
            .size(40.dp)
            .semantics { stateDescription = if (favorite) "已收藏" else "未收藏" }
            .pressable {
                AppSettings.toggleFavorite(id)
                Haptics.selection()
            }
            .glass(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            if (favorite) "取消收藏" else "收藏到首页",
            Modifier.size(20.dp),
            // 星标金色：深色用亮金，浅色用加深的金色
            tint = if (favorite) palette.gold else palette.text,
        )
    }
}

/** 圆形玻璃图标按钮 */
@Composable
fun CircleIconButton(icon: ImageVector, description: String?, modifier: Modifier = Modifier, size: Dp = 40.dp, enabled: Boolean = true, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Box(
        modifier
            .size(size)
            .pressable(enabled, onClick)
            .glass(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, Modifier.size(size * 0.5f), tint = if (enabled) palette.text else palette.tertiaryText)
    }
}

/** 文字形式的顶栏按钮 */
@Composable
fun TextAction(title: String, enabled: Boolean = true, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Box(
        Modifier
            .pressable(enabled, onClick)
            .glass(CircleShape)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(title, style = Fonts.subheadline, color = if (enabled) palette.accent else palette.tertiaryText)
    }
}

@Composable
fun HSpacer(width: Dp) = Spacer(Modifier.width(width))
