package com.creditdb.pro.utils

import java.text.Normalizer
import java.util.regex.Pattern

object TextNormalizer {
    private val PUNCTUATION_PATTERN = Pattern.compile("[\\s\\-_・:：,，.．!！?？/／★☆♪〜~・()（）「」『』\\[\\]【】]")

    /**
     * 日本語文字列を検索用に正規化する
     * 1. 全角英数字記号 -> 半角
     * 2. カタカナ -> ひらがな
     * 3. ゐゑゔ等の変体仮名 -> いえぶ
     * 4. 空白・記号の除去
     * 5. 小文字化
     */
    fun normalize(text: String?): String {
        if (text.isNullOrBlank()) return ""
        
        // 全角英数を半角化 (NFKC)
        var s = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFKC)
        
        // カタカナをひらがなに変換
        val sb = java.lang.StringBuilder(s.length)
        for (ch in s) {
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
        
        // 記号・空白の除去
        return PUNCTUATION_PATTERN.matcher(sb.toString()).replaceAll("")
    }
}
