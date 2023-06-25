package com.kotlin.mvvm.contact.view.main

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.kotlin.mvvm.contact.R


@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    lateinit var tvSkip: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        MobileAds.initialize(this) {}
        tvSkip = findViewById(R.id.tvSkip)
        AppOpenAd.load(
            this@SplashActivity,
            this@SplashActivity.getString(R.string.admob_app_open_ad_unit_id),
            AdRequest.Builder().build(),
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                /**
                 * Called when an app open ad has loaded.
                 *
                 * @param appOpenAd the loaded app open ad.
                 */
                override fun onAdLoaded(appOpenAd: AppOpenAd) {
                    appOpenAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        /** Called when full screen content is dismissed. */
                        override fun onAdDismissedFullScreenContent() {
                            startMainActivity()
                        }

                        /** Called when fullscreen content failed to show. */
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        }

                        /** Called when fullscreen content is shown. */
                        override fun onAdShowedFullScreenContent() {
                        }
                    }
                    appOpenAd.show(this@SplashActivity)
                }

                /**
                 * Called when an app open ad has failed to load.
                 *
                 * @param loadAdError the error.
                 */
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                }
            }
        )
        tvSkip.setOnClickListener {
            startMainActivity()
        }
    }

    /** Start the MainActivity. */
    fun startMainActivity() {
        val intent = Intent(this, ContactActivity::class.java)
        startActivity(intent)
    }
}