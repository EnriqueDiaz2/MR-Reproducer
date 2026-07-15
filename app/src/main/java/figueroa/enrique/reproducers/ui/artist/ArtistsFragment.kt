package figueroa.enrique.reproducers.ui.artist

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import figueroa.enrique.reproducers.data.db.AppDatabase
import figueroa.enrique.reproducers.data.repository.MusicRepository
import figueroa.enrique.reproducers.databinding.FragmentArtistsBinding

class ArtistsFragment : Fragment() {
    private var _binding: FragmentArtistsBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: MusicRepository

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentArtistsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        repo = MusicRepository(AppDatabase.getDatabase(requireContext()))
        val adapter = ArtistAdapter { artist ->
            startActivity(
                Intent(requireContext(), ArtistDetailActivity::class.java)
                    .putExtra("artistName", artist.name)
            )
        }
        binding.recyclerArtists.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerArtists.adapter = adapter
        repo.allArtists.observe(viewLifecycleOwner) { adapter.submitList(it) }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
