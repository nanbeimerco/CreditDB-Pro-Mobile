package com.creditdb.pro

import com.creditdb.pro.utils.TextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {

    @Test
    fun testNormalization_katakanaToHiragana() {
        assertEquals("ふりーれん", TextNormalizer.normalize("フリーレン"))
        assertEquals("あんのひであき", TextNormalizer.normalize("アンノヒデアキ"))
    }

    @Test
    fun testNormalization_zenkakuToHankaku() {
        assertEquals("cowboybebop", TextNormalizer.normalize("Ｃｏｗｂｏｙ　Ｂｅｂｏｐ"))
        assertEquals("tier1", TextNormalizer.normalize("Ｔｉｅｒ　１"))
    }

    @Test
    fun testNormalization_removePunctuationAndWhitespaces() {
        assertEquals("あしたのじょー2", TextNormalizer.normalize("あしたのジョー ２"))
        assertEquals("新世紀えぶぁんげりおん", TextNormalizer.normalize("新世紀エヴァンゲリオン"))
    }

    @Test
    fun testNormalization_emptyOrNull() {
        assertEquals("", TextNormalizer.normalize(null))
        assertEquals("", TextNormalizer.normalize("   "))
    }

    @Test
    fun testNormalization_staffNames() {
        assertEquals("梶", TextNormalizer.normalize("梶"))
        assertEquals("梶裕貴", TextNormalizer.normalize("梶 裕貴"))
        assertEquals("かじゆうき", TextNormalizer.normalize("カジ ユウキ"))
    }
}

