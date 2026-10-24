package com.creditdb.pro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditdb.pro.ui.glass.GlassSettings
import com.creditdb.pro.ui.glass.isGlassSupported
import com.creditdb.pro.ui.theme.ColorPresetSpec
import com.creditdb.pro.ui.theme.ColorPresets
import com.creditdb.pro.ui.theme.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePaletteBottomSheet(
    onDismissRequest: () -> Unit
) {
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentPreset = ThemeManager.currentPreset
    val isEn = com.creditdb.pro.ui.theme.LanguageManager.isEnglish
    val currentLang = com.creditdb.pro.ui.theme.LanguageManager.currentLanguage
    val liquidGlassEnabled by GlassSettings.liquidGlass.collectAsState()
    val reduceDynamicBlur by GlassSettings.reduceDynamicBlur.collectAsState()
    val glassSupported = isGlassSupported()

    com.creditdb.pro.ui.glass.GlassModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = modalBottomSheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 言語設定セクション
            Text(
                text = if (isEn) "Language Settings" else "言語設定 (Language)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            SingleChoiceButtonGroup(
                options = listOf(
                    com.creditdb.pro.ui.theme.AppLanguage.SYSTEM,
                    com.creditdb.pro.ui.theme.AppLanguage.JAPANESE,
                    com.creditdb.pro.ui.theme.AppLanguage.ENGLISH
                ),
                selected = currentLang,
                onSelect = { com.creditdb.pro.ui.theme.LanguageManager.setLanguage(it) },
                label = {
                    when (it) {
                        com.creditdb.pro.ui.theme.AppLanguage.SYSTEM -> if (isEn) "System" else "端末設定"
                        com.creditdb.pro.ui.theme.AppLanguage.JAPANESE -> "日本語"
                        com.creditdb.pro.ui.theme.AppLanguage.ENGLISH -> "English"
                    }
                },
                scrollable = false
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Liquid Glass 表示設定セクション
            Text(
                text = if (isEn) "Display & Glass Effects" else "表示・ガラス効果設定",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // スイッチ 1: Liquid Glass ON/OFF
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = glassSupported) {
                        GlassSettings.setLiquidGlass(!liquidGlassEnabled)
                    }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Liquid Glass",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (glassSupported) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (glassSupported) {
                            if (isEn) "Real-time optical refraction & blur for floating bars and sheets"
                            else "フローティングバーやシートにリアルタイム屈折・ブラーを適用します"
                        } else {
                            if (isEn) "Available on Android 12+"
                            else "Android 12 以上で利用可"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = liquidGlassEnabled && glassSupported,
                    onCheckedChange = { GlassSettings.setLiquidGlass(it) },
                    enabled = glassSupported
                )
            }

            // スイッチ 2: 透明度を下げる (Reduce Transparency)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        GlassSettings.setReduceDynamicBlur(!reduceDynamicBlur)
                    }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = if (isEn) "Reduce Transparency" else "透明度を下げる",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isEn) {
                            "Suppresses dynamic blur & refraction for higher contrast and lower GPU load"
                        } else {
                            "動的ブラーと屈折を抑え、視認性と省電力性を高めた不透明サーフェスにします"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = reduceDynamicBlur,
                    onCheckedChange = { GlassSettings.setReduceDynamicBlur(it) }
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Text(
                text = if (isEn) "Color Theme Presets" else "カラーテーマ・プリセット",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (isEn) {
                    "Select presets based on Material 3 color harmonies (Primary/Secondary/Tertiary). Tier grading colors remain consistent."
                } else {
                    "Material 3のカラー原則（Primary/Secondary/Tertiary）に基づいた上質なプリセットを選択できます。Tier格付けの色分けは維持されます。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            ColorPresets.ALL_PRESETS.forEach { preset ->
                val isSelected = preset.id == currentPreset.id
                PresetCard(
                    preset = preset,
                    isSelected = isSelected,
                    onClick = {
                        ThemeManager.selectPreset(preset)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PresetCard(
    preset: ColorPresetSpec,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isEn = com.creditdb.pro.ui.theme.LanguageManager.isEnglish
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 1.5.dp else 0.8.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isSelected) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (isEn) "Active" else "適用中",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Text(
                    text = preset.getLocalizedDescription(isEn),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // カラーパレットプレビュードット (Primary, Secondary, Tertiary)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColorDot(color = preset.primary)
                ColorDot(color = preset.secondary)
                ColorDot(color = preset.tertiary)
            }
        }
    }
}

@Composable
private fun ColorDot(color: Color) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(color)
            .border(0.8.dp, Color.White.copy(alpha = 0.2f), CircleShape)
    )
}
