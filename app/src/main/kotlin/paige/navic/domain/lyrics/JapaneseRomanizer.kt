/*
 * Navic, a Subsonic music streaming app for Android
 * Copyright (c) 2026 paige
 * SPDX-License-Identifier: GPL-3.0-only
 */

package paige.navic.domain.lyrics

import com.atilika.kuromoji.ipadic.Tokenizer

/**
 * Romanizes Japanese text to romaji (Hepburn, without macrons).
 *
 * Kanji readings are resolved with Kuromoji (morphological analysis, IPADIC
 * dictionary bundled with the app); kana is converted deterministically.
 * Characters that are neither kana nor kanji pass through unchanged.
 *
 * The Kuromoji tokenizer loads a ~13 MB dictionary, so it is initialized
 * lazily on a background thread. Until initialization completes,
 * [romanize] falls back to converting kana only and leaves kanji untouched.
 */
object JapaneseRomanizer {

	@Volatile
	private var tokenizer: Tokenizer? = null
	private val lock = Any()

	fun isReady(): Boolean = tokenizer != null

	/** Loads the Kuromoji dictionary. Blocking — call from a background thread. */
	fun ensureInitialized() {
		if (tokenizer == null) {
			synchronized(lock) {
				if (tokenizer == null) {
					tokenizer = Tokenizer()
				}
			}
		}
	}

	fun isJapanese(c: Char): Boolean =
		c in 'ぁ'..'ゟ' || c in 'ァ'..'ヿ' ||
			c in 'ｦ'..'ﾟ' || // halfwidth katakana
			c in '一'..'鿿' || c in '㐀'..'䶿' // CJK unified + extension A

	fun romanize(text: String): String {
		val t = tokenizer
		if (t == null) return KanaRomanizer.romanizeKanaOnly(text)

		val out = StringBuilder(text.length * 2)
		for (token in t.tokenize(text)) {
			val surface = token.surface
			val reading = token.reading
			if (reading != null && reading != "*") {
				out.append(KanaRomanizer.katakanaToRomaji(reading))
			} else {
				// Unknown token (proper nouns, symbols, ...): convert kana
				// directly, leave kanji as-is.
				out.append(KanaRomanizer.romanizeKanaOnly(surface))
			}
		}
		return out.toString()
	}
}

/**
 * Deterministic kana -> romaji conversion (Hepburn, ASCII only).
 *
 * Long vowels are written doubled (おう -> "ou") rather than with macrons,
 * matching the convention of most published romaji lyrics.
 */
internal object KanaRomanizer {

	private val BASIC = mapOf(
		"あ" to "a", "い" to "i", "う" to "u", "え" to "e", "お" to "o",
		"か" to "ka", "き" to "ki", "く" to "ku", "け" to "ke", "こ" to "ko",
		"さ" to "sa", "し" to "shi", "す" to "su", "せ" to "se", "そ" to "so",
		"た" to "ta", "ち" to "chi", "つ" to "tsu", "て" to "te", "と" to "to",
		"な" to "na", "に" to "ni", "ぬ" to "nu", "ね" to "ne", "の" to "no",
		"は" to "ha", "ひ" to "hi", "ふ" to "fu", "へ" to "he", "ほ" to "ho",
		"ま" to "ma", "み" to "mi", "む" to "mu", "め" to "me", "も" to "mo",
		"や" to "ya", "ゆ" to "yu", "よ" to "yo",
		"ら" to "ra", "り" to "ri", "る" to "ru", "れ" to "re", "ろ" to "ro",
		"わ" to "wa", "を" to "wo", "ん" to "n",
		"が" to "ga", "ぎ" to "gi", "ぐ" to "gu", "げ" to "ge", "ご" to "go",
		"ざ" to "za", "じ" to "ji", "ず" to "zu", "ぜ" to "ze", "ぞ" to "zo",
		"だ" to "da", "ぢ" to "ji", "づ" to "zu", "で" to "de", "ど" to "do",
		"ば" to "ba", "び" to "bi", "ぶ" to "bu", "べ" to "be", "ぼ" to "bo",
		"ぱ" to "pa", "ぴ" to "pi", "ぷ" to "pu", "ぺ" to "pe", "ぽ" to "po",
		"ゔ" to "vu",
		// digraphs
		"きゃ" to "kya", "きゅ" to "kyu", "きょ" to "kyo",
		"しゃ" to "sha", "しゅ" to "shu", "しょ" to "sho",
		"ちゃ" to "cha", "ちゅ" to "chu", "ちょ" to "cho",
		"にゃ" to "nya", "にゅ" to "nyu", "にょ" to "nyo",
		"ひゃ" to "hya", "ひゅ" to "hyu", "ひょ" to "hyo",
		"みゃ" to "mya", "みゅ" to "myu", "みょ" to "myo",
		"りゃ" to "rya", "りゅ" to "ryu", "りょ" to "ryo",
		"ぎゃ" to "gya", "ぎゅ" to "gyu", "ぎょ" to "gyo",
		"じゃ" to "ja", "じゅ" to "ju", "じょ" to "jo",
		"びゃ" to "bya", "びゅ" to "byu", "びょ" to "byo",
		"ぴゃ" to "pya", "ぴゅ" to "pyu", "ぴょ" to "pyo",
		"ぢゃ" to "ja", "ぢゅ" to "ju", "ぢょ" to "jo",
		"うゃ" to "wya",
		// contracted sounds with small vowels
		"ふぁ" to "fa", "ふぃ" to "fi", "ふぇ" to "fe", "ふぉ" to "fo", "ふゅ" to "fyu",
		"てぃ" to "ti", "でぃ" to "di", "とぅ" to "tu",
		"ちぇ" to "che", "しぇ" to "she", "じぇ" to "je",
		"うぃ" to "wi", "うぇ" to "we", "うぉ" to "wo",
		"ゔぁ" to "va", "ゔぃ" to "vi", "ゔぇ" to "ve", "ゔぉ" to "vo",
		"つぁ" to "tsa", "つぃ" to "tsi", "つぇ" to "tse", "つぉ" to "tso",
		"くぁ" to "kwa", "ぐぁ" to "gwa"
	)

