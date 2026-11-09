package com.kotlin.mvvm.contact.model

/**
 * How a name is shown. Chinese, Japanese and Korean names put the family name first with no space
 * (陈 + 伟明 → 陈伟明); other names are "first last".
 */
object ContactNames {

    fun displayName(firstName: String, lastName: String): String {
        val first = firstName.trim()
        val last = lastName.trim()
        return when {
            first.isEmpty() -> last
            last.isEmpty() -> first
            isEastAsian(first) || isEastAsian(last) -> last + first
            else -> "$first $last"
        }
    }

    /** One family-name character for East Asian names (陈), otherwise first + last initials (GH). */
    fun initials(firstName: String, lastName: String): String {
        val first = firstName.trim()
        val last = lastName.trim()
        if (isEastAsian(first) || isEastAsian(last)) {
            return firstCharacter(last.ifEmpty { first })
        }
        return firstCharacter(first).uppercase() + firstCharacter(last).uppercase()
    }

    fun Contact.displayName(): String = displayName(firstName, lastName)

    private fun isEastAsian(text: String): Boolean =
        text.codePoints().anyMatch { Character.UnicodeScript.of(it) in EAST_ASIAN }

    // By code point, so a character outside the BMP isn't cut in half.
    private fun firstCharacter(text: String): String =
        if (text.isEmpty()) "" else String(Character.toChars(text.codePointAt(0)))

    private val EAST_ASIAN = setOf(
        Character.UnicodeScript.HAN,
        Character.UnicodeScript.HIRAGANA,
        Character.UnicodeScript.KATAKANA,
        Character.UnicodeScript.HANGUL
    )
}
