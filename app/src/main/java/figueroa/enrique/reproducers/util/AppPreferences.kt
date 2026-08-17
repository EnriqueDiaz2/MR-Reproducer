package figueroa.enrique.reproducers.util

import android.content.Context

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
            .edit().putBoolean(KEY_ADAPTIVE_SHUFFLE, enabled).apply()
    }

    fun isAppCarouselEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_APP_CAROUSEL, false)

    fun setAppCarousel(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_APP_CAROUSEL, enabled).apply()
    }

    fun isNotificationCarouselEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTIF_CAROUSEL, false)

    fun setNotificationCarousel(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_NOTIF_CAROUSEL, enabled).apply()
    }

    fun isWelcomeShown(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_WELCOME_SHOWN, false)

    fun setWelcomeShown(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_WELCOME_SHOWN, true).apply()
    }

    fun isAdaptiveShuffleInfoShown(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ADAPTIVE_SHUFFLE_INFO_SHOWN, false)

    fun setAdaptiveShuffleInfoShown(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ADAPTIVE_SHUFFLE_INFO_SHOWN, true).apply()
    }

    fun isCarouselInfoShown(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_CAROUSEL_INFO_SHOWN, false)

    fun setCarouselInfoShown(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_CAROUSEL_INFO_SHOWN, true).apply()
    }
}
