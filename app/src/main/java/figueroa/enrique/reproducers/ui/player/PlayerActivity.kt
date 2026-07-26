package figueroa.enrique.reproducers.ui.player

import android.content.*
import android.os.*
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import figueroa.enrique.reproducers.R
import figueroa.enrique.reproducers.data.db.AppDatabase
import figueroa.enrique.reproducers.data.model.Song
import figueroa.enrique.reproducers.data.repository.MusicRepository
import figueroa.enrique.reproducers.databinding.ActivityPlayerBinding
import figueroa.enrique.reproducers.service.MusicService
import figueroa.enrique.reproducers.util.AppPreferences
import figueroa.enrique.reproducers.util.ArtworkResolver
import figueroa.enrique.reproducers.util.CoverCarousel
import kotlinx.coroutines.*
import java.io.File

@UnstableApi
class PlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlayerBinding
    private var musicService: MusicService? = null
    private var bound = false
    private lateinit var handler: Handler
    private lateinit var repo: MusicRepository
    private var showingLyrics = false
    private var isUserSeeking = false
    private var lyricsWarningShown = false

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

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                musicService?.seekTo(seekBar!!.progress.toLong())
                isUserSeeking = false
            }
        })

        binding.btnImportLyrics.setOnClickListener {
            lyricsImportLauncher.launch(arrayOf("text/*"))
        }
    }

    private val lyricsImportLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { importLyricsFile(it) }
    }

    private fun importLyricsFile(uri: android.net.Uri) {
        val song = currentSong ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (text.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PlayerActivity, getString(R.string.no_lyrics_found), Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                // Guarda una copia local para no depender del URI externo
                val fileName = "lyrics_${song.id}.txt"
                val internalFile = File(filesDir, fileName)
                internalFile.writeText(text)

                val updatedSong = song.copy(lyricsPath = internalFile.absolutePath)
                repo.updateSong(updatedSong)
                musicService?.updateCurrentSong(updatedSong)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PlayerActivity, getString(R.string.lyrics_imported), Toast.LENGTH_SHORT).show()
                    if (showingLyrics) fetchOrLoadLyrics()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PlayerActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private val currentSong get() = musicService?.currentPlaylist?.getOrNull(musicService?.currentIndex ?: 0)

    private fun updateUI() {
        val song = currentSong ?: return
        binding.playerTitle.text = song.title
        binding.playerArtist.text = song.artist

        CoroutineScope(Dispatchers.Main).launch {
            val sources = withContext(Dispatchers.IO) {
                ArtworkResolver.sourcesFor(this@PlayerActivity, song, musicService)
            }
            if (AppPreferences.isAppCarouselEnabled(this@PlayerActivity) && sources.size > 1) {
                CoverCarousel.start(binding.playerCover, sources)
            } else {
                CoverCarousel.applyStatic(binding.playerCover, sources)
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

    private fun showAddToPlaylistDialog(song: Song) {
        repo.allPlaylists.observe(this) { playlists ->
            if (playlists.isEmpty()) {
                Toast.makeText(this, getString(R.string.first_create_playlist), Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(this@PlayerActivity, getString(R.string.added_to_playlist, playlist.name), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

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

    private fun toggleLyrics() {
        // Si vamos a MOSTRAR la letra y aún no se ha avisado en esta sesión, mostrar el aviso primero
        if (!showingLyrics && !lyricsWarningShown) {
            showLyricsWarningDialog()
            return
        }
        proceedToggleLyrics()
    }

    private fun proceedToggleLyrics() {
        showingLyrics = !showingLyrics
        binding.lyricsView.visibility = if (showingLyrics) View.VISIBLE else View.GONE
        binding.playerCover.visibility = if (showingLyrics) View.GONE else View.VISIBLE
        if (showingLyrics) fetchOrLoadLyrics()
    }

    private fun showLyricsWarningDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.lyrics_warning_title))
            .setMessage(getString(R.string.lyrics_warning_message))
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                lyricsWarningShown = true
                dialog.dismiss()
                proceedToggleLyrics()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun fetchOrLoadLyrics() {
        val song = currentSong ?: return
        binding.lyricsView.text = getString(R.string.searching_lyrics)
        CoroutineScope(Dispatchers.IO).launch {
            val lyrics = if (song.lyricsPath != null) {
                try { File(song.lyricsPath).readText() } catch (e: Exception) { null }
            } else {
                LyricsFetcher.searchOnline(song.title, song.artist)
            }
            withContext(Dispatchers.Main) {
                binding.lyricsView.text = lyrics ?: run {
                    if (song.artist.equals("Desconocido", ignoreCase = true))
                        getString(R.string.lyrics_needs_artist)
                    else
                        getString(R.string.no_lyrics_found)
                }
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
