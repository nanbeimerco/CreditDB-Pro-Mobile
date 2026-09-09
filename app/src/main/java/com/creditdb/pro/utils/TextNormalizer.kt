package com.creditdb.pro.utils

import java.text.Normalizer
import java.util.regex.Pattern

object TextNormalizer {
    private val PUNCTUATION_PATTERN = Pattern.compile("[\\s\\-_・:：,，.．!！?？/／★☆♪〜~・()（）「」『』\\[\\]【】]")

    // 新旧漢字・異体字の代表新字体マッピング
    private val KANJI_VARIANTS = mapOf(
        '惡' to '悪', '櫻' to '桜', '鐵' to '鉄', '國' to '国', '龍' to '竜',
        '廣' to '広', '髙' to '高', '﨑' to '崎', '壽' to '寿', '體' to '体',
        '戰' to '戦', '畫' to '画', '號' to '号', '變' to '変', '戀' to '恋',
        '黑' to '黒', '蟲' to '虫', '擊' to '撃', '寫' to '写', '眞' to '真',
        '遙' to '遥', '條' to '条', '齊' to '斉', '齋' to '斉', '斎' to '斉',
        '藪' to '薮', '峰' to '峯', '嶋' to '島', '濱' to '浜', '濵' to '浜'
    )

    /**
     * 日本語文字列を検索用に正規化する
     * 1. 全角英数字記号 -> 半角
     * 2. カタカナ -> ひらがな
     * 3. ゐゑゔ等の変体仮名 -> いえぶ
     * 4. 新旧漢字・異体字 -> 代表新字体
     * 5. 空白・記号の除去
     * 6. 小文字化
     */
    fun normalize(text: String?): String {
        if (text.isNullOrBlank()) return ""
        
        // 全角英数を半角化 (NFKC)
        var s = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFKC)
        
        // カタカナをひらがなに変換 & 異体字変換
        val sb = java.lang.StringBuilder(s.length)
        for (ch in s) {
            val variant = KANJI_VARIANTS[ch]
            if (variant != null) {
                sb.append(variant)
            } else {
                when (ch) {
                    'ゐ', 'ヰ' -> sb.append('い')
                    'ゑ', 'ヱ' -> sb.append('え')
                    'ゔ', 'ヴ' -> sb.append('ぶ')
                    else -> {
                        val code = ch.code
                        if (code in 0x30A1..0x30F6) {
                            sb.append((code - 0x60).toChar())
                        } else {
                            sb.append(ch)
                        }
                    }
                }
            }
        }
        
        // 記号・空白の除去
        return PUNCTUATION_PATTERN.matcher(sb.toString()).replaceAll("")
    }
}
