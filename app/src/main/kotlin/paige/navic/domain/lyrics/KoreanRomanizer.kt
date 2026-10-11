/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package paige.navic.domain.lyrics

/**
 * Romanizes Korean Hangul text using the Revised Romanization of Korean.
 *
 * Precomposed Hangul syllables (U+AC00..U+D7A3) are decomposed arithmetically
 * into choseong (initial), jungseong (medial) and jongseong (final) jamo, which
 * are then mapped to Latin per the official tables. Sound-assimilation rules
 * between syllables are intentionally not applied; per-syllable substitution
 * keeps the output predictable and aligned with the original lyrics.
 *
 * Characters outside the Hangul ranges pass through unchanged.
 */
object KoreanRomanizer {

	private const val SBASE = 0xAC00
	private const val LCOUNT = 19
	private const val VCOUNT = 21
	private const val TCOUNT = 28

	/** choseong (initial consonants) */
	private val INITIALS = arrayOf(
		"g", "kk", "n", "d", "tt", "r", "m", "b", "pp",
		"s", "ss", "", "j", "jj", "ch", "k", "t", "p", "h"
	)

	/** jungseong (vowels) */
	private val MEDIALS = arrayOf(
		"a", "ae", "ya", "yae", "eo", "e", "yeo", "ye",
		"o", "wa", "wae", "oe", "yo", "u", "wo", "we",
		"wi", "yu", "eu", "ui", "i"
	)

	/** jongseong (final consonants), index 0 = none */
	private val FINALS = arrayOf(
		"", "k", "k", "k", "n", "n", "n", "t",
		"l", "k", "m", "l", "l", "l", "l", "p",
		"m", "p", "p", "t", "t", "ng", "t", "t",
		"k", "t", "p", "t"
	)

	/** Compatibility jamo (U+3131..U+3163) fallback mapping. */
	private val COMPAT_JAMO = mapOf(
		'ㄱ' to "g", 'ㄲ' to "kk", 'ㄴ' to "n", 'ㄷ' to "d", 'ㄸ' to "tt",
		'ㄹ' to "r", 'ㅁ' to "m", 'ㅂ' to "b", 'ㅃ' to "pp",
		'ㅅ' to "s", 'ㅆ' to "ss", 'ㅇ' to "ng", 'ㅈ' to "j", 'ㅉ' to "jj",
		'ㅊ' to "ch", 'ㅋ' to "k", 'ㅌ' to "t", 'ㅍ' to "p", 'ㅎ' to "h",
		'ㅏ' to "a", 'ㅐ' to "ae", 'ㅑ' to "ya", 'ㅒ' to "yae", 'ㅓ' to "eo",
		'ㅔ' to "e", 'ㅕ' to "yeo", 'ㅖ' to "ye", 'ㅗ' to "o", 'ㅘ' to "wa",
		'ㅙ' to "wae", 'ㅚ' to "oe", 'ㅛ' to "yo", 'ㅜ' to "u", 'ㅝ' to "wo",
		'ㅞ' to "we", 'ㅟ' to "wi", 'ㅠ' to "yu", 'ㅡ' to "eu", 'ㅢ' to "ui",
		'ㅣ' to "i"
	)

	fun isHangul(c: Char): Boolean =
		c in '가'..'힣' || c in 'ᄀ'..'ᇿ' || c in 'ㄱ'..'ㆎ'

	fun romanize(text: String): String {
		val out = StringBuilder(text.length * 2)
		for (c in text) {
			when {
				c in '가'..'힣' -> {
					val sIndex = c.code - SBASE
					val tIndex = sIndex % TCOUNT
					val vIndex = (sIndex / TCOUNT) % VCOUNT
					val lIndex = sIndex / (TCOUNT * VCOUNT)
					out.append(INITIALS[lIndex])
					out.append(MEDIALS[vIndex])
					out.append(FINALS[tIndex])
				}
				c in 'ㄱ'..'ㆎ' -> out.append(COMPAT_JAMO[c] ?: c)
				else -> out.append(c)
			}
		}
		return out.toString()
	}
}
