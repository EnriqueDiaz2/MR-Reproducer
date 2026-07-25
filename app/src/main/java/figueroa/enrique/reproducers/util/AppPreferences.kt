package figueroa.enrique.reproducers.util

import android.content.Context

object AppPreferences {
    private const val PREFS = "app_prefs"
    private const val KEY_ADAPTIVE_SHUFFLE = "adaptive_shuffle"

    fun isAdaptiveShuffleEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ADAPTIVE_SHUFFLE, false)

    fun setAdaptiveShuffle(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ADAPTIVE_SHUFFLE, enabled).apply()
    }

    private const val KEY_APP_CAROUSEL = "app_carousel_enabled"
    private const val KEY_NOTIF_CAROUSEL = "notification_carousel_enabled"

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
}
