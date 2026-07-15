package figueroa.enrique.reproducers.ui.album

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import figueroa.enrique.reproducers.data.db.AppDatabase
import figueroa.enrique.reproducers.data.model.Album
import figueroa.enrique.reproducers.data.repository.MusicRepository
import figueroa.enrique.reproducers.databinding.FragmentAlbumsBinding
import kotlinx.coroutines.*

class AlbumsFragment : Fragment() {
    private var _binding: FragmentAlbumsBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: MusicRepository
    private var albumToEdit: Album? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let { imageUri ->
                requireContext().contentResolver.takePersistableUriPermission(
                    imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                albumToEdit?.let { album ->
                    CoroutineScope(Dispatchers.IO).launch {
                        repo.updateAlbumCover(album.id, imageUri.toString())
                    }
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAlbumsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        repo = MusicRepository(AppDatabase.getDatabase(requireContext()))

        val adapter = AlbumAdapter(
            onClick = { album ->
                startActivity(
                    Intent(requireContext(), figueroa.enrique.reproducers.ui.album.AlbumDetailActivity::class.java)
                        .putExtra("albumId", album.id)
                        .putExtra("albumName", album.name)
                        .putExtra("albumArtist", album.artist)
                )
            },
            onEditCover = { album ->
                albumToEdit = album
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "image/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                pickImageLauncher.launch(intent)
            }
        )

        binding.recyclerAlbums.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerAlbums.adapter = adapter

        repo.allAlbums.observe(viewLifecycleOwner) { adapter.submitList(it) }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
