package figueroa.enrique.reproducers.ui.playlist

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import figueroa.enrique.reproducers.data.db.AppDatabase
import figueroa.enrique.reproducers.data.model.Song
import figueroa.enrique.reproducers.data.repository.MusicRepository
import figueroa.enrique.reproducers.databinding.ActivityDetailBinding
import figueroa.enrique.reproducers.ui.base.BasePlayerActivity
import figueroa.enrique.reproducers.ui.main.MainActivity
import figueroa.enrique.reproducers.ui.player.PlayerActivity
import figueroa.enrique.reproducers.ui.songs.SongAdapter
import figueroa.enrique.reproducers.util.EditSongDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class PlaylistDetailActivity : BasePlayerActivity() {
    private lateinit var binding: ActivityDetailBinding
    private lateinit var repo: MusicRepository
    private var currentSongs: List<Song> = emptyList()

    override fun getMiniPlayerBinding() = binding.miniPlayerRoot

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val playlistId = intent.getLongExtra("playlistId", 0)
        supportActionBar?.title = intent.getStringExtra("playlistName")
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        repo = MusicRepository(AppDatabase.getDatabase(this))

        //repo = MusicRepository(AppDatabase.getDatabase(requireContext()))

        val adapter = SongAdapter(
            onClick = { song, index ->
                musicService?.playSong(song, currentSongs, index)
                startActivity(Intent(this, PlayerActivity::class.java))
            },
            onFavoriteClick = { song -> CoroutineScope(Dispatchers.IO).launch { repo.toggleFavorite(song) } },
            onMoreClick = { song ->
                EditSongDialog.show(this, song, repo) {
                    Toast.makeText(this, getString(figueroa.enrique.reproducers.R.string.song_updated), Toast.LENGTH_SHORT).show()
                }
            },
            musicService = { musicService }
        )

        binding.recyclerDetail.layoutManager = LinearLayoutManager(this)
        binding.recyclerDetail.adapter = adapter
        repo.songsInPlaylist(playlistId).observe(this) {
            currentSongs = it
            adapter.submitList(it)
        }
    }
}
