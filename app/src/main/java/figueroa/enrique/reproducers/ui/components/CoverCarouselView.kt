package figueroa.enrique.reproducers.ui.components

import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import figueroa.enrique.reproducers.util.AppPreferences
import figueroa.enrique.reproducers.util.CoverCarousel

@Composable
fun CoverCarouselView(
    sources: List<Any>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { imageView ->
            if (AppPreferences.isAppCarouselEnabled(context) && sources.size > 1) {
                CoverCarousel.start(imageView, sources)
            } else {
                CoverCarousel.applyStatic(imageView, sources)
            }
        },
        modifier = modifier
    )
}
