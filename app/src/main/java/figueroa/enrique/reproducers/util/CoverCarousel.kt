package figueroa.enrique.reproducers.util

import android.app.Activity
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import figueroa.enrique.reproducers.R

object CoverCarousel {
    private val handler = Handler(Looper.getMainLooper())
    private val runningTags = HashMap<ImageView, Any>()

    private fun isSafeToLoad(imageView: ImageView, token: Any): Boolean {
        if (runningTags[imageView] != token) return false
        if (!ViewCompat.isAttachedToWindow(imageView)) return false
        val activity = imageView.context as? Activity
        if (activity != null && (activity.isFinishing || activity.isDestroyed)) return false
        return true
    }

    private fun render(imageView: ImageView, src: Any) {
        try {
            imageView.imageTintList = null
            when (src) {
                is Bitmap -> imageView.setImageBitmap(src)
                is String -> Glide.with(imageView)
                    .load(src)
                    .placeholder(R.drawable.ic_music_note)
                    .transition(DrawableTransitionOptions.withCrossFade(500))
                    .into(imageView)
            }
        } catch (_: Exception) { /* Activity/vista ya no válida en ese instante */ }
    }

    // Modo animado: rota entre las imágenes disponibles
    fun start(imageView: ImageView, sources: List<Any>, intervalMs: Long = 3500) {
        if (sources.isEmpty()) { applyStatic(imageView, sources); return }
        if (sources.size == 1) { applyStatic(imageView, sources); return }

        stop(imageView)
        val token = Any()
        runningTags[imageView] = token
        var index = 0

        val runnable = object : Runnable {
            override fun run() {
                if (!isSafeToLoad(imageView, token)) return
                val src = sources[index % sources.size]
                imageView.tag = src
                render(imageView, src)
                index++
                handler.postDelayed(this, intervalMs)
            }
        }
        handler.post(runnable)
    }

    // Modo quieto: solo la primera imagen disponible, sin recargar si ya es la misma (evita parpadeo)
    fun applyStatic(imageView: ImageView, sources: List<Any>) {
        stop(imageView)
        val src = sources.firstOrNull()
        if (src == null) {
            if (imageView.tag != "empty") {
                imageView.tag = "empty"
                imageView.imageTintList = ContextCompat.getColorStateList(imageView.context, R.color.iconTint)
                imageView.setImageResource(R.drawable.ic_music_note)
            }
            return
        }
        if (imageView.tag == src) return // ya se está mostrando -> no recargar
        imageView.tag = src
        render(imageView, src)
    }

    fun stop(imageView: ImageView) {
        runningTags.remove(imageView)
    }
}
