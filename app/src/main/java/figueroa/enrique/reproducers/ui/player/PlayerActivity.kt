package figueroa.enrique.reproducers.ui.player

import android.content.*
import android.graphics.Bitmap
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.Glide
import figueroa.enrique.reproducers.R
import figueroa.enrique.reproducers.data.db.AppDatabase
import figueroa.enrique.reproducers.data.repository.MusicRepository
import figueroa.enrique.reproducers.databinding.ActivityPlayerBinding
import figueroa.enrique.reproducers.service.MusicService
import kotlinx.coroutines.*

@UnstableApi
class PlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlayerBinding
    private var musicService: MusicService? = null
    private var bound = false
    private lateinit var handler: Handler
    private lateinit var repo: MusicRepository
    private var showingLyrics = false
    private var isUserSeeking = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            musicService = (service as MusicService.MusicBinder).getService()
            bound = true
            musicService?.setOnCurrentSongChangedListener { updateUI() }
            updateUI()
            syncShuffleRepeatIcons()
            startProgressUpdates()
        }
        override fun onServiceDisconnected(name: ComponentName?) { bound = false }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handler = Handler(Looper.getMainLooper())
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repo = MusicRepository(AppDatabase.getDatabase(this))

        binding.btnCollapse.setOnClickListener { finish() }

        binding.btnPlayPause.setOnClickListener {
            musicService?.togglePlayPause()
            updatePlayPauseIcon()
        }
        binding.btnNext.setOnClickListener { musicService?.next(); updateUI() }
        binding.btnPrevious.setOnClickListener { musicService?.previous(); updateUI() }

        binding.btnShuffle.setOnClickListener {
            musicService?.let {
                it.isShuffle = !it.isShuffle
                binding.btnShuffle.alpha = if (it.isShuffle) 1f else 0.4f
            }
        }



        binding.btnRepeat.setOnClickListener {
            musicService?.let {
                it.repeatMode = (it.repeatMode + 1) % 3
                val icon = when (it.repeatMode) {
                    MusicService.REPEAT_ONE -> R.drawable.ic_repeat_one
                    MusicService.REPEAT_ALL -> R.drawable.ic_repeat
                    else -> R.drawable.ic_repeat
                }
                binding.btnRepeat.setImageResource(icon)
                binding.btnRepeat.alpha = if (it.repeatMode == MusicService.REPEAT_OFF) 0.4f else 1f
            }
        }

        binding.btnFavoritePlayer.setOnClickListener {
            currentSong?.let { song ->
                CoroutineScope(Dispatchers.IO).launch {
                    repo.toggleFavorite(song)
                    withContext(Dispatchers.Main) {
                        musicService?.updateCurrentSong(song.copy(isFavorite = !song.isFavorite))
                        updateUI()
                    }
                }
            }
        }

        binding.btnAddToPlaylist.setOnClickListener {
            currentSong?.let { song -> showAddToPlaylistDialog(song) }
        }

        binding.btnLyrics.setOnClickListener { toggleLyrics() }

        binding.seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {
                isUserSeeking = true
            }
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                musicService?.seekTo(seekBar!!.progress.toLong())
                isUserSeeking = false
            }
        })

        /*binding.seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                musicService?.seekTo(seekBar!!.progress.toLong())
            }
        })*/
    }

    private val currentSong get() = musicService?.currentPlaylist?.getOrNull(musicService?.currentIndex ?: 0)

    private fun updateUI() {
        val song = currentSong ?: return
        binding.playerTitle.text = song.title
        binding.playerArtist.text = song.artist

        CoroutineScope(Dispatchers.Main).launch {
            val artwork = withContext(Dispatchers.IO) { loadSafeArtwork(song) }

            if (artwork != null) {
                binding.playerCover.imageTintList = null
                binding.playerCover.setImageBitmap(artwork)
            } else {
                binding.playerCover.imageTintList = ContextCompat.getColorStateList(this@PlayerActivity, R.color.iconTint)
                binding.playerCover.setImageResource(R.drawable.ic_music_note)
            }
        }
        binding.btnFavoritePlayer.setImageResource(
            if (song.isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
        )
        updatePlayPauseIcon()
    }

    private fun syncShuffleRepeatIcons() {
        val service = musicService ?: return
        binding.btnShuffle.alpha = if (service.isShuffle) 1f else 0.4f
        val repeatIcon = when (service.repeatMode) {
            MusicService.REPEAT_ONE -> R.drawable.ic_repeat_one
            else -> R.drawable.ic_repeat
        }
        binding.btnRepeat.setImageResource(repeatIcon)
        binding.btnRepeat.alpha = if (service.repeatMode == MusicService.REPEAT_OFF) 0.4f else 1f
    }

    private suspend fun loadSafeArtwork(song: figueroa.enrique.reproducers.data.model.Song): Bitmap? {
        val albumCoverPath = repo.albumById(song.albumId)?.coverImagePath
        val albumCover = albumCoverPath?.let { path ->
            try {
                Glide.with(this@PlayerActivity)
                    .asBitmap()
                    .load(path)
                    .frame(1000000)
                    .submit()
                    .get()
            } catch (e: Exception) {
                null
            }
        }

        if (albumCover != null && !isLikelyBlankArtwork(albumCover)) {
            return albumCover
        }

        val embedded = musicService?.loadArtwork(song)
        if (embedded != null && !isLikelyBlankArtwork(embedded)) {
            return embedded
        }

        return null
    }

    private fun isLikelyBlankArtwork(bitmap: Bitmap): Boolean {
        if (bitmap.width <= 2 || bitmap.height <= 2) return true

        val stepX = (bitmap.width / 5).coerceAtLeast(1)
        val stepY = (bitmap.height / 5).coerceAtLeast(1)
        var samples = 0
        var sum = 0.0
        var sumSquares = 0.0

        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                val brightness = (android.graphics.Color.red(pixel) + android.graphics.Color.green(pixel) + android.graphics.Color.blue(pixel)) / 3.0
                sum += brightness
                sumSquares += brightness * brightness
                samples++
                x += stepX
            }
            y += stepY
        }

        val mean = sum / samples
        val variance = (sumSquares / samples) - (mean * mean)
        return (mean > 240.0 && variance < 120.0) || (mean < 15.0 && variance < 120.0)
    }

    private fun showAddToPlaylistDialog(song: figueroa.enrique.reproducers.data.model.Song) {
        repo.allPlaylists.observe(this) { playlists ->
            if (playlists.isEmpty()) {
                android.widget.Toast.makeText(this, getString(R.string.first_create_playlist), android.widget.Toast.LENGTH_SHORT).show()
                return@observe
            }
            val names = playlists.map { it.name }.toTypedArray()
            android.app.AlertDialog.Builder(this)
                .setTitle(R.string.add_to_playlist)
                .setItems(names) { _, which ->
                    val playlist = playlists[which]
                    CoroutineScope(Dispatchers.IO).launch {
                        repo.addSongToPlaylist(playlist.id, song.id)
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(this@PlayerActivity, getString(R.string.added_to_playlist, playlist.name), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    /*private fun updateUI() {
        val song = currentSong ?: return
        binding.playerTitle.text = song.title
        binding.playerArtist.text = song.artist
        Glide.with(this).load(song.filePath).placeholder(R.drawable.ic_music_note).into(binding.playerCover)
        binding.btnFavoritePlayer.setImageResource(
            if (song.isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
        )
        updatePlayPauseIcon()
        binding.seekBar.max = musicService?.getDuration()?.toInt() ?: 0
    }*/

    private fun updatePlayPauseIcon() {
        val playing = musicService?.isPlaying() == true
        binding.btnPlayPause.setImageResource(
            if (playing) R.drawable.ic_pause_circle else R.drawable.ic_play_circle
        )
    }

    private fun startProgressUpdates() {
        handler.post(object : Runnable {
            override fun run() {
                val service = musicService
                if (service != null) {
                    updatePlayPauseIcon() // <-- se refresca cada 500ms, siempre en sincronía real

                    if (service.isReady() && !isUserSeeking) {
                        val duration = service.getDuration()
                        if (duration > 0) {
                            if (binding.seekBar.max != duration.toInt()) {
                                binding.seekBar.max = duration.toInt()
                            }
                            val current = service.getCurrentPosition().toInt()
                            if (Build.VERSION.SDK_INT >= 26) {
                                binding.seekBar.setProgress(current, true)
                            } else {
                                binding.seekBar.progress = current
                            }
                        }
                    }
                }
                handler.postDelayed(this, 500)
            }
        })
    }

    /*private fun startProgressUpdates() {
        handler.post(object : Runnable {
            override fun run() {
                val service = musicService
                if (service != null && service.isReady() && !isUserSeeking) {
                    val duration = service.getDuration()
                    if (duration > 0) {
                        if (binding.seekBar.max != duration.toInt()) {
                            binding.seekBar.max = duration.toInt()
                        }
                        val current = service.getCurrentPosition().toInt()
                        // Animación suave del progreso (API 26+)
                        if (android.os.Build.VERSION.SDK_INT >= 26) {
                            binding.seekBar.setProgress(current, true)
                        } else {
                            binding.seekBar.progress = current
                        }
                    }
                }
                handler.postDelayed(this, 500)
            }
        })
    }*/

    /*private fun startProgressUpdates() {
        handler.post(object : Runnable {
            override fun run() {
                musicService?.let {
                    binding.seekBar.progress = it.getCurrentPosition().toInt()
                }
                handler.postDelayed(this, 500)
            }
        })
    }*/

    private fun toggleLyrics() {
        showingLyrics = !showingLyrics
        binding.lyricsView.visibility = if (showingLyrics) android.view.View.VISIBLE else android.view.View.GONE
        binding.playerCover.visibility = if (showingLyrics) android.view.View.GONE else android.view.View.VISIBLE
        if (showingLyrics) fetchOrLoadLyrics()
    }

    private fun fetchOrLoadLyrics() {
        val song = currentSong ?: return
        binding.lyricsView.text = getString(R.string.searching_lyrics)
        CoroutineScope(Dispatchers.IO).launch {
            val lyrics = if (song.lyricsPath != null) {
                java.io.File(song.lyricsPath).readText()
            } else {
                LyricsFetcher.searchOnline(song.title, song.artist)
            }
            withContext(Dispatchers.Main) {
                binding.lyricsView.text = lyrics ?: getString(R.string.no_lyrics_found)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.startForegroundService(this, Intent(this, MusicService::class.java))
        Intent(this, MusicService::class.java).also { bindService(it, connection, Context.BIND_AUTO_CREATE) }
    }

    override fun onStop() {
        super.onStop()
        musicService?.setOnCurrentSongChangedListener(null)
        if (bound) { unbindService(connection); bound = false }
        handler.removeCallbacksAndMessages(null)
    }
}
