package figueroa.enrique.reproducers.ui.favorites

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
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
    private lateinit var adapter: SongAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        repo = MusicRepository(AppDatabase.getDatabase(requireContext()))

        adapter = SongAdapter(
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
            },
            musicService = { (activity as? MainActivity)?.musicService }
        )

        binding.recyclerFavorites.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerFavorites.adapter = adapter
        repo.favorites.observe(viewLifecycleOwner) { adapter.submitList(it) }

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

    override fun onDestroyView() {
        super.onDestroyView(); _binding = null

        refreshHandler.removeCallbacksAndMessages(null)
    }
}
