/*
 * Copyright 2014 Eng Chong Meng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cog.hymnchtv.utils;

import android.content.Context;

import org.cog.hymnchtv.R;

/**
 * ThemeHelper class that set the app Theme as specified by user
 *
 * @author Eng Chong Meng
 */
public class ThemeHelper {
    /**
     * Possible values for the different theme settings. Important:
     * Do not change the order of the items! The ordinal value (position) is used when saving the settings.
     */
    public enum Theme {
        LIGHT,
        DARK
    }

    /** Theme for users who never chose one (user decision 2026-10-02: light). MainActivity uses it as the pref default. */
    public static final Theme DEFAULT_THEME = Theme.LIGHT;

    // Note: mTheme is kept in sync with the stored theme by ThemePrefs (HymnsApp.onCreate and every change)
    private static Theme mTheme = DEFAULT_THEME;

    /**
     * Set the app Theme per current mTheme
     *
     * @param ctx context
     */
    public static void setTheme(Context ctx) {
        // DayNight is applied globally via AppCompatDelegate (ThemePrefs, from HymnsApp); signature kept for compatibility.
    }

    /**
     * Set the app theme as per specified
     *
     * @param ctx context
     * @param theme the new theme
     */
    public static void setTheme(Context ctx, Theme theme) {
        mTheme = theme;
    }

    /**
     * Called by ThemePrefs once the DayNight mode is resolved, so isAppTheme() (CSS choice) matches what is on screen.
     * Also restores the cache after process death (HymnsApp.onCreate), which the old static field did not survive.
     */
    public static void syncDark(boolean dark) {
        mTheme = dark ? Theme.DARK : Theme.LIGHT;
    }

    public static Theme getAppTheme() {
        return mTheme;
    }

    public static void setAppTheme(Theme theme) {
        mTheme = theme;
    }

    public static int getAppThemeResourceId() {
        return getAppThemeResourceId(mTheme);
    }

    /**
     * Get the app specific theme to init android theme for use
     *
     * @param theme the current theme
     *
     * @return app android theme for use
     */
    private static int getAppThemeResourceId(Theme theme) {
        return (theme == Theme.LIGHT) ? R.style.AppTheme_Light : R.style.AppTheme_Dark;
    }

    /**
     * Return true if the current app mTheme is per the specifies theme
     *
     * @param theme the theme
     *
     * @return true if the current app mThem is the same as the specified mTheme
     */
    public static boolean isAppTheme(Theme theme) {
        return (mTheme == theme);
    }
}
