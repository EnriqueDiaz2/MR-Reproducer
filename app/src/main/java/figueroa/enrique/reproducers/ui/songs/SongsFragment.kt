package figueroa.enrique.reproducers.ui.songs

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import figueroa.enrique.reproducers.R
import figueroa.enrique.reproducers.ui.main.MainActivity
import figueroa.enrique.reproducers.data.db.AppDatabase
import figueroa.enrique.reproducers.data.repository.MusicRepository
import figueroa.enrique.reproducers.databinding.FragmentSongsBinding
import figueroa.enrique.reproducers.service.MusicService
import figueroa.enrique.reproducers.ui.player.PlayerActivity
import kotlinx.coroutines.*

class SongsFragment : Fragment() {
    private var _binding: FragmentSongsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: SongAdapter
    private lateinit var viewModel: SongsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSongsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val repo = MusicRepository(AppDatabase.getDatabase(requireContext()))
        viewModel = ViewModelProvider(this, SongsViewModelFactory(repo))[SongsViewModel::class.java]

        adapter = SongAdapter(
            onClick = { song, index ->
                val service = (activity as? MainActivity)?.musicService
                if (service == null) {
                    ContextCompat.startForegroundService(requireContext(),
                        Intent(requireContext(),
                            MusicService::class.java)
                    )

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.player_not_ready),
                        Toast.LENGTH_SHORT).show()

                    return@SongAdapter
                }

                val list = viewModel.songs.value ?: listOf()
                service.playSong(song, list, index)
                startActivity(Intent(requireContext(),
                    PlayerActivity::class.java)
                )

                /*val service = (activity as? MainActivity)?.musicService
                if (service == null) {
                    android.widget.Toast.makeText(requireContext(), getString(R.string.player_not_ready), android.widget.Toast.LENGTH_SHORT).show()
                    return@SongAdapter
                }
                val list = viewModel.songs.value ?: listOf()
                ContextCompat.startForegroundService(
                    requireContext(),
                    android.content.Intent(requireContext(), figueroa.enrique.reproducers.service.MusicService::class.java)
                )
                service.playSong(song, list, index)
                startActivity(android.content.Intent(requireContext(), PlayerActivity::class.java))*/
            },
            onFavoriteClick = { song -> viewModel.toggleFavorite(song) },
            onMoreClick = { song -> showSongOptionsDialog(song) }
            //onMoreClick = { song -> showAddToPlaylistDialog(song) }
        )

        binding.recyclerSongs.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSongs.adapter = adapter

        viewModel.songs.observe(viewLifecycleOwner) { songs ->
            adapter.submitList(songs)
        }
    }

    private fun showAddToPlaylistDialog(song: figueroa.enrique.reproducers.data.model.Song) {
        val repo = MusicRepository(AppDatabase.getDatabase(requireContext()))
        repo.allPlaylists.observe(viewLifecycleOwner) { playlists ->
            if (playlists.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.first_create_playlist), Toast.LENGTH_SHORT).show()
                return@observe
            }
            val names = playlists.map { it.name }.toTypedArray()
            android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.add_to_playlist)
                .setItems(names) { _, which ->
                    val playlist = playlists[which]
                    CoroutineScope(Dispatchers.IO).launch {
                        repo.addSongToPlaylist(playlist.id, song.id)
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun showSongOptionsDialog(song: figueroa.enrique.reproducers.data.model.Song) {
        val options = arrayOf(getString(R.string.edit_song), getString(R.string.add_to_playlist_option))
        android.app.AlertDialog.Builder(requireContext())
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val repo = MusicRepository(AppDatabase.getDatabase(requireContext()))
                        figueroa.enrique.reproducers.util.EditSongDialog.show(requireContext(), song, repo) {
                            Toast.makeText(requireContext(), getString(R.string.song_updated), Toast.LENGTH_SHORT).show()
                        }
                    }
                    1 -> showAddToPlaylistDialog(song)
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
