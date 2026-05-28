package com.kotlin.mvvm.contact.view.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.kotlin.mvvm.contact.R

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = {
            AdView(it).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = context.getString(R.string.admob_banner_ad_unit_id)
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
