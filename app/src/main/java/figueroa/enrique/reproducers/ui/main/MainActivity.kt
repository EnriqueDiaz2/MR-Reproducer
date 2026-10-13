@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package figueroa.enrique.reproducers.ui.main

import android.Manifest
import android.app.Activity
import android.content.*
import android.net.Uri
import android.os.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.OptIn
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.util.UnstableApi
import figueroa.enrique.reproducers.R
import figueroa.enrique.reproducers.ReproducersApp
import figueroa.enrique.reproducers.data.db.AppDatabase
import figueroa.enrique.reproducers.data.model.Album
import figueroa.enrique.reproducers.data.model.Song
import figueroa.enrique.reproducers.data.repository.MusicRepository
import figueroa.enrique.reproducers.service.MusicService
import figueroa.enrique.reproducers.ui.album.AlbumDetailActivity
import figueroa.enrique.reproducers.ui.artist.ArtistDetailActivity
import figueroa.enrique.reproducers.ui.components.MiniPlayerUi
import figueroa.enrique.reproducers.ui.player.PlayerActivity
import figueroa.enrique.reproducers.ui.playlist.PlaylistDetailActivity
import figueroa.enrique.reproducers.ui.screens.*
import figueroa.enrique.reproducers.ui.theme.ReproducersTheme
import figueroa.enrique.reproducers.util.AppPreferences
import figueroa.enrique.reproducers.util.ArtworkResolver
import figueroa.enrique.reproducers.util.EqualizerHelper
import figueroa.enrique.reproducers.util.InfoDialogHelper
import kotlinx.coroutines.*
import java.util.Locale

class MainActivity : ComponentActivity() {

    var musicService: MusicService? = null
    private var bound = false
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var repo: MusicRepository
    private var equalizerHelper: EqualizerHelper? = null

    private var currentSongState = mutableStateOf<Song?>(null)
    private var isPlayingState = mutableStateOf(false)
    private var coverSourcesState = mutableStateOf<List<Any>>(emptyList())
    private var albumToEdit: Album? = null

