package com.flxrs.dankchat

import android.app.Application
import android.app.UiModeManager
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.ImageDecoderDecoder
import com.flxrs.dankchat.di.EmoteOkHttpClient
import com.flxrs.dankchat.utils.gifs.GifDrawableDecoder
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class DankChatApplication : Application(), ImageLoaderFactory {
    @Inject
    @EmoteOkHttpClient
    lateinit var client: OkHttpClient

    override fun onCreate() {
        super.onCreate()

        val uiModeManager = getSystemService<UiModeManager>()
        val isTv = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val followSystemKey = getString(R.string.preference_follow_system_key)
        val darkModeKey = getString(R.string.preference_dark_theme_key)
        val themeKey = getString(R.string.preference_theme_key)
        val theme = preferences.getString(themeKey, followSystemKey) ?: followSystemKey

        val supportsLightMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 || isTv
        val supportsSystemDarkMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        val nightMode = when {
            // Force dark theme on < Android 8.1 because of statusbar/navigationbar issues, or if system dark mode is not supported
            theme == darkModeKey || !supportsLightMode || (theme == followSystemKey && !supportsSystemDarkMode) -> AppCompatDelegate.MODE_NIGHT_YES
            theme == followSystemKey                                                                            -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else                                                                                                -> AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            //.logger(DebugLogger())
            .okHttpClient { client }
            .componentRegistry {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> add(ImageDecoderDecoder(context = this@DankChatApplication, enforceMinimumFrameDelay = true))
                    else                                           -> add(GifDrawableDecoder())
                }
            }
            .build()
    }
}