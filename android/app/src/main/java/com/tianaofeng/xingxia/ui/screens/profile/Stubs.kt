package com.tianaofeng.xingxia.ui.screens.profile

import androidx.compose.runtime.Composable
import com.tianaofeng.xingxia.ui.components.Screen
import com.tianaofeng.xingxia.ui.components.SecondaryText

// 临时占位，逐个替换为正式页面

@Composable
fun BirthChartScreen() {
    Screen(title = "BirthChartScreen", isRoot = false) { SecondaryText("即将推出") }
}

@Composable
fun CompatibilityScreen() {
    Screen(title = "CompatibilityScreen", isRoot = false) { SecondaryText("即将推出") }
}

@Composable
fun MeScreen() {
    Screen(title = "MeScreen", isRoot = true) { SecondaryText("即将推出") }
}

@Composable
fun ProfileEditorScreen(profileId: String?, isOnboarding: Boolean) {
    Screen(title = "ProfileEditorScreen", isRoot = false) { SecondaryText("即将推出") }
}

@Composable
fun ProfileListScreen() {
    Screen(title = "ProfileListScreen", isRoot = false) { SecondaryText("即将推出") }
}

@Composable
fun SettingsScreen() {
    Screen(title = "SettingsScreen", isRoot = false) { SecondaryText("即将推出") }
}

@Composable
fun ThemeSettingsScreen() {
    Screen(title = "ThemeSettingsScreen", isRoot = false) { SecondaryText("即将推出") }
}
