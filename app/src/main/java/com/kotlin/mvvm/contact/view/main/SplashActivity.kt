package com.kotlin.mvvm.contact.view.main

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
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
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.kotlin.mvvm.contact.R
import com.kotlin.mvvm.contact.view.compose.ContactUsTheme

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MobileAds.initialize(this) {}
        loadAppOpenAd()

        setContent {
            ContactUsTheme {
                SplashScreen(onSkipClick = { startMainActivity() })
            }
        }
    }

    private fun loadAppOpenAd() {
        AppOpenAd.load(
            this,
            getString(R.string.admob_app_open_ad_unit_id),
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(appOpenAd: AppOpenAd) {
                    appOpenAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            startMainActivity()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) = Unit
                    }
                    appOpenAd.show(this@SplashActivity)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) = Unit
            }
        )
    }

    private fun startMainActivity() {
        startActivity(Intent(this, ContactActivity::class.java))
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
