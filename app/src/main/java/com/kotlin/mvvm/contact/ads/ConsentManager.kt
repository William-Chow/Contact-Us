package com.kotlin.mvvm.contact.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * UMP (GDPR/EEA) gate in front of the Ads SDK.
 *
 * Ads must not be requested before the user's consent state is known, so MobileAds is initialised
 * from here instead of unconditionally in Application.onCreate.
 */
object ConsentManager {

    data class State(
        val canRequestAds: Boolean = false,
        /** True only where the user must be able to reopen the consent form themselves. */
        val privacyOptionsRequired: Boolean = false
    )

    private val adsInitialized = AtomicBoolean(false)

    private val _state = MutableStateFlow(State())

    /**
     * Observed by the UI instead of reading UMP once, so the banner and the privacy entry follow a
     * consent answer that only arrives after the screen was composed.
     */
    val state: StateFlow<State> = _state.asStateFlow()

    /**
     * Whether a gather has run to the end in this process. A screen that finds this false knows the
     * splash was left (skip, timeout) before consent settled, and gathers again itself.
     */
    var isSettled = false
        private set

    /**
     * Refreshes the consent state, showing the consent form when one is required.
     *
     * [onFormShowing] fires right before the form is put in front of the user; callers use it to
     * drop any splash timeout, because past that point the flow is paced by the user rather than by
     * us. [onReady] reports whether ads may now be requested. If [activity] has gone by the time
     * the form has loaded, neither fires and consent stays unsettled for the next screen to gather.
     */
    fun gather(
        activity: Activity,
        onFormShowing: () -> Unit = {},
        onReady: (canRequestAds: Boolean) -> Unit = {}
    ) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                if (consentInformation.isConsentFormAvailable &&
                    consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED
                ) {
                    loadAndShowForm(activity, onFormShowing, onReady)
                } else {
                    settle(activity, onReady)
                }
            },
            {
                // Consent service unreachable: fall back to whatever state is already stored.
                settle(activity, onReady)
            }
        )
    }

    /**
     * Reopens the consent form from the app's own privacy entry point. [onDismissed] receives the
     * error message when the form could not be shown, and null when it closed normally.
     */
    fun showPrivacyOptions(activity: Activity, onDismissed: (errorMessage: String?) -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            // The new answer can change both whether ads may run and whether this entry is needed.
            initializeAdsIfAllowed(activity)
            onDismissed(formError?.message)
        }
    }

    /**
     * Publishes the stored consent state to [state] and starts the Ads SDK if it allows ads. Safe
     * on every cold start: a no-op for ads until consent is actually on file.
     */
    fun initializeAdsIfAllowed(context: Context): Boolean {
        val consentInformation = UserMessagingPlatform.getConsentInformation(context)
        val current = State(
            canRequestAds = consentInformation.canRequestAds(),
            privacyOptionsRequired = consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        )
        _state.value = current
        if (current.canRequestAds) initializeAds(context)
        return current.canRequestAds
    }

    /**
     * Loads and shows the form here rather than through loadAndShowConsentFormIfRequired, so the
     * caller hears when the form actually goes up (the download is a second round trip that its
     * timeout must still cover), and so the form is never shown on an activity that has gone.
     */
    private fun loadAndShowForm(
        activity: Activity,
        onFormShowing: () -> Unit,
        onReady: (Boolean) -> Unit
    ) {
        UserMessagingPlatform.loadConsentForm(
            activity,
            { form ->
                // Skipped, timed out or recreated while the form loaded: whichever activity took
                // over gathers again, so this one neither shows the form nor settles.
                if (!activity.isFinishing && !activity.isDestroyed) {
                    onFormShowing()
                    // A form error still leaves a usable stored state, so canRequestAds() decides.
                    form.show(activity) { settle(activity, onReady) }
                }
            },
            {
                settle(activity, onReady)
            }
        )
    }

    private fun settle(context: Context, onReady: (Boolean) -> Unit) {
        isSettled = true
        onReady(initializeAdsIfAllowed(context))
    }

    private fun initializeAds(context: Context) {
        if (!adsInitialized.compareAndSet(false, true)) return
        MobileAds.initialize(context.applicationContext) {}
    }
}
