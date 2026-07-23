package figueroa.enrique.reproducers.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatDelegate
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.core.os.LocaleListCompat
import figueroa.enrique.reproducers.R
import figueroa.enrique.reproducers.ReproducersApp
import figueroa.enrique.reproducers.ui.main.MainActivity
import figueroa.enrique.reproducers.data.db.AppDatabase
import figueroa.enrique.reproducers.util.AppPreferences
import figueroa.enrique.reproducers.util.EqualizerHelper
import figueroa.enrique.reproducers.data.model.Song
import figueroa.enrique.reproducers.data.repository.MusicRepository
import figueroa.enrique.reproducers.databinding.FragmentSettingsBinding
import kotlinx.coroutines.*

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: MusicRepository
    private var equalizerHelper: EqualizerHelper? = null
    private var languageSpinnerReady = false // evita el auto-disparo inicial
    private var appearanceSpinnerReady = false

    private val folderPicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> scanFolder(uri) }
        } else {
            Toast.makeText(requireContext(), getString(R.string.selection_cancelled), Toast.LENGTH_SHORT).show()
        }
    }

    private val filePicker = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) importFiles(uris)
        else Toast.makeText(requireContext(), getString(R.string.no_file_selected), Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        repo = MusicRepository(AppDatabase.getDatabase(requireContext()))
        setupAppearanceSpinner()

        binding.btnSelectFolder.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                folderPicker.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al abrir selector: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnImportFiles.setOnClickListener {
            try {
                filePicker.launch(arrayOf("audio/*", "video/*"))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al abrir selector: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        binding.switchAdaptiveShuffle.isChecked = AppPreferences.isAdaptiveShuffleEnabled(requireContext())
        binding.switchAdaptiveShuffle.setOnCheckedChangeListener { _, isChecked ->
            AppPreferences.setAdaptiveShuffle(requireContext(), isChecked)
        }

        setupLanguageSpinner()
        setupEqualizer()
    }

    private fun scanFolder(treeUri: Uri) {
        requireContext().contentResolver.takePersistableUriPermission(
            treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        val dir = DocumentFile.fromTreeUri(requireContext(), treeUri)
        if (dir == null) {
            Toast.makeText(requireContext(), getString(R.string.could_not_read_folder), Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val songs = mutableListOf<Song>()
            dir.listFiles().forEach { file ->
                val type = file.type ?: ""
                if (file.isFile && (type.startsWith("audio") || type.startsWith("video"))) {
                    songs.add(
                        Song(
                            title = file.name?.substringBeforeLast(".") ?: getString(R.string.unknown_value),
                            artist = getString(R.string.unknown_value),
                            album = getString(R.string.unknown_value),
                            filePath = file.uri.toString(),
                            duration = 0L,
                            isVideo = type.startsWith("video")
                        )
                    )
                }
            }
            repo.importSongsFromFolder(songs)
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "${songs.size} canciones importadas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun importFiles(uris: List<Uri>) {
        CoroutineScope(Dispatchers.IO).launch {
            val songs = uris.map { uri ->
                requireContext().contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                Song(
                    title = uri.lastPathSegment?.substringAfterLast("/") ?: getString(R.string.unknown_value),
                    artist = getString(R.string.unknown_value),
                    album = getString(R.string.unknown_value),
                    filePath = uri.toString(),
                    duration = 0L
                )
            }
            repo.importSongsFromFolder(songs)
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "${songs.size} archivos importados", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupLanguageSpinner() {
        val languages = arrayOf(getString(R.string.language_spanish), getString(R.string.language_english))
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLanguage.adapter = adapter

        // Preselecciona el idioma actual sin disparar el listener
        val currentLang = java.util.Locale.getDefault().language
        binding.spinnerLanguage.setSelection(if (currentLang == "en") 1 else 0, false)

        binding.spinnerLanguage.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!languageSpinnerReady) {
                    // Ignora el primer disparo automático del Spinner
                    languageSpinnerReady = true
                    return
                }
                val localeCode = if (position == 0) "es" else "en"
                setAppLocale(localeCode)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupAppearanceSpinner() {
        val options = arrayOf(getString(R.string.appearance_light), getString(R.string.appearance_dark))
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAppearance.adapter = adapter

        val prefs = requireContext().getSharedPreferences(ReproducersApp.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val savedMode = prefs.getString(ReproducersApp.KEY_APPEARANCE_MODE, ReproducersApp.MODE_SYSTEM)
        val currentSelection = when (savedMode) {
            ReproducersApp.MODE_DARK -> 1
            ReproducersApp.MODE_LIGHT -> 0
            else -> if ((resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES) 1 else 0
        }
        binding.spinnerAppearance.setSelection(currentSelection, false)

        binding.spinnerAppearance.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!appearanceSpinnerReady) {
                    appearanceSpinnerReady = true
                    return
                }
                val mode = if (position == 0) ReproducersApp.MODE_LIGHT else ReproducersApp.MODE_DARK
                saveAppearanceMode(mode)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun saveAppearanceMode(mode: String) {
        requireContext().getSharedPreferences(ReproducersApp.PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit()
            .putString(ReproducersApp.KEY_APPEARANCE_MODE, mode)
            .apply()

        when (mode) {
            ReproducersApp.MODE_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            ReproducersApp.MODE_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        requireActivity().recreate()
    }

    private fun setAppLocale(code: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
        requireActivity().recreate()
    }

    @OptIn(UnstableApi::class)
    private fun setupEqualizer() {
        val service = (activity as? MainActivity)?.musicService

        if (service == null) {
            binding.equalizerContainer.removeAllViews()
            val msg = android.widget.TextView(requireContext()).apply {
                text = getString(R.string.play_song_to_use_equalizer)
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
            }
            binding.equalizerContainer.addView(msg)
            return
        }

        val sessionId = service.getExoPlayer().audioSessionId
        if (sessionId == 0) {
            binding.equalizerContainer.removeAllViews()
            val msg = android.widget.TextView(requireContext()).apply {
                text = getString(R.string.play_song_to_use_equalizer)
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
            }
            binding.equalizerContainer.addView(msg)
            return
        }

        try {
            equalizerHelper = EqualizerHelper(sessionId)
            val eq = equalizerHelper ?: return

            binding.equalizerContainer.removeAllViews()
            for (band in 0 until eq.bandCount) {
                val bandShort = band.toShort()
                val seekBar = android.widget.SeekBar(requireContext()).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    ).apply { marginStart = 4; marginEnd = 4 }
                    max = (eq.bandLevelRange[1] - eq.bandLevelRange[0])
                    progress = (eq.getBandLevel(bandShort) - eq.bandLevelRange[0])
                    setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(sb: android.widget.SeekBar?, p: Int, fromUser: Boolean) {
                            if (fromUser) eq.setBandLevel(bandShort, (p + eq.bandLevelRange[0]).toShort())
                        }
                        override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
                        override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
                    })
                }
                binding.equalizerContainer.addView(seekBar)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No se pudo iniciar el ecualizador: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        equalizerHelper?.release()
        equalizerHelper = null
        super.onDestroyView()
        _binding = null
    }
}
