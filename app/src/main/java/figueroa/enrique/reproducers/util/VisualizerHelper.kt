package figueroa.enrique.reproducers.util

import android.media.audiofx.Visualizer
import android.util.Log

class VisualizerHelper {
    private var visualizer: Visualizer? = null
    private var isEnabled = false

    fun start(
        audioSessionId: Int,
        onFftData: ((ByteArray) -> Unit)? = null,
        onWaveformData: ((ByteArray) -> Unit)? = null
    ) {
        if (audioSessionId <= 0) return
        stop()

        try {
            val vis = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (waveform != null && isEnabled) {
                                onWaveformData?.invoke(waveform)
                            }
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (fft != null && isEnabled) {
                                onFftData?.invoke(fft)
                            }
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    onWaveformData != null,
                    onFftData != null
                )
                enabled = true
            }
            visualizer = vis
            isEnabled = true
        } catch (e: Exception) {
            Log.e("VisualizerHelper", "Error al iniciar Visualizer: ${e.message}", e)
            stop()
        }
    }

    fun stop() {
        isEnabled = false
        try {
            visualizer?.apply {
                enabled = false
                release()
            }
        } catch (e: Exception) {
            Log.e("VisualizerHelper", "Error al detener Visualizer: ${e.message}", e)
        } finally {
            visualizer = null
        }
    }
}
