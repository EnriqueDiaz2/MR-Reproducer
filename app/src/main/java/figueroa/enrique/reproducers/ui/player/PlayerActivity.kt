package figueroa.enrique.reproducers.ui.player

import android.content.*
import android.os.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import figueroa.enrique.reproducers.R
import figueroa.enrique.reproducers.data.db.AppDatabase
import figueroa.enrique.reproducers.data.model.Song
import figueroa.enrique.reproducers.data.repository.MusicRepository
import figueroa.enrique.reproducers.service.MusicService
import figueroa.enrique.reproducers.ui.screens.PlayerScreen
import figueroa.enrique.reproducers.ui.theme.ReproducersTheme
import figueroa.enrique.reproducers.util.AppPreferences
import figueroa.enrique.reproducers.util.ArtworkResolver
import figueroa.enrique.reproducers.util.VisualizerHelper
import kotlinx.coroutines.*
import java.io.File

@androidx.annotation.OptIn(UnstableApi::class)
class PlayerActivity : ComponentActivity() {

    private var musicService: MusicService? = null
    private var bound = false
    private lateinit var handler: Handler
    private lateinit var repo: MusicRepository

    private var currentSongState = mutableStateOf<Song?>(null)
    private var isPlayingState = mutableStateOf(false)
    private var currentPositionState = mutableLongStateOf(0L)
    private var durationState = mutableLongStateOf(0L)
    private var isShuffleState = mutableStateOf(false)
    private var repeatModeState = mutableIntStateOf(MusicService.REPEAT_OFF)
    private var coverSourcesState = mutableStateOf<List<Any>>(emptyList())
    private var visualizerModeState = mutableIntStateOf(0)
    private var showingLyricsState = mutableStateOf(false)
    private var lyricsTextState = mutableStateOf<String?>(null)
    private var lyricsWarningShown = false

