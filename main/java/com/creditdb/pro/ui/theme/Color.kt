package com.creditdb.pro.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Material 3 共通ニュートラル階層 (ダークモード基調)
 * 文字・背景・サーフェス・枠線の視認性と落ち着きを最優先に設計
 */
val DarkBackground = Color(0xFF111318)
val DarkOnBackground = Color(0xFFE2E2E6)

val DarkSurface = Color(0xFF111318)
val DarkOnSurface = Color(0xFFE2E2E6)

val DarkSurfaceVariant = Color(0xFF44474E)
val DarkOnSurfaceVariant = Color(0xFFC4C7C5)

val DarkSurfaceContainer = Color(0xFF1D2024)
val DarkSurfaceContainerHigh = Color(0xFF282A2F)

val DarkOutline = Color(0xFF8E918F)
val DarkOutlineVariant = Color(0xFF444746)

// ライトテーマ用ニュートラル
val LightBackground = Color(0xFFF8FAFC)
val LightOnBackground = Color(0xFF191C20)
val LightSurface = Color(0xFFFAF9FD)
val LightOnSurface = Color(0xFF191C20)
val LightSurfaceVariant = Color(0xFFE0E2EC)
val LightOnSurfaceVariant = Color(0xFF44474F)
val LightSurfaceContainer = Color(0xFFF0F0F4)
val LightSurfaceContainerHigh = Color(0xFFE8E8EC)
val LightOutline = Color(0xFF74777F)
val LightOutlineVariant = Color(0xFFC4C7D0)

/**
 * 判定バッジ (M3コンテナに調和するトーン)
 */
val VerdictSurprise = Color(0xFF82D9A7)    // サプライズ名作 (落ち着いたエメラルド)
val VerdictNormal = Color(0xFF94A3B8)      // 概ねスタッフ前評判通り (ニュートラルスレート)
val VerdictUnderperform = Color(0xFFF28B82)// 期待外れ (落ち着いたソフトローズ)

/**
 * Material 3 カラープリセット仕様
 */
data class ColorPresetSpec(
    val id: String,
    val name: String,
    val description: String,
    val descriptionEn: String = "",
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val previewDot: Color
) {
    fun getLocalizedDescription(isEn: Boolean = LanguageManager.isEnglish): String {
        return if (isEn && descriptionEn.isNotBlank()) descriptionEn else description
    }
}

object ColorPresets {
    val TITANIUM_SLATE = ColorPresetSpec(
        id = "titanium_slate",
        name = "Titanium Slate",
        description = "知的でプロフェッショナルなチタンスレート (標準)",
        descriptionEn = "Intellectual and professional titanium slate (Default)",
        primary = Color(0xFFA8C7FA),
        onPrimary = Color(0xFF003062),
        primaryContainer = Color(0xFF00468A),
        onPrimaryContainer = Color(0xFFD6E3FF),
        secondary = Color(0xFFBEC6DC),
        onSecondary = Color(0xFF283141),
        secondaryContainer = Color(0xFF3E4759),
        onSecondaryContainer = Color(0xFFDAE2F9),
        tertiary = Color(0xFFEFB8C8),
        onTertiary = Color(0xFF492532),
        tertiaryContainer = Color(0xFF633B48),
        onTertiaryContainer = Color(0xFFFFD8E4),
        previewDot = Color(0xFFA8C7FA)
    )

    val MATERIAL_LAVENDER = ColorPresetSpec(
        id = "material_lavender",
        name = "Material Lavender",
        description = "Google Material 3 公式ベースラインラベンダー",
        descriptionEn = "Official Google Material 3 baseline lavender",
        primary = Color(0xFFD0BCFF),
        onPrimary = Color(0xFF381E72),
        primaryContainer = Color(0xFF4F378B),
        onPrimaryContainer = Color(0xFFEADDFF),
        secondary = Color(0xFFCCC2DC),
        onSecondary = Color(0xFF332D41),
        secondaryContainer = Color(0xFF4A4458),
        onSecondaryContainer = Color(0xFFE8DEF8),
        tertiary = Color(0xFFEFB8C8),
        onTertiary = Color(0xFF492532),
        tertiaryContainer = Color(0xFF633B48),
        onTertiaryContainer = Color(0xFFFFD8E4),
        previewDot = Color(0xFFD0BCFF)
    )

    val NORDIC_EMERALD = ColorPresetSpec(
        id = "nordic_emerald",
        name = "Nordic Emerald",
        description = "落ち着きと品位のある北欧フォレストグリーン",
        descriptionEn = "Calm and refined Nordic forest green",
        primary = Color(0xFF82D9A7),
        onPrimary = Color(0xFF003822),
        primaryContainer = Color(0xFF005234),
        onPrimaryContainer = Color(0xFFA0F5C2),
        secondary = Color(0xFFB4CCBE),
        onSecondary = Color(0xFF20352A),
        secondaryContainer = Color(0xFF364B3F),
        onSecondaryContainer = Color(0xFFD0E8D9),
        tertiary = Color(0xFFD3C7A8),
        onTertiary = Color(0xFF38301B),
        tertiaryContainer = Color(0xFF4F462F),
        onTertiaryContainer = Color(0xFFF0E3C3),
        previewDot = Color(0xFF82D9A7)
    )

