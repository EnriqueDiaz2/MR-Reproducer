package figueroa.enrique.reproducers.ui.album

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import figueroa.enrique.reproducers.R
import figueroa.enrique.reproducers.data.db.AppDatabase
import figueroa.enrique.reproducers.data.model.Song
import figueroa.enrique.reproducers.data.repository.MusicRepository
import figueroa.enrique.reproducers.databinding.ActivityDetailBinding
import figueroa.enrique.reproducers.ui.base.BasePlayerActivity
import figueroa.enrique.reproducers.ui.main.MainActivity
import figueroa.enrique.reproducers.ui.player.PlayerActivity
import figueroa.enrique.reproducers.ui.songs.SongAdapter
import kotlinx.coroutines.*

class AlbumDetailActivity : BasePlayerActivity() {
    private lateinit var binding: ActivityDetailBinding
    private lateinit var repo: MusicRepository
    private lateinit var adapter: SongAdapter
    private var currentSongs: List<Song> = emptyList()

    override fun getMiniPlayerBinding() = binding.miniPlayerRoot

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        //val albumId = intent.getLongExtra("albumId", 0)
        val albumName = intent.getStringExtra("albumName") ?: ""
        val albumArtist = intent.getStringExtra("albumArtist") ?: ""
        supportActionBar?.title = albumName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        repo = MusicRepository(AppDatabase.getDatabase(this))

        adapter = SongAdapter(
            onClick = { song, index ->
                musicService?.playSong(song, currentSongs, index)
                startActivity(Intent(this, PlayerActivity::class.java))
            },
            onFavoriteClick = { song -> CoroutineScope(Dispatchers.IO).launch { repo.toggleFavorite(song) } },
            onMoreClick = { song ->
                figueroa.enrique.reproducers.util.EditSongDialog.show(this, song, repo) {
                    android.widget.Toast.makeText(this, getString(R.string.song_updated), android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            musicService = { musicService }
        )

        binding.recyclerDetail.layoutManager = LinearLayoutManager(this)
        binding.recyclerDetail.adapter = adapter
        repo.songsByAlbum(albumName, albumArtist).observe(this) {
            currentSongs = it
            adapter.submitList(it)
        }

        refreshHandler.post(refreshRunnable)
    }

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        @SuppressLint("NotifyDataSetChanged")
        override fun run() {
            adapter.notifyDataSetChanged()
            refreshHandler.postDelayed(this, 1000)
        }
    }
}
