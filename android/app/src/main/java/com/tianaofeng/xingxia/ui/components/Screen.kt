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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tianaofeng.xingxia.ui.nav.LocalNavigator
import com.tianaofeng.xingxia.ui.theme.LocalPalette

/** 平板、折叠屏、横屏下正文的最大宽度（与 iOS 的 readableWidth 一致） */
val ReadableWidth = 640.dp

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
    content: @Composable ColumnScope.() -> Unit,
) {
    val navigator = LocalNavigator.current
    val back = onBack ?: { navigator.back() }
    if (onBack != null) BackHandler { onBack() }
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
    ) {
        TopBar(title, isRoot, back, actions)
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
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier.navigationBarsPadding())
                .padding(horizontal = 16.dp)
            Column(body, verticalArrangement = Arrangement.spacedBy(spacing), horizontalAlignment = horizontalAlignment) {
                content()
                if (scrollable) {
                    // 根页面底部要给悬浮底栏留出空间
                    Spacer(Modifier.height(if (isRoot) 96.dp else 24.dp))
                    Spacer(Modifier.navigationBarsPadding())
                }
            }
        }
    }
}

@Composable
private fun TopBar(title: String, isRoot: Boolean, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit) {
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
                content = actions,
            )
        }
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