    private val folderPicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> scanFolder(uri) }
        } else {
            Toast.makeText(this, getString(R.string.selection_cancelled), Toast.LENGTH_SHORT).show()
        }
    }

    private val filePicker = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) importFiles(uris)
        else Toast.makeText(this, getString(R.string.no_file_selected), Toast.LENGTH_SHORT).show()
    }

    private val pickAlbumImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let { imageUri ->
                contentResolver.takePersistableUriPermission(
                    imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                albumToEdit?.let { album ->
                    CoroutineScope(Dispatchers.IO).launch {
                        repo.updateAlbumCover(album.id, imageUri.toString())
                    }
                }
            }
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            bound = true
            musicService?.setOnCurrentSongChangedListener { updateMiniPlayerState() }
            updateMiniPlayerState()
            startMiniPlayerUpdates()
            setupEqualizer()
        }
        override fun onServiceDisconnected(name: ComponentName?) { bound = false }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repo = MusicRepository(AppDatabase.getDatabase(this))
        requestNeededPermissions()

        if (!AppPreferences.isWelcomeShown(this)) {
            InfoDialogHelper.show(
                context = this,
                title = getString(R.string.welcome_title),
                message = getString(R.string.welcome_message),
                checkboxText = getString(R.string.dont_show_again),
                buttonText = getString(R.string.understood)
            ) { dontShowAgain ->
                if (dontShowAgain) {
                    AppPreferences.setWelcomeShown(this)
                }
            }
        }

        setContent {
            val prefs = getSharedPreferences(ReproducersApp.PREFS_NAME, Context.MODE_PRIVATE)
            val savedMode = prefs.getString(ReproducersApp.KEY_APPEARANCE_MODE, ReproducersApp.MODE_SYSTEM)
            var isDarkMode by remember {
                mutableStateOf(
                    when (savedMode) {
                        ReproducersApp.MODE_DARK -> true
                        ReproducersApp.MODE_LIGHT -> false
                        else -> (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                    }
                )
            }

            ReproducersTheme(darkTheme = isDarkMode) {
                var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0: Songs, 1: Albums, 2: Artists, 3: Playlists, 4: Favorites, 5: Settings

                val songs by repo.allSongs.observeAsState(initial = emptyList())
                val albums by repo.allAlbums.observeAsState(initial = emptyList())
                val artists by repo.allArtists.observeAsState(initial = emptyList())
                val playlists by repo.allPlaylists.observeAsState(initial = emptyList())
                val favorites by repo.favorites.observeAsState(initial = emptyList())

                val currentSong = currentSongState.value
                val isPlaying = isPlayingState.value
                val coverSources = coverSourcesState.value

                var isAdaptiveShuffleEnabled by remember { mutableStateOf(AppPreferences.isAdaptiveShuffleEnabled(this@MainActivity)) }
                var isAppCarouselEnabled by remember { mutableStateOf(AppPreferences.isAppCarouselEnabled(this@MainActivity)) }
                var isNotificationCarouselEnabled by remember { mutableStateOf(AppPreferences.isNotificationCarouselEnabled(this@MainActivity)) }

                Scaffold(
                    topBar = {
                        OptInTopBar(
                            selectedTab = selectedTab,
                            onSettingsClick = { selectedTab = 5 },
                            onBackFromSettingsClick = { selectedTab = 0 }
                        )
                    },
                    bottomBar = {
                        if (selectedTab != 5) {
                            Column {
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
                                            startActivity(Intent(this@MainActivity, PlayerActivity::class.java))
                                        }
                                    }
                                )

                                NavigationBar {
                                    NavigationBarItem(
                                        selected = selectedTab == 0,
                                        onClick = { selectedTab = 0 },
                                        icon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                                        label = { Text(stringResource(R.string.songs)) }
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == 1,
                                        onClick = { selectedTab = 1 },
                                        icon = { Icon(Icons.Default.Album, contentDescription = null) },
                                        label = { Text(stringResource(R.string.albums)) }
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == 2,
                                        onClick = { selectedTab = 2 },
                                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                                        label = { Text(stringResource(R.string.artists)) }
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == 3,
                                        onClick = { selectedTab = 3 },
                                        icon = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                                        label = { Text(stringResource(R.string.playlists)) }
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == 4,
                                        onClick = { selectedTab = 4 },
                                        icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                                        label = { Text(stringResource(R.string.favorites)) }
                                    )
                                }
                            }
                        }
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        when (selectedTab) {
                            0 -> SongsScreen(
                                songs = songs,
                                currentSong = currentSong,
                                playlists = playlists,
                                getPlaylistIdsForSong = { songId -> repo.getPlaylistIdsForSong(songId) },
                                onSongClick = { song, index ->
                                    playSongInService(song, songs, index)
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
                            1 -> AlbumsScreen(
                                albums = albums,
                                onAlbumClick = { album ->
                                    startActivity(
                                        Intent(this@MainActivity, AlbumDetailActivity::class.java)
                                            .putExtra("albumId", album.id)
                                            .putExtra("albumName", album.name)
                                            .putExtra("albumArtist", album.artist)
                                    )
                                },
                                onEditCoverClick = { album ->
                                    albumToEdit = album
                                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                        type = "image/*"
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                    }
                                    pickAlbumImageLauncher.launch(intent)
                                }
                            )
                            2 -> ArtistsScreen(
                                artists = artists,
                                onArtistClick = { artist ->
                                    startActivity(
                                        Intent(this@MainActivity, ArtistDetailActivity::class.java)
                                            .putExtra("artistName", artist.name)
                                    )
                                }
                            )
                            3 -> PlaylistsScreen(
                                playlists = playlists,
                                onPlaylistClick = { playlist ->
                                    startActivity(
                                        Intent(this@MainActivity, PlaylistDetailActivity::class.java)
                                            .putExtra("playlistId", playlist.id)
                                            .putExtra("playlistName", playlist.name)
                                    )
                                },
                                onCreatePlaylistConfirm = { name ->
                                    CoroutineScope(Dispatchers.IO).launch { repo.createPlaylist(name) }
                                }
                            )
                            4 -> FavoritesScreen(
                                favorites = favorites,
                                currentSong = currentSong,
                                playlists = playlists,
                                getPlaylistIdsForSong = { songId -> repo.getPlaylistIdsForSong(songId) },
                                onSongClick = { song, index ->
                                    playSongInService(song, favorites, index)
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
                            5 -> SettingsScreen(
                                isAdaptiveShuffleEnabled = isAdaptiveShuffleEnabled,
                                onAdaptiveShuffleChanged = { isChecked ->
                                    isAdaptiveShuffleEnabled = isChecked
                                    if (isChecked && !AppPreferences.isAdaptiveShuffleInfoShown(this@MainActivity)) {
                                        InfoDialogHelper.show(
                                            context = this@MainActivity,
                                            title = getString(R.string.adaptive_shuffle_info_title),
                                            message = getString(R.string.adaptive_shuffle_info_message),
                                            checkboxText = getString(R.string.dont_show_again),
                                            buttonText = getString(R.string.understood)
                                        ) { dontShowAgain ->
                                            if (dontShowAgain) {
                                                AppPreferences.setAdaptiveShuffleInfoShown(this@MainActivity)
                                            }
                                        }
                                    }
                                    AppPreferences.setAdaptiveShuffle(this@MainActivity, isChecked)
                                },
                                isAppCarouselEnabled = isAppCarouselEnabled,
                                onAppCarouselChanged = { isChecked ->
                                    isAppCarouselEnabled = isChecked
                                    if (isChecked && !AppPreferences.isCarouselInfoShown(this@MainActivity)) {
                                        InfoDialogHelper.show(
                                            context = this@MainActivity,
                                            title = getString(R.string.carousel_info_title),
                                            message = getString(R.string.carousel_info_message),
                                            checkboxText = getString(R.string.dont_show_again),
                                            buttonText = getString(R.string.understood)
                                        ) { dontShowAgain ->
                                            if (dontShowAgain) {
                                                AppPreferences.setCarouselInfoShown(this@MainActivity)
                                            }
                                        }
                                    }
                                    AppPreferences.setAppCarousel(this@MainActivity, isChecked)
                                    musicService?.refreshCarouselSettings()
                                },
                                isNotificationCarouselEnabled = isNotificationCarouselEnabled,
                                onNotificationCarouselChanged = { isChecked ->
                                    isNotificationCarouselEnabled = isChecked
                                    if (isChecked && !AppPreferences.isCarouselInfoShown(this@MainActivity)) {
                                        InfoDialogHelper.show(
                                            context = this@MainActivity,
                                            title = getString(R.string.carousel_info_title),
                                            message = getString(R.string.carousel_info_message),
                                            checkboxText = getString(R.string.dont_show_again),
                                            buttonText = getString(R.string.understood)
                                        ) { dontShowAgain ->
                                            if (dontShowAgain) {
                                                AppPreferences.setCarouselInfoShown(this@MainActivity)
                                            }
                                        }
                                    }
                                    AppPreferences.setNotificationCarousel(this@MainActivity, isChecked)
                                    musicService?.refreshCarouselSettings()
                                },
                                isDarkMode = isDarkMode,
                                onDarkModeChanged = { isDark ->
                                    isDarkMode = isDark
                                    val mode = if (isDark) ReproducersApp.MODE_DARK else ReproducersApp.MODE_LIGHT
                                    prefs.edit().putString(ReproducersApp.KEY_APPEARANCE_MODE, mode).apply()
                                    AppCompatDelegate.setDefaultNightMode(
                                        if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                                    )
                                },
                                onSelectFolderClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                                        folderPicker.launch(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                },
                                onImportFilesClick = {
                                    try {
                                        filePicker.launch(arrayOf("audio/*", "video/*"))
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                },
                                onRescanFolderClick = { rescanFolder() },
                                equalizerBandsCount = equalizerHelper?.bandCount?.toInt() ?: 0,
                                getBandLevel = { band -> equalizerHelper?.getBandLevel(band) ?: 0 },
                                getBandRange = { equalizerHelper?.bandLevelRange },
                                setBandLevel = { band, level -> equalizerHelper?.setBandLevel(band, level) }
                            )
                        }
                    }
                }
            }
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    @OptIn(UnstableApi::class)
    private fun playSongInService(song: Song, list: List<Song>, index: Int) {
        val service = musicService
        if (service == null) {
            ContextCompat.startForegroundService(this, Intent(this, MusicService::class.java))
            Toast.makeText(this, getString(R.string.player_not_ready), Toast.LENGTH_SHORT).show()
            return
        }
        service.playSong(song, list, index)
        startActivity(Intent(this, PlayerActivity::class.java))
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
                ArtworkResolver.sourcesFor(this@MainActivity, song, service)
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

    private fun scanFolder(treeUri: Uri) {
        contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        AppPreferences.setMusicFolderUri(this, treeUri.toString())
        val dir = DocumentFile.fromTreeUri(this, treeUri) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val songs = mutableListOf<Song>()
            dir.listFiles().forEach { file ->
                val type = file.type ?: ""
                if (file.isFile && (type.startsWith("audio") || type.startsWith("video"))) {
                    songs.add(
                        Song(
                            title = file.name?.substringBeforeLast(".") ?: getString(R.string.unknown_value),
                            artist = getString(R.string.unknown_value),
                            album = getString(R.string.unknown_value),
                            filePath = file.uri.toString(),
                            duration = 0L,
                            isVideo = type.startsWith("video")
                        )
                    )
                }
            }
            repo.importSongsFromFolder(songs)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, getString(R.string.songs_imported_count, songs.size), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun rescanFolder() {
        val uriString = AppPreferences.getMusicFolderUri(this)
        if (uriString.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.select_music_folder), Toast.LENGTH_SHORT).show()
            try {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                folderPicker.launch(intent)
            } catch (e: Exception) {}
            return
        }
        val treeUri = Uri.parse(uriString)
        try {
            contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {}
        val dir = DocumentFile.fromTreeUri(this, treeUri)
        if (dir == null) {
            Toast.makeText(this, getString(R.string.could_not_read_folder), Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val existingSongs = repo.getAllSongsList()
            val existingPaths = existingSongs.map { it.filePath }.toSet()
            val newSongs = mutableListOf<Song>()
            dir.listFiles().forEach { file ->
                val type = file.type ?: ""
                if (file.isFile && (type.startsWith("audio") || type.startsWith("video"))) {
                    val path = file.uri.toString()
                    if (!existingPaths.contains(path)) {
                        newSongs.add(
                            Song(
                                title = file.name?.substringBeforeLast(".") ?: getString(R.string.unknown_value),
                                artist = getString(R.string.unknown_value),
                                album = getString(R.string.unknown_value),
                                filePath = path,
                                duration = 0L,
                                isVideo = type.startsWith("video")
                            )
                        )
                    }
                }
            }
            if (newSongs.isNotEmpty()) {
                repo.importSongsFromFolder(newSongs)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, getString(R.string.songs_imported_count, newSongs.size), Toast.LENGTH_SHORT).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, getString(R.string.no_more_songs), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun importFiles(uris: List<Uri>) {
        CoroutineScope(Dispatchers.IO).launch {
            val songs = uris.map { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                Song(
                    title = uri.lastPathSegment?.substringAfterLast("/") ?: getString(R.string.unknown_value),
                    artist = getString(R.string.unknown_value),
                    album = getString(R.string.unknown_value),
                    filePath = uri.toString(),
                    duration = 0L
                )
            }
            repo.importSongsFromFolder(songs)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "${songs.size} archivos importados", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    @OptIn(UnstableApi::class)
    private fun setupEqualizer() {
        val service = musicService ?: return
        val sessionId = service.getExoPlayer().audioSessionId
        if (sessionId > 0) {
            try {
                equalizerHelper = EqualizerHelper(sessionId)
            } catch (_: Exception) {}
        }
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

    private fun requestNeededPermissions() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
            perms.add(Manifest.permission.READ_MEDIA_AUDIO)
            perms.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        ActivityCompat.requestPermissions(this, perms.toTypedArray(), 100)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptInTopBar(
    selectedTab: Int,
    onSettingsClick: () -> Unit,
    onBackFromSettingsClick: () -> Unit
) {
    val title = when (selectedTab) {
        0 -> stringResource(R.string.songs)
        1 -> stringResource(R.string.albums)
        2 -> stringResource(R.string.artists)
        3 -> stringResource(R.string.playlists)
        4 -> stringResource(R.string.favorites)
        5 -> stringResource(R.string.settings)
        else -> stringResource(R.string.app_name)
    }

    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            if (selectedTab == 5) {
                IconButton(onClick = onBackFromSettingsClick) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                }
            }
        },
        actions = {
            if (selectedTab != 5) {
                IconButton(onClick = onSettingsClick) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Configuración")
                }
            }
        }
    )
}
