package com.creditdb.pro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = modalBottomSheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = currentLang == com.creditdb.pro.ui.theme.AppLanguage.SYSTEM,
                    onClick = { com.creditdb.pro.ui.theme.LanguageManager.setLanguage(com.creditdb.pro.ui.theme.AppLanguage.SYSTEM) },
                    label = { Text(if (isEn) "System Default" else "端末設定依存") }
                )
                FilterChip(
                    selected = currentLang == com.creditdb.pro.ui.theme.AppLanguage.JAPANESE,
                    onClick = { com.creditdb.pro.ui.theme.LanguageManager.setLanguage(com.creditdb.pro.ui.theme.AppLanguage.JAPANESE) },
                    label = { Text("日本語 (JP)") }
                )
                FilterChip(
                    selected = currentLang == com.creditdb.pro.ui.theme.AppLanguage.ENGLISH,
                    onClick = { com.creditdb.pro.ui.theme.LanguageManager.setLanguage(com.creditdb.pro.ui.theme.AppLanguage.ENGLISH) },
                    label = { Text("English (EN)") }
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
