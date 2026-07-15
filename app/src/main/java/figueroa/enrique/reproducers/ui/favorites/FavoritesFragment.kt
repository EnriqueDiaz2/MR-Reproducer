package figueroa.enrique.reproducers.ui.favorites

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import figueroa.enrique.reproducers.ui.main.MainActivity
import figueroa.enrique.reproducers.data.db.AppDatabase
import figueroa.enrique.reproducers.data.repository.MusicRepository
import figueroa.enrique.reproducers.databinding.FragmentFavoritesBinding
import figueroa.enrique.reproducers.ui.player.PlayerActivity
import figueroa.enrique.reproducers.ui.songs.SongAdapter
import kotlinx.coroutines.*

class FavoritesFragment : Fragment() {
    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: MusicRepository

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        repo = MusicRepository(AppDatabase.getDatabase(requireContext()))

        val adapter = SongAdapter(
            onClick = { song, _ ->
                val list = repo.favorites.value ?: listOf()
                val index = list.indexOf(song)
                (activity as? MainActivity)?.musicService?.playSong(song, list, index)
                startActivity(Intent(requireContext(), PlayerActivity::class.java))
            },
            onFavoriteClick = { song -> CoroutineScope(Dispatchers.IO).launch { repo.toggleFavorite(song) } },
            onMoreClick = { song ->
                figueroa.enrique.reproducers.util.EditSongDialog.show(requireContext(), song, repo) {
                    android.widget.Toast.makeText(requireContext(), "Canción actualizada", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )

        binding.recyclerFavorites.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerFavorites.adapter = adapter
        repo.favorites.observe(viewLifecycleOwner) { adapter.submitList(it) }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
