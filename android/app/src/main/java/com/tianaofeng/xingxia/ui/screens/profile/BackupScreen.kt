package com.tianaofeng.xingxia.ui.screens.profile

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tianaofeng.xingxia.core.DateText
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.data.AppSettings
import com.tianaofeng.xingxia.data.Backup
import com.tianaofeng.xingxia.data.BackupCategory
import com.tianaofeng.xingxia.data.BackupException
import com.tianaofeng.xingxia.data.BackupFile
import com.tianaofeng.xingxia.data.BackupMode
import com.tianaofeng.xingxia.data.BackupRestoreResult
import com.tianaofeng.xingxia.ui.components.BigActionButton
import com.tianaofeng.xingxia.ui.components.Flow
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GlassButton
import com.tianaofeng.xingxia.ui.components.GlassCard
import com.tianaofeng.xingxia.ui.components.GlowIcon
import com.tianaofeng.xingxia.ui.components.GradientIcon
import com.tianaofeng.xingxia.ui.components.Haptics
import com.tianaofeng.xingxia.ui.components.Screen
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.SectionHeader
import com.tianaofeng.xingxia.ui.components.TagChip
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.Instant

/** 数据备份：导出为 JSON 文件、从文件恢复（与 iOS 版互通，规则见 data/Backup.kt） */
@Composable
fun BackupScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<BackupFile?>(null) }
    var confirmReplace by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Pair<String, String>?>(null) }

    // 选好位置后再生成内容：选文件期间页面被系统重建也不会丢
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            val error = runCatching {
                val text = Backup.export()
                withContext(Dispatchers.IO) { write(context, uri, text.toByteArray(Charsets.UTF_8)) }
            }.exceptionOrNull()
            busy = false
            if (error == null) {
                AppSettings.backupLastExportAt = System.currentTimeMillis()
                Haptics.success()
                view.announceForAccessibility("备份已导出")
            } else {
                Haptics.error()
                message = "导出失败" to ((error as? BackupException)?.message ?: BackupException.exportFailed.message.orEmpty())
            }
        }
    }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            val result = runCatching {
                val bytes = withContext(Dispatchers.IO) { read(context, uri) }
                withContext(Dispatchers.Default) { Backup.parse(bytes) }
            }
            busy = false
            result.onSuccess {
                pending = it
                Haptics.selection()
                view.announceForAccessibility("已读取备份文件，请选择合并或全部替换")
            }.onFailure { error ->
                Haptics.error()
                message = if (error is BackupException) "无法恢复" to error.message.orEmpty()
                else "打不开这个文件" to "没能读取这个文件，请换一个文件或稍后再试。"
            }
        }
    }

    fun restore(mode: BackupMode) {
        val file = pending ?: return
        runCatching { Backup.restore(file, mode) }
            .onSuccess { result ->
                pending = null
                Haptics.success()
                message = "恢复完成" to summary(result, mode)
            }
            .onFailure { error ->
                Haptics.error()
                message = "恢复失败" to "本机数据没有改动。${error.message.orEmpty()}"
            }
    }

    Screen(title = "数据备份") {
        BackupIntro()
        ExportCard(enabled = !busy) { exporter.launch(Backup.defaultFileName()) }
        GlassCard(spacing = 14.dp) {
            SectionHeader("从文件恢复", icon = Icons.Rounded.Download)
            val file = pending
            if (file == null) {
                SecondaryText("选择星匣导出的备份文件（iPhone 或 Android 导出的都可以），会先列出文件里有哪些数据，再决定合并还是替换。")
                GlassButton("选择备份文件", Icons.Rounded.Folder, Modifier.fillMaxWidth(), enabled = !busy) {
                    // 有的文件管理器把 .json 标成 octet-stream，一并放行，内容由 Backup.parse 把关
                    importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                }
            } else {
                BackupPreview(
                    file,
                    onMerge = { restore(BackupMode.MERGE) },
                    onReplace = { confirmReplace = true },
                    onCancel = { pending = null },
                )
            }
        }
        SecondaryText(
            "备份文件是明文 JSON，含生日等个人信息，请存在自己的网盘或电脑里，不要随意转发。",
            style = Fonts.caption2,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (confirmReplace) {
        AlertDialog(
            onDismissRequest = { confirmReplace = false },
            confirmButton = {
                TextButton(onClick = {
                    confirmReplace = false
                    restore(BackupMode.REPLACE)
                }) { Text("全部替换", color = LocalPalette.current.negative) }
            },
            dismissButton = { TextButton(onClick = { confirmReplace = false }) { Text("取消") } },
            title = { Text("用备份替换本机数据？") },
            text = { Text("本机对应的档案、记录、倒数日等会先被删除，再换成备份里的数据，无法撤销。不放心的话，先导出一份当前数据。") },
        )
    }
    message?.let { (title, text) ->
        AlertDialog(
            onDismissRequest = { message = null },
            confirmButton = { TextButton(onClick = { message = null }) { Text("好") } },
            title = { Text(title) },
            text = { Text(text) },
        )
    }
}

@Composable
private fun BackupIntro() {
    val palette = LocalPalette.current
    GlassCard(spacing = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GlowIcon(Icons.Rounded.Save, palette.accent, palette.accentSecondary, 52.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("把数据存成一个文件", style = Fonts.title3.weight(FontWeight.Bold))
                SecondaryText("星匣的数据只保存在这台设备上，定期导出一份更安心。")
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BackupTip(Icons.Rounded.Sync, "卸载重装", "卸载 App 或清除应用数据会删掉本机的记录，之前先导出一份，出了问题也能找回。")
            BackupTip(Icons.Rounded.Smartphone, "换手机", "在旧手机上导出，把文件传到新手机，再用「从文件恢复」导入。")
            BackupTip(Icons.Rounded.SwapHoriz, "iPhone 和 Android 互通", "两个版本用同一种备份格式，可以互相导入。")
        }
    }
}

@Composable
private fun BackupTip(icon: ImageVector, title: String, text: String) {
    Row(Modifier.semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GradientIcon(icon, size = 20.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = Fonts.subheadline.weight(FontWeight.SemiBold))
            SecondaryText(text, style = Fonts.footnote)
        }
    }
}

