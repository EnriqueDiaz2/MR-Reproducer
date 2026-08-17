package figueroa.enrique.reproducers.ui.main

import android.content.*
import android.os.*
import android.view.View
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import figueroa.enrique.reproducers.data.db.AppDatabase
import figueroa.enrique.reproducers.data.repository.MusicRepository
import figueroa.enrique.reproducers.databinding.ActivityMainBinding
import figueroa.enrique.reproducers.service.MusicService
import figueroa.enrique.reproducers.ui.player.PlayerActivity
import figueroa.enrique.reproducers.ui.songs.SongsFragment
import figueroa.enrique.reproducers.R
import figueroa.enrique.reproducers.ui.album.AlbumsFragment
import figueroa.enrique.reproducers.ui.artist.ArtistsFragment
import figueroa.enrique.reproducers.ui.favorites.FavoritesFragment
import figueroa.enrique.reproducers.ui.playlist.PlaylistsFragment
import figueroa.enrique.reproducers.ui.settings.SettingsFragment
import androidx.core.app.ActivityCompat
import android.Manifest
import figueroa.enrique.reproducers.util.AppPreferences
import figueroa.enrique.reproducers.util.ArtworkResolver
import figueroa.enrique.reproducers.util.CoverCarousel
import figueroa.enrique.reproducers.util.InfoDialogHelper
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    var musicService: MusicService? = null
    private var bound = false
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var repo: MusicRepository

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            bound = true
            musicService?.setOnCurrentSongChangedListener { updateMiniPlayer() }
            updateMiniPlayer()
            startMiniPlayerUpdates()
        }
        override fun onServiceDisconnected(name: ComponentName?) { bound = false }
    }

    @OptIn(UnstableApi::class)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        binding.toolbar.menu.findItem(R.id.action_settings)?.icon?.let {
            it.mutate()
            it.setTint(ContextCompat.getColor(this, R.color.iconTint))
        }

        repo = MusicRepository(AppDatabase.getDatabase(this))

        requestNeededPermissions()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, SongsFragment())
                .commit()
        }

        findViewById<View>(R.id.miniPlayerRoot).visibility = View.VISIBLE

        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_settings) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, SettingsFragment())
                    .addToBackStack(null)
                    .commit()
                true
            } else false
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_songs -> SongsFragment()
                R.id.nav_albums -> AlbumsFragment()
                R.id.nav_artists -> ArtistsFragment()
                R.id.nav_playlists -> PlaylistsFragment()
                R.id.nav_favorites -> FavoritesFragment()
                //R.id.nav_settings -> SettingsFragment()
                else -> SongsFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
            true
        }

        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_settings) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, SettingsFragment())
                    .addToBackStack("settings")
                    .commit()
                true
            } else false
        }

// Escucha cambios en el backstack para ocultar/mostrar el botón de configuración
        supportFragmentManager.addOnBackStackChangedListener {
            val isInSettings = supportFragmentManager.backStackEntryCount > 0
            binding.toolbar.menu.findItem(R.id.action_settings)?.isVisible = !isInSettings

            binding.toolbar.navigationIcon = if (isInSettings) {
                ContextCompat.getDrawable(this, R.drawable.ic_back)?.apply {
                    mutate()
                    setTint(ContextCompat.getColor(this@MainActivity, R.color.iconTint))
                }
            } else null
            binding.toolbar.setNavigationOnClickListener {
                if (isInSettings) supportFragmentManager.popBackStack()
            }
            binding.bottomNav.visibility = if (isInSettings) View.GONE else View.VISIBLE
        }

        // --- Mini player ---
        binding.miniPlayerRoot.miniPlayPause.setOnClickListener {
            musicService?.togglePlayPause()
            updateMiniPlayer()
        }
        binding.miniPlayerRoot.miniNext.setOnClickListener {
            musicService?.next()
            updateMiniPlayer()
        }
        binding.miniPlayerRoot.miniPrev.setOnClickListener {
            musicService?.previous()
            updateMiniPlayer()
        }
        binding.miniPlayerRoot.root.setOnClickListener {
            if (musicService?.currentPlaylist?.isNotEmpty() == true) {
                startActivity(Intent(this, PlayerActivity::class.java))
            }
        }
    }

    private fun updateMiniPlayer() {
        val service = musicService ?: return
        val song = service.currentPlaylist.getOrNull(service.currentIndex)

        if (song == null) {
            binding.miniPlayerRoot.root.visibility = View.GONE
            CoverCarousel.stop(binding.miniPlayerRoot.miniCover)
            return
        }

        binding.miniPlayerRoot.root.visibility = View.VISIBLE
        binding.miniPlayerRoot.miniTitle.text = song.title
        binding.miniPlayerRoot.miniArtist.text = song.artist

        CoroutineScope(Dispatchers.Main).launch {
            val sources = withContext(Dispatchers.IO) {
                ArtworkResolver.sourcesFor(this@MainActivity, song, service)
            }
            if (AppPreferences.isAppCarouselEnabled(this@MainActivity) && sources.size > 1) {
                CoverCarousel.start(binding.miniPlayerRoot.miniCover, sources)
            } else {
                CoverCarousel.applyStatic(binding.miniPlayerRoot.miniCover, sources)
            }
        }

        binding.miniPlayerRoot.miniPlayPause.setImageResource(
            if (service.isPlaying()) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    private fun startMiniPlayerUpdates() {
        handler.post(object : Runnable {
            override fun run() {
                updateMiniPlayer()
                handler.postDelayed(this, 1000)
            }
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
        musicService?.setOnCurrentSongChangedListener(null)
        if (bound) { unbindService(connection); bound = false }
        handler.removeCallbacksAndMessages(null)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNeededPermissions() {
        val perms = mutableListOf(Manifest.permission.POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= 33) {
            perms.add(Manifest.permission.READ_MEDIA_AUDIO)
            perms.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        ActivityCompat.requestPermissions(this, perms.toTypedArray(), 100)
    }
}
