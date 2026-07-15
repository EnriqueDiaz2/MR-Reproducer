package figueroa.enrique.reproducers.ui.playlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import figueroa.enrique.reproducers.R
import figueroa.enrique.reproducers.data.model.Playlist
import figueroa.enrique.reproducers.databinding.ItemPlaylistBinding

class PlaylistAdapter(private val onClick: (Playlist) -> Unit) :
    ListAdapter<Playlist, PlaylistAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemPlaylistBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val playlist = getItem(position)
        holder.binding.playlistName.text = playlist.name
        Glide.with(holder.itemView)
            .load(playlist.coverImagePath)
            .placeholder(R.drawable.ic_playlist)
            .into(holder.binding.playlistImage)

        //holder.binding.playlistImage.setImageResource(R.drawable.ic_playlist)

        holder.binding.root.setOnClickListener { onClick(playlist) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Playlist>() {
            override fun areItemsTheSame(a: Playlist, b: Playlist) = a.id == b.id
            override fun areContentsTheSame(a: Playlist, b: Playlist) = a == b
        }
    }
}
