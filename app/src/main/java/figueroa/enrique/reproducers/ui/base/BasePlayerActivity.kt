package figueroa.enrique.reproducers.ui.base

import android.content.*
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import figueroa.enrique.reproducers.R
import figueroa.enrique.reproducers.databinding.MiniPlayerBinding
import figueroa.enrique.reproducers.service.MusicService
import figueroa.enrique.reproducers.ui.player.PlayerActivity

abstract class BasePlayerActivity : AppCompatActivity() {
    var musicService: MusicService? = null
    private var bound = false
    private val handler = Handler(Looper.getMainLooper())

    // Cada subclase devuelve su binding del mini player (o null si no lo usa)
    protected open fun getMiniPlayerBinding(): MiniPlayerBinding? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            musicService = (service as MusicService.MusicBinder).getService()
            bound = true
            onServiceReady()
            setupMiniPlayerListeners()
            updateMiniPlayer()
            startMiniPlayerUpdates()
        }
        override fun onServiceDisconnected(name: ComponentName?) { bound = false }
    }

    open fun onServiceReady() {}

    @OptIn(UnstableApi::class)
    private fun setupMiniPlayerListeners() {
        val mp = getMiniPlayerBinding() ?: return
        mp.miniPlayPause.setOnClickListener { musicService?.togglePlayPause(); updateMiniPlayer() }
        mp.miniNext.setOnClickListener { musicService?.next(); updateMiniPlayer() }
        mp.miniPrev.setOnClickListener { musicService?.previous(); updateMiniPlayer() }
        mp.root.setOnClickListener {
            if (musicService?.currentPlaylist?.isNotEmpty() == true) {
                startActivity(Intent(this, PlayerActivity::class.java))
            }
        }
    }

    private fun updateMiniPlayer() {
        val mp = getMiniPlayerBinding() ?: return
        val service = musicService ?: return
        val song = service.currentPlaylist.getOrNull(service.currentIndex)
        if (song == null) { mp.root.visibility = View.GONE; return }
        mp.root.visibility = View.VISIBLE
        mp.miniTitle.text = song.title
        mp.miniArtist.text = song.artist

        val artwork = service.loadArtwork(song)
        if (artwork != null) {
            mp.miniCover.imageTintList = null
            mp.miniCover.setImageBitmap(artwork)
        } else {
            mp.miniCover.imageTintList = ContextCompat.getColorStateList(this, R.color.iconTint)
            mp.miniCover.setImageResource(R.drawable.ic_music_note)
        }

        mp.miniPlayPause.setImageResource(if (service.isPlaying()) R.drawable.ic_pause else R.drawable.ic_play)
    }

    private fun startMiniPlayerUpdates() {
        handler.post(object : Runnable {
            override fun run() { updateMiniPlayer(); handler.postDelayed(this, 1000) }
        })
    }

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
        handler.removeCallbacksAndMessages(null)
    }
}