    val AMBER_BRONZE = ColorPresetSpec(
        id = "amber_bronze",
        name = "Amber Bronze",
        description = "映画的で温かみのあるクラシックアンバー",
        descriptionEn = "Cinematic and warm classic amber",
        primary = Color(0xFFF0C068),
        onPrimary = Color(0xFF422C00),
        primaryContainer = Color(0xFF5E4100),
        onPrimaryContainer = Color(0xFFFFDEA4),
        secondary = Color(0xFFD7C4A8),
        onSecondary = Color(0xFF3B2F1B),
        secondaryContainer = Color(0xFF52452F),
        onSecondaryContainer = Color(0xFFF4E0C3),
        tertiary = Color(0xFFDEC2B7),
        onTertiary = Color(0xFF402C26),
        tertiaryContainer = Color(0xFF58423B),
        onTertiaryContainer = Color(0xFFFBDBD0),
        previewDot = Color(0xFFF0C068)
    )

    val PURE_MONOCHROME = ColorPresetSpec(
        id = "pure_monochrome",
        name = "Pure Monochrome",
        description = "Tierの色味のみを最大限に際立たせる白黒ミニマル",
        descriptionEn = "Black & white minimalist highlighting Tier grade colors",
        primary = Color(0xFFE2E2E6),
        onPrimary = Color(0xFF1B1B1F),
        primaryContainer = Color(0xFF303034),
        onPrimaryContainer = Color(0xFFE2E2E6),
        secondary = Color(0xFFC4C7C5),
        onSecondary = Color(0xFF1B1B1F),
        secondaryContainer = Color(0xFF303034),
        onSecondaryContainer = Color(0xFFC4C7C5),
        tertiary = Color(0xFF909094),
        onTertiary = Color(0xFF1B1B1F),
        tertiaryContainer = Color(0xFF303034),
        onTertiaryContainer = Color(0xFFC4C7C5),
        previewDot = Color(0xFFE2E2E6)
    )

    val ALL_PRESETS = listOf(
        TITANIUM_SLATE,
        MATERIAL_LAVENDER,
        NORDIC_EMERALD,
        AMBER_BRONZE,
        PURE_MONOCHROME
    )

    fun getPresetById(id: String): ColorPresetSpec {
        return ALL_PRESETS.firstOrNull { it.id == id } ?: TITANIUM_SLATE
    }

    fun createDarkColorScheme(preset: ColorPresetSpec): ColorScheme {
        return darkColorScheme(
            primary = preset.primary,
            onPrimary = preset.onPrimary,
            primaryContainer = preset.primaryContainer,
            onPrimaryContainer = preset.onPrimaryContainer,
            secondary = preset.secondary,
            onSecondary = preset.onSecondary,
            secondaryContainer = preset.secondaryContainer,
            onSecondaryContainer = preset.onSecondaryContainer,
            tertiary = preset.tertiary,
            onTertiary = preset.onTertiary,
            tertiaryContainer = preset.tertiaryContainer,
            onTertiaryContainer = preset.onTertiaryContainer,
            background = DarkBackground,
            onBackground = DarkOnBackground,
            surface = DarkSurface,
            onSurface = DarkOnSurface,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = DarkOnSurfaceVariant,
            surfaceContainer = DarkSurfaceContainer,
            surfaceContainerHigh = DarkSurfaceContainerHigh,
            outline = DarkOutline,
            outlineVariant = DarkOutlineVariant
        )
    }

    fun createLightColorScheme(preset: ColorPresetSpec): ColorScheme {
        return lightColorScheme(
            primary = preset.primary,
            onPrimary = preset.onPrimary,
            primaryContainer = preset.primaryContainer,
            onPrimaryContainer = preset.onPrimaryContainer,
            secondary = preset.secondary,
            onSecondary = preset.onSecondary,
            secondaryContainer = preset.secondaryContainer,
            onSecondaryContainer = preset.onSecondaryContainer,
            tertiary = preset.tertiary,
            onTertiary = preset.onTertiary,
            tertiaryContainer = preset.tertiaryContainer,
            onTertiaryContainer = preset.onTertiaryContainer,
            background = LightBackground,
            onBackground = LightOnBackground,
            surface = LightSurface,
            onSurface = LightOnSurface,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = LightOnSurfaceVariant,
            surfaceContainer = LightSurfaceContainer,
            surfaceContainerHigh = LightSurfaceContainerHigh,
            outline = LightOutline,
            outlineVariant = LightOutlineVariant
        )
    }
}
