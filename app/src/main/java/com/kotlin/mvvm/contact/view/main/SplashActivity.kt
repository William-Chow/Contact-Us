package com.kotlin.mvvm.contact.view.main

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.kotlin.mvvm.contact.R
import com.kotlin.mvvm.contact.ads.ConsentManager
import com.kotlin.mvvm.contact.view.compose.ContactUsTheme
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    // Skip, the consent callbacks, the ad callbacks and the timeout can all fire; only the first
    // one may navigate.
    private val navigated = AtomicBoolean(false)
    private val phaseTimeout = Runnable { startMainActivity() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ContactUsTheme {
                SplashScreen(onSkipClick = { startMainActivity() })
            }
        }

        // Guards the consent network round trip only. It is dropped before any form can appear,
        // because from that point the user sets the pace and must not be navigated away from.
        handler.postDelayed(phaseTimeout, CONSENT_TIMEOUT_MS)
        ConsentManager.gather(
            activity = this,
            onFormMayShow = { handler.removeCallbacks(phaseTimeout) },
            onReady = onReady@{ canRequestAds ->
                handler.removeCallbacks(phaseTimeout)
                if (navigated.get() || isFinishing || isDestroyed) return@onReady
                if (canRequestAds) {
                    handler.postDelayed(phaseTimeout, AD_TIMEOUT_MS)
                    loadAppOpenAd()
                } else {
                    // Consent withheld: no ad to wait for, so go straight in.
                    startMainActivity()
                }
            }
        )
    }

    override fun onDestroy() {
        handler.removeCallbacks(phaseTimeout)
        super.onDestroy()
    }

    private fun loadAppOpenAd() {
        AppOpenAd.load(
            this,
            getString(R.string.admob_app_open_ad_unit_id),
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(appOpenAd: AppOpenAd) {
                    // The user may have skipped ahead while the ad was still loading.
                    if (navigated.get() || isFinishing || isDestroyed) return
                    handler.removeCallbacks(phaseTimeout)

                    appOpenAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            startMainActivity()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            startMainActivity()
                        }
                    }
                    appOpenAd.show(this@SplashActivity)
                }

                // No ad (offline, no fill, error): carry on into the app instead of doing nothing.
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    startMainActivity()
                }
            }
        )
    }

    private fun startMainActivity() {
        if (!navigated.compareAndSet(false, true)) return
        handler.removeCallbacks(phaseTimeout)
        startActivity(Intent(this, ContactActivity::class.java))
        finish()
    }

    private companion object {
        const val CONSENT_TIMEOUT_MS = 8_000L
        const val AD_TIMEOUT_MS = 5_000L
    }
}

@Composable
private fun SplashScreen(onSkipClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(120.dp)
            )
            Text(
                text = stringResource(R.string.splash_activity_text),
                modifier = Modifier.padding(top = 20.dp),
                color = Color.Black,
                fontSize = 13.sp
            )
        }
        TextButton(
            onClick = onSkipClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth()
                .padding(end = 10.dp, bottom = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.skip),
                color = Color.Black,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
