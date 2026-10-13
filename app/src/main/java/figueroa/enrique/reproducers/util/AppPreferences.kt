package figueroa.enrique.reproducers.util

import android.content.Context
import androidx.core.content.edit

object AppPreferences {
    private const val PREFS = "app_prefs"
    private const val KEY_ADAPTIVE_SHUFFLE = "adaptive_shuffle"
    private const val KEY_APP_CAROUSEL = "app_carousel_enabled"
    private const val KEY_NOTIF_CAROUSEL = "notification_carousel_enabled"
    private const val KEY_WELCOME_SHOWN = "welcome_message_shown"
    private const val KEY_ADAPTIVE_SHUFFLE_INFO_SHOWN = "adaptive_shuffle_info_shown"
    private const val KEY_CAROUSEL_INFO_SHOWN = "carousel_info_shown"

    fun isAdaptiveShuffleEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ADAPTIVE_SHUFFLE, false)

    fun setAdaptiveShuffle(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_ADAPTIVE_SHUFFLE, enabled) }
    }

    fun isAppCarouselEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_APP_CAROUSEL, false)

    fun setAppCarousel(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_APP_CAROUSEL, enabled) }
    }

    fun isNotificationCarouselEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTIF_CAROUSEL, false)

    fun setNotificationCarousel(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_NOTIF_CAROUSEL, enabled) }
    }

    fun isWelcomeShown(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_WELCOME_SHOWN, false)

    fun setWelcomeShown(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_WELCOME_SHOWN, true) }
    }

    fun isAdaptiveShuffleInfoShown(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ADAPTIVE_SHUFFLE_INFO_SHOWN, false)

    fun setAdaptiveShuffleInfoShown(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_ADAPTIVE_SHUFFLE_INFO_SHOWN, true) }
    }

    fun isCarouselInfoShown(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_CAROUSEL_INFO_SHOWN, false)

    fun setCarouselInfoShown(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_CAROUSEL_INFO_SHOWN, true) }
    }

    private const val KEY_VISUALIZER_MODE = "visualizer_mode"
    private const val KEY_MUSIC_FOLDER_URI = "music_folder_uri"

    fun getVisualizerMode(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_VISUALIZER_MODE, 0)

    fun setVisualizerMode(context: Context, mode: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putInt(KEY_VISUALIZER_MODE, mode) }
    }

    fun getMusicFolderUri(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_MUSIC_FOLDER_URI, null)

    fun setMusicFolderUri(context: Context, uri: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putString(KEY_MUSIC_FOLDER_URI, uri) }
    }
}
