/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide;

import android.app.Application;
import android.content.ContentProvider;
import android.os.Build;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.os.StrictMode.VmPolicy;
import com.battlelancer.seriesguide.settings.AppSettings;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.util.ThemeUtils;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.analytics.GoogleAnalytics;
import io.fabric.sdk.android.Fabric;
import net.danlew.android.joda.JodaTimeAndroid;
import timber.log.Timber;

/**
 * Initializes settings and services and on pre-ICS implements actions for low memory state.
 *
 * @author Uwe Trottmann
 */
public class SeriesGuideApplication extends Application {

    public static final int NOTIFICATION_EPISODE_ID = 1;
    public static final int NOTIFICATION_SUBSCRIPTION_ID = 2;
    public static final int NOTIFICATION_TRAKT_AUTH_ID = 3;

    /**
     * Time calculation has changed, all episodes need re-calculation.
     */
    public static final int RELEASE_VERSION_12_BETA5 = 218;
    /**
     * Requires legacy cache clearing due to switch to Picasso for posters.
     */
    public static final int RELEASE_VERSION_16_BETA1 = 15010;
    /**
     * Requires trakt watched movie (re-)download.
     */
    public static final int RELEASE_VERSION_23_BETA4 = 15113;
    /**
     * Requires full show update due to switch to locally stored trakt ids.
     */
    public static final int RELEASE_VERSION_26_BETA3 = 15142;

    /**
     * The content authority used to identify the SeriesGuide {@link ContentProvider}
     */
    public static final String CONTENT_AUTHORITY = BuildConfig.APPLICATION_ID + ".provider";

    @Override
    public void onCreate() {
        super.onCreate();

        // logging setup
        if (BuildConfig.DEBUG) {
            // detailed logcat logging
            Timber.plant(new Timber.DebugTree());
        } else {
            // crash and error reporting
            Timber.plant(new AnalyticsTree(this));
            if (!Fabric.isInitialized()) {
                Fabric.with(this, new Crashlytics());
            }
        }

        // initialize joda-time-android
        JodaTimeAndroid.init(this);

        // Load the current theme into a global variable
        ThemeUtils.updateTheme(DisplaySettings.getThemeIndex(this));

        // Ensure GA opt-out
        GoogleAnalytics.getInstance(this).setAppOptOut(AppSettings.isGaAppOptOut(this));
        if (BuildConfig.DEBUG) {
            GoogleAnalytics.getInstance(this).setDryRun(true);
        }
        // Initialize tracker
        Analytics.getTracker(this);

        enableStrictMode();
    }

    /**
     * Used to enable {@link StrictMode} for debug builds.
     */
    private static void enableStrictMode() {
        if (!BuildConfig.DEBUG) {
            return;
        }
        // Enable StrictMode
        final ThreadPolicy.Builder threadPolicyBuilder = new ThreadPolicy.Builder();
        threadPolicyBuilder.detectAll();
        threadPolicyBuilder.penaltyLog();
        StrictMode.setThreadPolicy(threadPolicyBuilder.build());

        // Policy applied to all threads in the virtual machine's process
        final VmPolicy.Builder vmPolicyBuilder = new VmPolicy.Builder();
        vmPolicyBuilder.detectAll();
        vmPolicyBuilder.penaltyLog();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            vmPolicyBuilder.detectLeakedRegistrationObjects();
        }
        StrictMode.setVmPolicy(vmPolicyBuilder.build());
    }
}