@Composable
private fun ExportCard(enabled: Boolean, onExport: () -> Unit) {
    val palette = LocalPalette.current
    val last = AppSettings.backupLastExportAt
    val days = if (last > 0) DayKey.of(Instant.ofEpochMilli(last)).daysTo(DayKey.today) else null
    // 没导出过，或者离上次导出已满 7 天
    val stale = (days ?: 7) >= 7
    val status = if (last <= 0 || days == null) "还没有导出过备份" else {
        val instant = Instant.ofEpochMilli(last)
        val time = "${DateText.full(DayKey.of(instant))} ${DateText.time(instant)}"
        if (days >= 7) "上次导出：$time，已经 $days 天了" else "上次导出：$time"
    }
    GlassCard(spacing = 14.dp) {
        SectionHeader("导出备份", icon = Icons.Rounded.Upload)
        val tone = if (stale) palette.caution else palette.positive
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(if (stale) Icons.Rounded.Warning else Icons.Rounded.CheckCircle, null, Modifier.size(18.dp), tint = tone)
            Text(status, style = Fonts.subheadline, color = tone, modifier = Modifier.weight(1f))
        }
        val present = BackupCategory.entries.map { it to it.localCount }.filter { it.second > 0 }
        if (present.isNotEmpty()) {
            val spoken = "本机数据：" + present.joinToString("，") { (c, n) -> "${c.label} $n ${c.unit}" }
            Column(Modifier.clearAndSetSemantics { contentDescription = spoken }) {
                Flow(spacing = 6.dp) { for ((c, n) in present) TagChip("${c.label} $n ${c.unit}") }
            }
        }
        SecondaryText(
            "包括档案、决策记录、签与塔罗、心情、倒数日、转盘、菜单、抽奖池等全部数据，以及首页模块和快捷工具收藏；主题、音效等设置不在其中。",
            style = Fonts.footnote,
        )
        BigActionButton("导出备份", Icons.Rounded.Upload, enabled = enabled, onClick = onExport)
    }
}

@Composable
private fun BackupPreview(file: BackupFile, onMerge: () -> Unit, onReplace: () -> Unit, onCancel: () -> Unit) {
    val palette = LocalPalette.current
    Column(Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("来自 ${file.platformName} 的备份", style = Fonts.headline)
        file.exportedAt?.let {
            val instant = Instant.ofEpochMilli(it)
            SecondaryText("${DateText.full(DayKey.of(instant))} ${DateText.time(instant)} 导出")
        }
    }
    val present = BackupCategory.entries.mapNotNull { c -> file.count(c)?.takeIf { it > 0 }?.let { c to it } }
    if (present.isEmpty()) {
        SecondaryText("这份备份里没有数据。")
    } else {
        val spoken = "备份里有：" + present.joinToString("，") { (c, n) -> "${c.label} $n ${c.unit}" }
        Column(Modifier.clearAndSetSemantics { contentDescription = spoken }) {
            Flow(spacing = 6.dp) { for ((c, n) in present) TagChip("${c.label} $n ${c.unit}", palette.accentSecondary) }
        }
    }
    if (file.includesSettings) SecondaryText("还包括首页模块和快捷工具收藏。", style = Fonts.footnote)
    SecondaryText("合并：保留本机数据，只添加备份里没有的。\n全部替换：本机对应的数据先删掉，再换成备份里的。", style = Fonts.footnote)
    val usable = file.totalCount > 0 || file.includesSettings
    BigActionButton("合并到本机", Icons.Rounded.AddCircle, enabled = usable, onClick = onMerge)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassButton("全部替换", Icons.Rounded.Sync, Modifier.weight(1f), enabled = usable, onClick = onReplace)
        GlassButton("取消", modifier = Modifier.weight(1f), onClick = onCancel)
    }
}

private fun summary(result: BackupRestoreResult, mode: BackupMode): String = when (mode) {
    BackupMode.MERGE ->
        if (result.totalAdded == 0) "本机已经包含备份里的全部内容，没有需要添加的。"
        else "新增 ${result.totalAdded} 项数据" + (if (result.skipped > 0) "，跳过 ${result.skipped} 项已有或重复的" else "") + "。"
    BackupMode.REPLACE ->
        "已换成备份里的数据，共恢复 ${result.totalAdded} 项" + (if (result.skipped > 0) "（${result.skipped} 项重复或不完整，已跳过）" else "") + "。"
}

/** 读文件；超过上限多读一个字节就停，交给 Backup.parse 报「文件太大」 */
private fun read(context: Context, uri: Uri): ByteArray {
    val limit = 64 * 1024 * 1024 + 1
    val input = context.contentResolver.openInputStream(uri) ?: throw BackupException.unreadable
    return input.use { stream ->
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        while (out.size() < limit) {
            val n = stream.read(buffer, 0, minOf(buffer.size, limit - out.size()))
            if (n < 0) break
            out.write(buffer, 0, n)
        }
        out.toByteArray()
    }
}

private fun write(context: Context, uri: Uri, bytes: ByteArray) {
    // "wt"：覆盖已有文件时先截断，避免旧内容残留在末尾
    val output = context.contentResolver.openOutputStream(uri, "wt") ?: throw BackupException.exportFailed
    output.use { it.write(bytes) }
}
