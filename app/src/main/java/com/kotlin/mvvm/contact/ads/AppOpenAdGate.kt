package com.kotlin.mvvm.contact.ads

import android.content.Context

/** When the splash may show an app-open ad. Pure, so it is unit-tested with plain timestamps. */
object AppOpenAdPolicy {

    /** At most one app-open ad in this window, so returning users aren't hit on every launch. */
    const val MIN_INTERVAL_MS = 4 * 60 * 60 * 1000L

    /**
     * Never on the very first launch ([firstOpenAt] null): a new user should meet the app before
     * an ad. After that, only when [MIN_INTERVAL_MS] has passed since the last one was shown. A
     * clock moved backwards past [lastShownAt] counts as elapsed rather than blocking ads for good.
     */
    fun shouldShow(firstOpenAt: Long?, lastShownAt: Long?, now: Long): Boolean {
        if (firstOpenAt == null) return false
        if (lastShownAt == null) return true
        return now - lastShownAt >= MIN_INTERVAL_MS || now < lastShownAt
    }
}

/** Remembers the first launch and the last app-open ad, and applies [AppOpenAdPolicy]. */
class AppOpenAdGate(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("app_open_ads", Context.MODE_PRIVATE)

    /** Records the first launch the first time it is asked, so that launch never shows an ad. */
    fun shouldShowNow(now: Long = System.currentTimeMillis()): Boolean {
        val firstOpenAt = if (prefs.contains(KEY_FIRST_OPEN)) prefs.getLong(KEY_FIRST_OPEN, now) else null
        if (firstOpenAt == null) prefs.edit().putLong(KEY_FIRST_OPEN, now).apply()
        val lastShownAt = if (prefs.contains(KEY_LAST_SHOWN)) prefs.getLong(KEY_LAST_SHOWN, now) else null
        return AppOpenAdPolicy.shouldShow(firstOpenAt, lastShownAt, now)
    }

    fun recordShown(now: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_SHOWN, now).apply()
    }

    private companion object {
        const val KEY_FIRST_OPEN = "first_open_at"
        const val KEY_LAST_SHOWN = "last_shown_at"
    }
}
