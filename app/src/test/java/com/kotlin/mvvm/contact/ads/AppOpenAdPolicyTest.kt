package com.kotlin.mvvm.contact.ads

import com.kotlin.mvvm.contact.ads.AppOpenAdPolicy.MIN_INTERVAL_MS
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppOpenAdPolicyTest {

    private val now = 1_800_000_000_000L

    @Test
    fun neverOnTheFirstLaunch() {
        assertFalse(AppOpenAdPolicy.shouldShow(firstOpenAt = null, lastShownAt = null, now = now))
    }

    @Test
    fun showsOnALaterLaunchWhenNoneShownYet() {
        assertTrue(AppOpenAdPolicy.shouldShow(firstOpenAt = now - 1_000, lastShownAt = null, now = now))
    }

    @Test
    fun waitsTheMinimumIntervalBetweenAds() {
        val first = now - 10 * MIN_INTERVAL_MS
        assertFalse(AppOpenAdPolicy.shouldShow(first, lastShownAt = now - MIN_INTERVAL_MS + 1, now = now))
        assertTrue(AppOpenAdPolicy.shouldShow(first, lastShownAt = now - MIN_INTERVAL_MS, now = now))
    }

    @Test
    fun aClockMovedBackwardsDoesNotBlockAdsForGood() {
        assertTrue(AppOpenAdPolicy.shouldShow(now - 1_000, lastShownAt = now + MIN_INTERVAL_MS, now = now))
    }
}
