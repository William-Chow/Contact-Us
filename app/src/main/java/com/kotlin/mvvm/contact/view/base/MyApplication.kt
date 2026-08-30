package com.kotlin.mvvm.contact.view.base

import android.app.Application
import com.kotlin.mvvm.contact.ads.ConsentManager

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Returning users whose consent is already on file get the Ads SDK started right away;
        // for everyone else this is a no-op until SplashActivity has gathered consent.
        ConsentManager.initializeAdsIfAllowed(this)
    }
}
