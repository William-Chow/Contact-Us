package com.kotlin.mvvm.contact.view.base

import android.app.Application
import com.google.android.gms.ads.MobileAds

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize the Ads SDK once for the whole app.
        MobileAds.initialize(this) {}
    }
}
