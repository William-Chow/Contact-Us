package com.kotlin.mvvm.contact.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * UMP (GDPR/EEA) gate in front of the Ads SDK.
 *
 * Ads must not be requested before the user's consent state is known, so MobileAds is initialised
 * from here instead of unconditionally in Application.onCreate.
 */
object ConsentManager {

    private val adsInitialized = AtomicBoolean(false)

    fun canRequestAds(context: Context): Boolean =
        UserMessagingPlatform.getConsentInformation(context).canRequestAds()

    /** True only where the user must be able to reopen the consent form themselves. */
    fun isPrivacyOptionsRequired(context: Context): Boolean =
        UserMessagingPlatform.getConsentInformation(context).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /**
     * Refreshes the consent state, showing the consent form when one is required.
     *
     * [onFormMayShow] fires once the network round trip is done and a form may be put in front of
     * the user; callers use it to drop any splash timeout, because past that point the flow is
     * paced by the user rather than by us. [onReady] reports whether ads may now be requested.
     */
    fun gather(
        activity: Activity,
        onFormMayShow: () -> Unit,
        onReady: (canRequestAds: Boolean) -> Unit
    ) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                onFormMayShow()
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    // A form error still leaves a usable stored state, so canRequestAds() decides.
                    settle(activity, consentInformation, onReady)
                }
            },
            {
                // Consent service unreachable: fall back to whatever state is already stored.
                settle(activity, consentInformation, onReady)
            }
        )
    }

    /**
     * Reopens the consent form from the app's own privacy entry point. [onDismissed] receives the
     * error message when the form could not be shown, and null when it closed normally.
     */
    fun showPrivacyOptions(activity: Activity, onDismissed: (errorMessage: String?) -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            onDismissed(formError?.message)
        }
    }

    /** Safe on every cold start: a no-op until consent is actually on file. */
    fun initializeAdsIfAllowed(context: Context) {
        if (canRequestAds(context)) initializeAds(context)
    }

    private fun settle(
        context: Context,
        consentInformation: ConsentInformation,
        onReady: (Boolean) -> Unit
    ) {
        val allowed = consentInformation.canRequestAds()
        if (allowed) initializeAds(context)
        onReady(allowed)
    }

    private fun initializeAds(context: Context) {
        if (!adsInitialized.compareAndSet(false, true)) return
        MobileAds.initialize(context.applicationContext) {}
    }
}
