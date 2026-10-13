package figueroa.enrique.reproducers.ui.artist

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import figueroa.enrique.reproducers.data.db.AppDatabase
import figueroa.enrique.reproducers.data.model.Song
import figueroa.enrique.reproducers.data.repository.MusicRepository
import figueroa.enrique.reproducers.service.MusicService
import figueroa.enrique.reproducers.ui.components.MiniPlayerUi
import figueroa.enrique.reproducers.ui.player.PlayerActivity
import figueroa.enrique.reproducers.ui.screens.DetailScreen
import figueroa.enrique.reproducers.ui.theme.ReproducersTheme
import figueroa.enrique.reproducers.util.ArtworkResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ArtistDetailActivity : ComponentActivity() {
    private lateinit var repo: MusicRepository
    var musicService: MusicService? = null
    private var bound = false
    private val handler = Handler(Looper.getMainLooper())

    private var currentSongState = mutableStateOf<Song?>(null)
    private var isPlayingState = mutableStateOf(false)
    private var coverSourcesState = mutableStateOf<List<Any>>(emptyList())

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            bound = true
            musicService?.setOnCurrentSongChangedListener { updateMiniPlayerState() }
            updateMiniPlayerState()
            startMiniPlayerUpdates()
        }
        override fun onServiceDisconnected(name: ComponentName?) { bound = false }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val artistName = intent.getStringExtra("artistName") ?: ""
        repo = MusicRepository(AppDatabase.getDatabase(this))

        setContent {
            val prefs = getSharedPreferences(figueroa.enrique.reproducers.ReproducersApp.PREFS_NAME, android.content.Context.MODE_PRIVATE)
            val savedMode = prefs.getString(figueroa.enrique.reproducers.ReproducersApp.KEY_APPEARANCE_MODE, figueroa.enrique.reproducers.ReproducersApp.MODE_SYSTEM)
            val isDarkMode = when (savedMode) {
                figueroa.enrique.reproducers.ReproducersApp.MODE_DARK -> true
                figueroa.enrique.reproducers.ReproducersApp.MODE_LIGHT -> false
                else -> (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }

            ReproducersTheme(darkTheme = isDarkMode) {
                val songs by repo.songsByArtist(artistName).observeAsState(initial = emptyList())
                val playlists by repo.allPlaylists.observeAsState(initial = emptyList())

                val currentSong = currentSongState.value
                val isPlaying = isPlayingState.value
                val coverSources = coverSourcesState.value

                Scaffold(
                    bottomBar = {
                        MiniPlayerUi(
                            song = currentSong,
                            isPlaying = isPlaying,
                            coverSources = coverSources,
                            onPlayPauseClick = {
                                musicService?.togglePlayPause()
                                updateMiniPlayerState()
                            },
                            onNextClick = {
                                musicService?.next()
                                updateMiniPlayerState()
                            },
                            onPrevClick = {
                                musicService?.previous()
                                updateMiniPlayerState()
                            },
                            onClick = {
                                if (musicService?.currentPlaylist?.isNotEmpty() == true) {
                                    startActivity(Intent(this@ArtistDetailActivity, PlayerActivity::class.java))
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        DetailScreen(
                            title = artistName,
                            songs = songs,
                            currentSong = currentSong,
                            playlists = playlists,
                            getPlaylistIdsForSong = { songId -> repo.getPlaylistIdsForSong(songId) },
                            onBackClick = { finish() },
                            onSongClick = { song, index ->
                                musicService?.playSong(song, songs, index)
                                startActivity(Intent(this@ArtistDetailActivity, PlayerActivity::class.java))
                            },
                            onFavoriteClick = { song ->
                                CoroutineScope(Dispatchers.IO).launch { repo.toggleFavorite(song) }
                            },
                            onEditSongConfirm = { song, title, artist, album ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    repo.updateSong(song.copy(title = title, artist = artist, album = album))
                                }
                            },
                            onAddToPlaylistSelected = { song, playlist ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    repo.addSongToPlaylist(playlist.id, song.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun updateMiniPlayerState() {
        val service = musicService ?: return
        val song = service.currentPlaylist.getOrNull(service.currentIndex)

        if (song == null) {
            currentSongState.value = null
            isPlayingState.value = false
            coverSourcesState.value = emptyList()
            return
        }

        currentSongState.value = song
        isPlayingState.value = service.isPlaying()

        CoroutineScope(Dispatchers.Main).launch {
            val sources = withContext(Dispatchers.IO) {
                ArtworkResolver.sourcesFor(this@ArtistDetailActivity, song, service)
            }
            coverSourcesState.value = sources
        }
    }

    private fun startMiniPlayerUpdates() {
        handler.post(object : Runnable {
            override fun run() {
                updateMiniPlayerState()
                handler.postDelayed(this, 1000)
            }
        })
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.startForegroundService(this, Intent(this, MusicService::class.java))
        Intent(this, MusicService::class.java).also {
            bindService(it, connection, BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        musicService?.setOnCurrentSongChangedListener(null)
        if (bound) { unbindService(connection); bound = false }
        handler.removeCallbacksAndMessages(null)
    }
}