	private val PUNCTUATION = mapOf(
		'。' to ".", '、' to ",", '「' to "\"", '」' to "\"",
		'『' to "\"", '』' to "\"", '・' to " ", '〜' to "~",
		'～' to "~", '…' to "...", '！' to "!", '？' to "?",
		'　' to " "
	)

	private val VOWELS = setOf('a', 'i', 'u', 'e', 'o')

	/** Normalizes katakana (incl. halfwidth) to hiragana for table lookup. */
	private fun toHiragana(c: Char): Char = when {
		c in 'ァ'..'ヶ' -> (c.code - 0x60).toChar()
		c == 'ヵ' -> 'か'
		// ヶ is usually read "ka" (counter / place names)
		c == 'ヶ' -> 'か'
		c in 'ｦ'..'ﾟ' -> when (c) {
			'ｦ' -> 'を'
			'ｧ' -> 'ぁ'; 'ｨ' -> 'ぃ'; 'ｩ' -> 'ぅ'; 'ｪ' -> 'ぇ'; 'ｫ' -> 'ぉ'
			'ｬ' -> 'ゃ'; 'ｭ' -> 'ゅ'; 'ｮ' -> 'ょ'; 'ｯ' -> 'っ'
			'ｰ' -> 'ー'
			// ｱ..ﾝ map linearly onto ア..ン
			else -> ((c.code - 0xFF71 + 0x30A2).toChar().code - 0x60).toChar()
		}
		else -> c
	}

	/** Converts a katakana reading (as returned by Kuromoji) to romaji. */
	fun katakanaToRomaji(katakana: String): String =
		romanizeKanaOnly(katakana)

	/**
	 * Converts kana in [text] to romaji; kanji and other characters are
	 * left untouched.
	 */
	fun romanizeKanaOnly(text: String): String {
		val kana = text.map { toHiragana(it) }
		val out = StringBuilder(text.length * 2)
		var i = 0
		var lastVowel = ' '
		while (i < kana.size) {
			val c = kana[i]
			when {
				// digraph: base + small ゃゅょ / small vowels
				i + 1 < kana.size && BASIC.containsKey("${c}${kana[i + 1]}") -> {
					val romaji = BASIC["${c}${kana[i + 1]}"]!!
					out.append(romaji)
					lastVowel = romaji.last()
					i += 2
				}
				c == 'っ' -> {
					// sokuon: geminate the following consonant
					if (i + 1 < kana.size) {
						val next = when {
							i + 2 < kana.size && BASIC.containsKey("${kana[i + 1]}${kana[i + 2]}") ->
								BASIC["${kana[i + 1]}${kana[i + 2]}"]!!
							else -> BASIC[kana[i + 1].toString()]
						}
						if (next != null) {
							out.append(if (next.startsWith("ch")) "t" else next.first().toString())
						}
					}
					i++
				}
				c == 'ー' -> {
					if (lastVowel != ' ') out.append(lastVowel)
					i++
				}
				c == 'ん' -> {
					val nextRomaji = if (i + 1 < kana.size) {
						when {
							i + 2 < kana.size && BASIC.containsKey("${kana[i + 1]}${kana[i + 2]}") ->
								BASIC["${kana[i + 1]}${kana[i + 2]}"]!!
							else -> BASIC[kana[i + 1].toString()] ?: ""
						}
					} else ""
					out.append(
						when {
							nextRomaji.firstOrNull() in listOf('b', 'm', 'p') -> "m"
							nextRomaji.firstOrNull() in VOWELS || nextRomaji.startsWith("y") -> "n'"
							else -> "n"
						}
					)
					lastVowel = ' '
					i++
				}
				BASIC.containsKey(c.toString()) -> {
					val romaji = BASIC[c.toString()]!!
					out.append(romaji)
					lastVowel = romaji.last()
					i++
				}
				PUNCTUATION.containsKey(c) -> {
					out.append(PUNCTUATION[c])
					lastVowel = ' '
					i++
				}
				else -> {
					out.append(text[i])
					lastVowel = ' '
					i++
				}
			}
		}
		return out.toString()
	}
}
