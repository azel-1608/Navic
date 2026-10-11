/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package paige.navic.domain.lyrics

/**
 * Romanizes lyrics lines to the Latin alphabet.
 *
 * Script detection is automatic per line:
 * - lines containing Hangul are romanized with [KoreanRomanizer]
 *   (Revised Romanization of Korean),
 * - lines containing hiragana, katakana or kanji are romanized with
 *   [JapaneseRomanizer] (Hepburn romaji),
 * - anything else is returned unchanged.
 *
 * Lines mixing both scripts are run through the Japanese pipeline first
 * (Kuromoji passes Hangul through untouched) and then the Korean one.
 */
object Romanizer {

	fun containsRomanizableScript(text: String): Boolean =
		text.any { KoreanRomanizer.isHangul(it) || JapaneseRomanizer.isJapanese(it) }

	fun romanizeLine(text: String): String {
		if (!containsRomanizableScript(text)) return text
		var out = text
		if (out.any { JapaneseRomanizer.isJapanese(it) }) {
			out = JapaneseRomanizer.romanize(out)
		}
		if (out.any { KoreanRomanizer.isHangul(it) }) {
			out = KoreanRomanizer.romanize(out)
		}
		return out
	}
}
