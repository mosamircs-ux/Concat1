package com.mosamircs.videoeditor;

import android.app.NativeActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Play Store host activity for the native Rust/Slint Video Editor (libconcat_android.so).
 * Subclasses NativeActivity so the full Slint GPU editor runs identically while adding
 * real Google UMP consent + Google Mobile Ads SDK (AdMob Banner + post-export Interstitial).
 */
public class VideoEditorActivity extends NativeActivity {
    private static final String TAG = "VideoEditorAdMob";

    private final AtomicBoolean isMobileAdsInitializeCalled = new AtomicBoolean(false);
    private ConsentInformation consentInformation;
    private InterstitialAd interstitialAd;
    private AdView bannerAdView;
    private PopupWindow bannerPopup;
    private boolean isDestroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestConsentAndInitAds();
    }

    private void requestConsentAndInitAds() {
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        consentInformation = UserMessagingPlatform.getConsentInformation(this);
        consentInformation.requestConsentInfoUpdate(
                this,
                params,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                        this,
                        formError -> {
                            if (formError != null) {
                                Log.w(TAG, "UMP consent form notice: " + formError.getMessage());
                            }
                            if (consentInformation.canRequestAds()) {
                                initializeMobileAdsSdk();
                            }
                        }
                ),
                requestError -> {
                    Log.w(TAG, "UMP consent update warning: " + requestError.getMessage());
                    if (consentInformation.canRequestAds()) {
                        initializeMobileAdsSdk();
                    }
                }
        );

        // Also initialize immediately if consent was already gathered in a previous session.
        if (consentInformation.canRequestAds()) {
            initializeMobileAdsSdk();
        }
    }

    private void initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return;
        }
        Log.i(TAG, "Initializing Google Mobile Ads SDK");
        MobileAds.initialize(this, initializationStatus -> {
            Log.i(TAG, "Google Mobile Ads SDK initialized");
            runOnUiThread(() -> {
                if (!isDestroyed) {
                    loadInterstitialAd();
                    showBannerOverlay();
                }
            });
        });
    }

    private void loadInterstitialAd() {
        if (isDestroyed) {
            return;
        }
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(
                this,
                BuildConfig.ADMOB_INTERSTITIAL_ID,
                adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        Log.i(TAG, "Interstitial ad loaded");
                        interstitialAd = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.w(TAG, "Interstitial ad failed to load: " + loadAdError.getMessage());
                        interstitialAd = null;
                    }
                }
        );
    }

    private void showBannerOverlay() {
        if (isDestroyed || bannerPopup != null) {
            return;
        }
        getWindow().getDecorView().post(() -> {
            if (isDestroyed || isFinishing() || bannerPopup != null) {
                return;
            }
            try {
                bannerAdView = new AdView(this);
                bannerAdView.setAdUnitId(BuildConfig.ADMOB_BANNER_ID);
                bannerAdView.setAdSize(AdSize.BANNER);

                LinearLayout container = new LinearLayout(this);
                container.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
                container.addView(bannerAdView);

                bannerPopup = new PopupWindow(
                        container,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        false
                );
                bannerPopup.setClippingEnabled(false);
                bannerPopup.showAtLocation(
                        getWindow().getDecorView(),
                        Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                        0,
                        0
                );
                bannerAdView.loadAd(new AdRequest.Builder().build());
            } catch (Throwable t) {
                Log.w(TAG, "Banner overlay skipped: " + t.getMessage());
            }
        });
    }

    /**
     * Invoked via reflection by ConcatFiles.publishVideo() after a user finishes exporting a video.
     */
    public void onVideoExported() {
        runOnUiThread(() -> {
            if (isDestroyed || isFinishing()) {
                return;
            }
            if (interstitialAd != null) {
                interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        interstitialAd = null;
                        loadInterstitialAd();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        Log.w(TAG, "Interstitial failed to show: " + adError.getMessage());
                        interstitialAd = null;
                        loadInterstitialAd();
                    }
                });
                interstitialAd.show(VideoEditorActivity.this);
            } else {
                loadInterstitialAd();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bannerAdView != null) {
            bannerAdView.resume();
        }
    }

    @Override
    protected void onPause() {
        if (bannerAdView != null) {
            bannerAdView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        isDestroyed = true;
        if (bannerPopup != null) {
            try {
                bannerPopup.dismiss();
            } catch (Throwable ignored) {
            }
            bannerPopup = null;
        }
        if (bannerAdView != null) {
            bannerAdView.destroy();
            bannerAdView = null;
        }
        super.onDestroy();
    }
}
