package figueroa.enrique.reproducers.ui.artist

import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import figueroa.enrique.reproducers.data.db.AppDatabase
import figueroa.enrique.reproducers.data.model.Song
import figueroa.enrique.reproducers.data.repository.MusicRepository
import figueroa.enrique.reproducers.databinding.ActivityDetailBinding
import figueroa.enrique.reproducers.ui.base.BasePlayerActivity
import figueroa.enrique.reproducers.ui.player.PlayerActivity
import figueroa.enrique.reproducers.ui.songs.SongAdapter
import kotlinx.coroutines.*

class ArtistDetailActivity : BasePlayerActivity() {
    private lateinit var binding: ActivityDetailBinding
    private lateinit var repo: MusicRepository
    private var currentSongs: List<Song> = emptyList()

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val artistName = intent.getStringExtra("artistName") ?: ""
        supportActionBar?.title = artistName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        repo = MusicRepository(AppDatabase.getDatabase(this))

        val adapter = SongAdapter(
            onClick = { song, index ->
                musicService?.playSong(song, currentSongs, index)
                startActivity(Intent(this, PlayerActivity::class.java))
            },
            onFavoriteClick = { song -> CoroutineScope(Dispatchers.IO).launch { repo.toggleFavorite(song) } },
            onMoreClick = { song ->
                figueroa.enrique.reproducers.util.EditSongDialog.show(this, song, repo) {
                    android.widget.Toast.makeText(this, getString(figueroa.enrique.reproducers.R.string.song_updated), android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )

        binding.recyclerDetail.layoutManager = LinearLayoutManager(this)
        binding.recyclerDetail.adapter = adapter
        repo.songsByArtist(artistName).observe(this) {
            currentSongs = it
            adapter.submitList(it)
        }
    }
}