    private var spectrumViewInstance: SpectrumVisualizerView? = null
    private var vuMeterViewInstance: VuMeterView? = null
    private val visualizerHelper = VisualizerHelper()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            musicService = (service as MusicService.MusicBinder).getService()
            bound = true
            musicService?.setOnCurrentSongChangedListener { updateUIState() }
            updateUIState()
            startProgressUpdates()
            startVisualizerIfNeeded()
        }
        override fun onServiceDisconnected(name: ComponentName?) { bound = false }
    }

    private val recordAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVisualizerIfNeeded()
        } else {
            Toast.makeText(this, "Permiso de audio requerido para el visualizador", Toast.LENGTH_SHORT).show()
            setVisualizerMode(0)
        }
    }

    private val lyricsImportLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { importLyricsFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handler = Handler(Looper.getMainLooper())
        repo = MusicRepository(AppDatabase.getDatabase(this))

        spectrumViewInstance = SpectrumVisualizerView(this)
        vuMeterViewInstance = VuMeterView(this)
        visualizerModeState.intValue = AppPreferences.getVisualizerMode(this)

        setContent {
            val prefs = getSharedPreferences(figueroa.enrique.reproducers.ReproducersApp.PREFS_NAME, Context.MODE_PRIVATE)
            val savedMode = prefs.getString(figueroa.enrique.reproducers.ReproducersApp.KEY_APPEARANCE_MODE, figueroa.enrique.reproducers.ReproducersApp.MODE_SYSTEM)
            val isDarkMode = when (savedMode) {
                figueroa.enrique.reproducers.ReproducersApp.MODE_DARK -> true
                figueroa.enrique.reproducers.ReproducersApp.MODE_LIGHT -> false
                else -> (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }

            ReproducersTheme(darkTheme = isDarkMode) {
                val currentSong = currentSongState.value
                val isPlaying = isPlayingState.value
                val currentPositionMs = currentPositionState.longValue
                val durationMs = durationState.longValue
                val isShuffle = isShuffleState.value
                val repeatMode = repeatModeState.intValue
                val coverSources = coverSourcesState.value
                val visualizerMode = visualizerModeState.intValue
                val showingLyrics = showingLyricsState.value
                val lyricsText = lyricsTextState.value

                PlayerScreen(
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    isShuffle = isShuffle,
                    repeatMode = repeatMode,
                    coverSources = coverSources,
                    visualizerMode = visualizerMode,
                    showingLyrics = showingLyrics,
                    lyricsText = lyricsText,
                    onCollapseClick = { finish() },
                    onPlayPauseClick = {
                        musicService?.togglePlayPause()
                        updateUIState()
                    },
                    onNextClick = {
                        musicService?.next()
                        updateUIState()
                    },
                    onPrevClick = {
                        musicService?.previous()
                        updateUIState()
                    },
                    onShuffleClick = {
                        musicService?.let {
                            it.isShuffle = !it.isShuffle
                            isShuffleState.value = it.isShuffle
                        }
                    },
                    onRepeatClick = {
                        musicService?.let {
                            it.repeatMode = (it.repeatMode + 1) % 3
                            repeatModeState.intValue = it.repeatMode
                        }
                    },
                    onFavoriteClick = {
                        currentSong?.let { song ->
                            CoroutineScope(Dispatchers.IO).launch {
                                repo.toggleFavorite(song)
                                withContext(Dispatchers.Main) {
                                    val updated = song.copy(isFavorite = !song.isFavorite)
                                    musicService?.updateCurrentSong(updated)
                                    currentSongState.value = updated
                                }
                            }
                        }
                    },
                    onAddToPlaylistClick = {
                        currentSong?.let { song -> showAddToPlaylistDialog(song) }
                    },
                    onLyricsClick = { toggleLyrics() },
                    onImportLyricsClick = { lyricsImportLauncher.launch(arrayOf("text/*")) },
                    onPrevVisualizerModeClick = { setVisualizerMode((visualizerModeState.intValue + 2) % 3) },
                    onNextVisualizerModeClick = { setVisualizerMode((visualizerModeState.intValue + 1) % 3) },
                    onSeekTo = { posMs -> musicService?.seekTo(posMs) },
                    spectrumViewInstance = spectrumViewInstance,
                    vuMeterViewInstance = vuMeterViewInstance
                )
            }
        }
    }

    private fun setVisualizerMode(mode: Int, save: Boolean = true) {
        visualizerModeState.intValue = mode
        if (save) AppPreferences.setVisualizerMode(this, mode)
        startVisualizerIfNeeded()
    }

    private fun startVisualizerIfNeeded() {
        if (showingLyricsState.value || visualizerModeState.intValue == 0) {
            visualizerHelper.stop()
            return
        }

        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            return
        }

        val sessionId = musicService?.getExoPlayer()?.audioSessionId ?: 0
        if (sessionId > 0) {
            visualizerHelper.start(
                audioSessionId = sessionId,
                onFftData = { fft ->
                    if (visualizerModeState.intValue == 1) {
                        spectrumViewInstance?.updateFft(fft)
                    }
                },
                onWaveformData = { waveform ->
                    if (visualizerModeState.intValue == 2) {
                        vuMeterViewInstance?.updateWaveform(waveform)
                    }
                }
            )
        }
    }

    private fun toggleLyrics() {
        if (!showingLyricsState.value && !lyricsWarningShown) {
            showLyricsWarningDialog()
            return
        }
        proceedToggleLyrics()
    }

    private fun proceedToggleLyrics() {
        showingLyricsState.value = !showingLyricsState.value
        if (showingLyricsState.value) {
            visualizerHelper.stop()
            fetchOrLoadLyrics()
        } else {
            setVisualizerMode(visualizerModeState.intValue, save = false)
        }
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
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            .setCancelable(true)
            .show()
    }

    private fun fetchOrLoadLyrics() {
        val song = currentSongState.value ?: return
        lyricsTextState.value = getString(R.string.searching_lyrics)
        CoroutineScope(Dispatchers.IO).launch {
            val lyrics = if (song.lyricsPath != null) {
                try { File(song.lyricsPath).readText() } catch (e: Exception) { null }
            } else {
                LyricsFetcher.searchOnline(song.title, song.artist)
            }
            withContext(Dispatchers.Main) {
                lyricsTextState.value = lyrics ?: run {
                    if (song.artist.equals("Desconocido", ignoreCase = true))
                        getString(R.string.lyrics_needs_artist)
                    else
                        getString(R.string.no_lyrics_found)
                }
            }
        }
    }

    private fun importLyricsFile(uri: android.net.Uri) {
        val song = currentSongState.value ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (text.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PlayerActivity, getString(R.string.no_lyrics_found), Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val fileName = "lyrics_${song.id}.txt"
                val internalFile = File(filesDir, fileName)
                internalFile.writeText(text)

                val updatedSong = song.copy(lyricsPath = internalFile.absolutePath)
                repo.updateSong(updatedSong)
                musicService?.updateCurrentSong(updatedSong)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PlayerActivity, getString(R.string.lyrics_imported), Toast.LENGTH_SHORT).show()
                    if (showingLyricsState.value) fetchOrLoadLyrics()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PlayerActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
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

    private fun updateUIState() {
        val service = musicService ?: return
        val song = service.currentPlaylist.getOrNull(service.currentIndex) ?: return

        currentSongState.value = song
        isPlayingState.value = service.isPlaying()
        isShuffleState.value = service.isShuffle
        repeatModeState.intValue = service.repeatMode

        CoroutineScope(Dispatchers.Main).launch {
            val sources = withContext(Dispatchers.IO) {
                ArtworkResolver.sourcesFor(this@PlayerActivity, song, service)
            }
            coverSourcesState.value = sources
        }

        startVisualizerIfNeeded()
    }

    private fun startProgressUpdates() {
        handler.post(object : Runnable {
            override fun run() {
                val service = musicService
                if (service != null) {
                    val playing = service.isPlaying()
                    isPlayingState.value = playing
                    if (!playing) {
                        spectrumViewInstance?.reset()
                    }
                    if (service.isReady()) {
                        durationState.longValue = service.getDuration()
                        currentPositionState.longValue = service.getCurrentPosition()
                    }
                }
                handler.postDelayed(this, 500)
            }
        })
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.startForegroundService(this, Intent(this, MusicService::class.java))
        Intent(this, MusicService::class.java).also { bindService(it, connection, BIND_AUTO_CREATE) }
    }

    override fun onStop() {
        super.onStop()
        visualizerHelper.stop()
        musicService?.setOnCurrentSongChangedListener(null)
        if (bound) { unbindService(connection); bound = false }
        handler.removeCallbacksAndMessages(null)
    }
}
