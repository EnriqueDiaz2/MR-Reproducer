package figueroa.enrique.reproducers.ui.playlist

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import figueroa.enrique.reproducers.R
import figueroa.enrique.reproducers.data.db.AppDatabase
import figueroa.enrique.reproducers.data.repository.MusicRepository
import figueroa.enrique.reproducers.databinding.FragmentPlaylistsBinding
import kotlinx.coroutines.*

class PlaylistsFragment : Fragment() {
    private var _binding: FragmentPlaylistsBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: MusicRepository

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlaylistsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        repo = MusicRepository(AppDatabase.getDatabase(requireContext()))

        val adapter = PlaylistAdapter { playlist ->
            startActivity(
                Intent(requireContext(), PlaylistDetailActivity::class.java)
                    .putExtra("playlistId", playlist.id)
                    .putExtra("playlistName", playlist.name)
            )
        }
        binding.recyclerPlaylists.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPlaylists.adapter = adapter
        repo.allPlaylists.observe(viewLifecycleOwner) { adapter.submitList(it) }

        binding.fabAddPlaylist.setOnClickListener {
            val input = EditText(requireContext()).apply { hint = getString(R.string.playlist_name_hint) }
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.new_playlist_title)
                .setView(input)
                .setPositiveButton(R.string.create) { _, _ ->
                    val name = input.text.toString().trim()
                    if (name.isNotEmpty()) {
                        CoroutineScope(Dispatchers.IO).launch { repo.createPlaylist(name) }
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
