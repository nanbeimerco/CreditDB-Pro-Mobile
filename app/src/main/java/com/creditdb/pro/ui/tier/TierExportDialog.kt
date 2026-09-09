package com.creditdb.pro.ui.tier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditdb.pro.ui.theme.AppStrings
import com.creditdb.pro.ui.theme.LanguageManager

data class TierImageExportOptions(
    val scale: Float = 1.6f, // 1.0f: 1080p, 1.6f: 2K, 2.5f: 4K
    val showTitle: Boolean = true,
    val showYear: Boolean = true,
    val showDeviationScore: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TierExportDialog(
    onDismiss: () -> Unit,
    onExportImage: (TierImageExportOptions) -> Unit,
    onCopyImage: (TierImageExportOptions) -> Unit,
    onCopyCleanText: () -> Unit,
    onCopyMarkdown: () -> Unit,
    onShareMarkdown: () -> Unit
) {
    val isEn = LanguageManager.isEnglish

    var selectedResolution by remember { mutableStateOf("2K") }
    var showTitle by remember { mutableStateOf(true) }
    var showYear by remember { mutableStateOf(true) }
    var showDevScore by remember { mutableStateOf(false) }

    val currentOptions = remember(selectedResolution, showTitle, showYear, showDevScore) {
        val scale = when (selectedResolution) {
            "1080p" -> 1.0f
            "4K" -> 2.5f
            else -> 1.6f
        }
        TierImageExportOptions(
            scale = scale,
            showTitle = showTitle,
            showYear = showYear,
            showDeviationScore = showDevScore
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = AppStrings.tierExport(isEn),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. 画像エクスポート設定カード
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = if (isEn) "Image Export Options" else "画像出力・表示設定",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 解像度選択
                        Text(
                            text = if (isEn) "Resolution (Quality)" else "出力解像度",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("1080p" to "FHD", "2K" to "2K QHD", "4K" to "4K UHD").forEach { (resKey, label) ->
                                val isSelected = selectedResolution == resKey
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedResolution = resKey },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // カード表示項目チェックボックス
                        Text(
                            text = if (isEn) "Card Contents" else "作品カード内の表示項目",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTitle = !showTitle }
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(checked = showTitle, onCheckedChange = { showTitle = it })
                            Text(if (isEn) "Show Title" else "作品名を表示", style = MaterialTheme.typography.bodySmall)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showYear = !showYear }
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(checked = showYear, onCheckedChange = { showYear = it })
                            Text(if (isEn) "Show Release Year" else "放送年を表示", style = MaterialTheme.typography.bodySmall)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDevScore = !showDevScore }
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(checked = showDevScore, onCheckedChange = { showDevScore = it })
                            Column {
                                Text(if (isEn) "Show Deviation Score" else "偏差値を表示", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = if (isEn) "CreditDB metric (OFF recommended for external sharing)" else "CreditDB独自指標（SNS等外部共有時はOFF推奨）",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // 2. アクションカード一覧
                ExportOptionCard(
                    icon = Icons.Default.Image,
                    title = AppStrings.tierExportImage(isEn),
                    desc = if (isEn) "Save high-resolution PNG to Gallery" else "設定した解像度で端末のギャラリーに保存します",
                    onClick = {
                        onDismiss()
                        onExportImage(currentOptions)
                    }
                )

                ExportOptionCard(
                    icon = Icons.Default.ContentCopy,
                    title = if (isEn) "Copy Image to Clipboard" else "画像をクリップボードにコピー",
                    desc = if (isEn) "Copy image to paste directly into Discord, X, etc." else "画像を直接DiscordやTwitter等に貼り付けできます",
                    onClick = {
                        onDismiss()
                        onCopyImage(currentOptions)
                    }
                )

                ExportOptionCard(
                    icon = Icons.Default.ContentCopy,
                    title = if (isEn) "Copy Tier List (Text)" else "Tier表テキストをコピー",
                    desc = if (isEn) "Clean text list for SNS / chat (no deviation score)" else "偏差値などの内部指標を含まないシンプルな共有用テキスト",
                    onClick = {
                        onDismiss()
                        onCopyCleanText()
                    }
                )

                ExportOptionCard(
                    icon = Icons.Default.Share,
                    title = AppStrings.tierExportMarkdownCopy(isEn),
                    desc = if (isEn) "Full statistical data & prompt for ChatGPT / Claude / Gemini" else "数理定義・全スタッフデータ・AIプロンプト付き詳細Markdown",
                    onClick = {
                        onDismiss()
                        onCopyMarkdown()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isEn) "Close" else "閉じる")
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    )
}

@Composable
private fun ExportOptionCard(
    icon: ImageVector,
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                )
            }
        }
    }
}

