package figueroa.enrique.reproducers.ui.album

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import figueroa.enrique.reproducers.R
import figueroa.enrique.reproducers.data.model.Album
import figueroa.enrique.reproducers.databinding.ItemAlbumBinding

class AlbumAdapter(
    private val onClick: (Album) -> Unit,
    private val onEditCover: (Album) -> Unit
) : ListAdapter<Album, AlbumAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemAlbumBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val album = getItem(position)
        holder.binding.albumName.text = album.name
        holder.binding.albumArtist.text = album.artist

        Glide.with(holder.itemView)
            .load(album.coverImagePath)
            .placeholder(R.drawable.ic_album)
            .into(holder.binding.albumCover)

        //holder.binding.albumCover.setImageResource(R.drawable.ic_album)

        holder.binding.root.setOnClickListener { onClick(album) }
        holder.binding.btnEditCover.setOnClickListener { onEditCover(album) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Album>() {
            override fun areItemsTheSame(a: Album, b: Album) = a.id == b.id
            override fun areContentsTheSame(a: Album, b: Album) = a == b
        }
    }
}
