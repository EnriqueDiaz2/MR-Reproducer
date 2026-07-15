package figueroa.enrique.reproducers.ui.artist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import figueroa.enrique.reproducers.R
import figueroa.enrique.reproducers.data.model.Artist
import figueroa.enrique.reproducers.databinding.ItemArtistBinding

class ArtistAdapter(private val onClick: (Artist) -> Unit) :
    ListAdapter<Artist, ArtistAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemArtistBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemArtistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val artist = getItem(position)
        holder.binding.artistName.text = artist.name
        Glide.with(holder.itemView)
            .load(artist.imagePath)
            .placeholder(R.drawable.ic_artist)
            .circleCrop()
            .into(holder.binding.artistImage)

        //holder.binding.artistImage.setImageResource(R.drawable.ic_artist)

        holder.binding.root.setOnClickListener { onClick(artist) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Artist>() {
            override fun areItemsTheSame(a: Artist, b: Artist) = a.id == b.id
            override fun areContentsTheSame(a: Artist, b: Artist) = a == b
        }
    }
}
