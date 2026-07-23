package figueroa.enrique.reproducers.util

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import figueroa.enrique.reproducers.R

object CoverCarousel {
    private val handler = Handler(Looper.getMainLooper())
    private val runningTags = HashMap<ImageView, Any>()

    // sources: mezcla de rutas String (uris de álbum/artista) y Bitmap (portada embebida), sin nulos
    fun start(imageView: ImageView, sources: List<Any>, intervalMs: Long = 3500) {
        stop(imageView)

        if (sources.isEmpty()) {
            imageView.imageTintList = ContextCompat.getColorStateList(imageView.context, R.color.iconTint)
            imageView.setImageResource(R.drawable.ic_music_note)
            return
        }

        val token = Any()
        runningTags[imageView] = token
        var index = 0

        fun showAt(i: Int) {
            imageView.imageTintList = null
            when (val src = sources[i % sources.size]) {
                is Bitmap -> imageView.setImageBitmap(src)
                is String -> Glide.with(imageView)
                    .load(src)
                    .placeholder(R.drawable.ic_music_note)
                    .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(600))
                    .into(imageView)
            }
        }

        if (sources.size == 1) {
            showAt(0)
            return
        }

        val runnable = object : Runnable {
            override fun run() {
                if (runningTags[imageView] != token) return
                showAt(index)
                index++
                handler.postDelayed(this, intervalMs)
            }
        }
        handler.post(runnable)
    }

    fun stop(imageView: ImageView) {
        runningTags.remove(imageView)
    }
}
