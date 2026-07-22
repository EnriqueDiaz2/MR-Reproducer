package figueroa.enrique.reproducers.ui.main

import android.content.*
import android.graphics.Bitmap
import android.os.*
import android.view.View
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.Glide
import figueroa.enrique.reproducers.data.db.AppDatabase
import figueroa.enrique.reproducers.data.repository.MusicRepository
import figueroa.enrique.reproducers.databinding.ActivityMainBinding
import figueroa.enrique.reproducers.service.MusicService
import figueroa.enrique.reproducers.ui.player.PlayerActivity
import figueroa.enrique.reproducers.ui.songs.SongsFragment
import figueroa.enrique.reproducers.R
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
                    .replace(R.id.fragmentContainer, figueroa.enrique.reproducers.ui.settings.SettingsFragment())
                    .addToBackStack(null)
                    .commit()
                true
            } else false
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_songs -> SongsFragment()
                R.id.nav_albums -> figueroa.enrique.reproducers.ui.album.AlbumsFragment()
                R.id.nav_artists -> figueroa.enrique.reproducers.ui.artist.ArtistsFragment()
                R.id.nav_playlists -> figueroa.enrique.reproducers.ui.playlist.PlaylistsFragment()
                R.id.nav_favorites -> figueroa.enrique.reproducers.ui.favorites.FavoritesFragment()
                //R.id.nav_settings -> figueroa.enrique.reproducers.ui.settings.SettingsFragment()
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
                    .replace(R.id.fragmentContainer, figueroa.enrique.reproducers.ui.settings.SettingsFragment())
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
                ContextCompat.getDrawable(this, R.drawable.ic_back)
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
            return
        }

        binding.miniPlayerRoot.root.visibility = View.VISIBLE
        binding.miniPlayerRoot.miniTitle.text = song.title
        binding.miniPlayerRoot.miniArtist.text = song.artist

        CoroutineScope(Dispatchers.Main).launch {
            val artwork = withContext(Dispatchers.IO) { loadSafeArtwork(song) }

            if (artwork != null) {
                binding.miniPlayerRoot.miniCover.imageTintList = null
                binding.miniPlayerRoot.miniCover.setImageBitmap(artwork)
            } else {
                binding.miniPlayerRoot.miniCover.imageTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.iconTint)
                binding.miniPlayerRoot.miniCover.setImageResource(R.drawable.ic_music_note)
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
        //ContextCompat.startForegroundService(this, Intent(this, MusicService::class.java))
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

    private suspend fun loadSafeArtwork(song: figueroa.enrique.reproducers.data.model.Song): Bitmap? {
        val albumCoverPath = repo.albumById(song.albumId)?.coverImagePath
        val albumCover = albumCoverPath?.let { path ->
            try {
                Glide.with(this@MainActivity)
                    .asBitmap()
                    .load(path)
                    .submit()
                    .get()
            } catch (_: Exception) {
                null
            }
        }

        if (albumCover != null && !isLikelyBlankArtwork(albumCover)) {
            return albumCover
        }

        val embedded = serviceOrNull()?.loadArtwork(song)
        if (embedded != null && !isLikelyBlankArtwork(embedded)) {
            return embedded
        }

        return null
    }

    private fun serviceOrNull(): MusicService? = musicService

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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNeededPermissions() {
        val perms = mutableListOf(android.Manifest.permission.POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= 33) {
            perms.add(android.Manifest.permission.READ_MEDIA_AUDIO)
            perms.add(android.Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            perms.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        androidx.core.app.ActivityCompat.requestPermissions(this, perms.toTypedArray(), 100)
    }
}
