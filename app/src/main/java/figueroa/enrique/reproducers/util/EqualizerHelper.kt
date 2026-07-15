package figueroa.enrique.reproducers.util

import android.media.audiofx.Equalizer

class EqualizerHelper(audioSessionId: Int) {
    private val equalizer = Equalizer(0, audioSessionId).apply { enabled = true }

    val bandCount = equalizer.numberOfBands
    val bandLevelRange: ShortArray = equalizer.bandLevelRange

    fun getBandFreq(band: Short) = equalizer.getCenterFreq(band)
    fun setBandLevel(band: Short, level: Short) = equalizer.setBandLevel(band, level)
    fun getBandLevel(band: Short) = equalizer.getBandLevel(band)
    fun release() = equalizer.release()
}
