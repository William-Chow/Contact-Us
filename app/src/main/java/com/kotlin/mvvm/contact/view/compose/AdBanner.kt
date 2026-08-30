package com.kotlin.mvvm.contact.view.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.kotlin.mvvm.contact.R
import com.kotlin.mvvm.contact.ads.ConsentManager

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Nothing may be requested until UMP says consent allows it.
    if (!ConsentManager.canRequestAds(context)) return

    val adView = remember {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = context.getString(R.string.admob_banner_ad_unit_id)
            loadAd(AdRequest.Builder().build())
        }
    }

    // Release the native AdView when the composable leaves composition.
    DisposableEffect(Unit) {
        onDispose { adView.destroy() }
    }

    AndroidView(modifier = modifier, factory = { adView })
}
