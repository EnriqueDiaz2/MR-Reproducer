package figueroa.enrique.reproducers.ui.base

import android.content.*
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import figueroa.enrique.reproducers.service.MusicService

abstract class BasePlayerActivity : AppCompatActivity() {
    var musicService: MusicService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            musicService = (service as MusicService.MusicBinder).getService()
            bound = true
            onServiceReady()
        }
        override fun onServiceDisconnected(name: ComponentName?) { bound = false }
    }

    open fun onServiceReady() {}

    override fun onStart() {
        super.onStart()
        ContextCompat.startForegroundService(this, Intent(this, MusicService::class.java))
        Intent(this, MusicService::class.java).also {
            bindService(it, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (bound) { unbindService(connection); bound = false }
    }
}
