package figueroa.enrique.reproducers

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class ReproducersApp : Application() {
    override fun onCreate() {
        super.onCreate()
        when (getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_APPEARANCE_MODE, MODE_SYSTEM)) {
            MODE_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            MODE_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    companion object {
        const val PREFS_NAME = "settings_prefs"
        const val KEY_APPEARANCE_MODE = "appearance_mode"
        const val MODE_LIGHT = "light"
        const val MODE_DARK = "dark"
        const val MODE_SYSTEM = "system"
    }
}