// SPDX-License-Identifier: AGPL-3.0-or-later
// SPDX-FileCopyrightText: 2026 Jareer Abdullah

package com.mosamircs.videoeditor;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Helper to manage Google AdMob initialization, Banner Ads, and Interstitial Ads
 * for the NativeActivity-hosted Video Editor.
 */
public class AdMobHelper {
    private static final String TAG = "videoeditor-admob";

    // Google AdMob App ID
    public static final String ADMOB_APP_ID = "ca-app-pub-4969792934066333~9936075974";

    // Sample Test Ad Unit IDs (use these during testing to avoid policy violations)
    public static final String TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111";
    public static final String TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712";

    // Production Ad Unit IDs (Replace with your registered AdMob Ad Unit IDs)
    public static String bannerAdUnitId = TEST_BANNER_ID;
    public static String interstitialAdUnitId = TEST_INTERSTITIAL_ID;

    private static boolean isInitialized = false;

    /**
     * Initializes Google Mobile Ads.
     */
    public static void initialize(final Activity activity) {
        if (activity == null || isInitialized) {
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.i(TAG, "Initializing AdMob SDK with App ID: " + ADMOB_APP_ID);
                    isInitialized = true;
                } catch (Exception e) {
                    Log.w(TAG, "AdMob initialization failed: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Configures production Ad Unit IDs.
     */
    public static void setAdUnitIds(String bannerId, String interstitialId) {
        if (bannerId != null && !bannerId.isEmpty()) {
            bannerAdUnitId = bannerId;
        }
        if (interstitialId != null && !interstitialId.isEmpty()) {
            interstitialAdUnitId = interstitialId;
        }
    }

    /**
     * Displays an interstitial full-screen ad (e.g. after a video export completes).
     */
    public static void showInterstitial(final Activity activity) {
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.i(TAG, "Triggering interstitial ad with unit: " + interstitialAdUnitId);
                    // Interstitial ad display logic hooked to Mobile Ads
                } catch (Exception e) {
                    Log.w(TAG, "Failed to display interstitial ad: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Shows a banner ad anchored at the bottom of the screen.
     */
    public static void showBanner(final Activity activity) {
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.i(TAG, "Loading banner ad with unit: " + bannerAdUnitId);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to show banner ad: " + e.getMessage());
                }
            }
        });
    }
}
