package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Script
import com.example.data.ScriptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TeleprompterViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ScriptRepository(database.scriptDao())

    val allScripts: StateFlow<List<Script>> = repository.allScripts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Form Editor State
    private val _currentId = MutableStateFlow<Int?>(null)
    val currentId: StateFlow<Int?> = _currentId.asStateFlow()

    private val _editTitle = MutableStateFlow("")
    val editTitle: StateFlow<String> = _editTitle.asStateFlow()

    private val _editContent = MutableStateFlow("")
    val editContent: StateFlow<String> = _editContent.asStateFlow()

    // Teleprompter Active Play Session Variables
    private val _scrollSpeed = MutableStateFlow(3.0f) // 1.0 to 10.0
    val scrollSpeed: StateFlow<Float> = _scrollSpeed.asStateFlow()

    private val _fontSize = MutableStateFlow(24.0f) // 14 to 56
    val fontSize: StateFlow<Float> = _fontSize.asStateFlow()

    private val _isMirrored = MutableStateFlow(false)
    val isMirrored: StateFlow<Boolean> = _isMirrored.asStateFlow()

    private val _alignment = MutableStateFlow("LEFT") // "LEFT", "CENTER"
    val alignment: StateFlow<String> = _alignment.asStateFlow()

    private val _highlightActive = MutableStateFlow(true)
    val highlightActive: StateFlow<Boolean> = _highlightActive.asStateFlow()

    private val _themeName = MutableStateFlow("YELLOW_ON_BLACK") // "WHITE_ON_BLACK", "YELLOW_ON_BLACK", "BLACK_ON_WHITE"
    val themeName: StateFlow<String> = _themeName.asStateFlow()

    private val _countdownTime = MutableStateFlow(3) // 0, 3, 5
    val countdownTime: StateFlow<Int> = _countdownTime.asStateFlow()

    private val _inPrompterMode = MutableStateFlow(false)
    val inPrompterMode: StateFlow<Boolean> = _inPrompterMode.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    init {
        // Preload default scripts if none exist using a one-time check
        viewModelScope.launch {
            val list = repository.allScripts.first()
            if (list.isEmpty()) {
                repository.insert(
                    Script(
                        title = "Latihan: Metode 3 Detik Redakan Grogi",
                        content = "Halo semua! Sebelum memulai presentasi, tarik napas dalam-dalam.\n\nTahan selama tiga detik.\n\nSatu...\n\nDua...\n\nTiga...\n\nlalu hembuskan perlahan.\n\nTrik sederhana ini terbukti menurunkan denyut jantung secara instan.\n\nSekarang, perhatikan audiens Anda, tersenyumlah dengan hangat, dan mulailah berbicara dengan tempo yang tenang.\n\nAnda siap memberikan impresi terbaik!",
                        scrollSpeed = 3.5f,
                        fontSize = 32f,
                        alignment = "CENTER"
                    )
                )
                repository.insert(
                    Script(
                        title = "Pidato Utama: Masa Depan Ketahanan Bangsa",
                        content = "Selamat pagi dan salam sejahtera untuk kita semua.\n\nHari ini adalah momentum bersejarah bagi kita untuk merefleksikan kembali nilai-nilai luhur kebersamaan, kejujuran, dan kegotongroyongan.\n\nDi tengah tantangan era digital global yang bergerak begitu dinamis, ketahanan bangsa tidak lagi hanya diukur dari kekuatan fisik, melainkan dari kedalaman persatuan dan ketangguhan karakter generasi mudanya.\n\nMari kita jadikan wadah ini sebagai landasan kolaborasi konkret demi masa depan yang lebih kokoh dan berdaulat.",
                        scrollSpeed = 2.5f,
                        fontSize = 26f,
                        alignment = "LEFT"
                    )
                )
            }
        }
    }

    fun setEditTitle(title: String) {
        _editTitle.value = title
    }

    fun setEditContent(content: String) {
        _editContent.value = content
    }

    fun setScrollSpeed(speed: Float) {
        _scrollSpeed.value = speed.coerceIn(1.0f, 10.0f)
    }

    fun adjustScrollSpeed(delta: Float) {
        setScrollSpeed(_scrollSpeed.value + delta)
    }

    fun setFontSize(size: Float) {
        _fontSize.value = size.coerceIn(14.0f, 56.0f)
    }

    fun adjustFontSize(delta: Float) {
        setFontSize(_fontSize.value + delta)
    }

    fun toggleMirror() {
        _isMirrored.value = !_isMirrored.value
    }

    fun setAlignment(align: String) {
        _alignment.value = align
    }

    fun toggleAlignment() {
        _alignment.value = if (_alignment.value == "LEFT") "CENTER" else "LEFT"
    }

    fun toggleHighlight() {
        _highlightActive.value = !_highlightActive.value
    }

    fun setThemeName(theme: String) {
        _themeName.value = theme
    }

    fun setCountdownTime(seconds: Int) {
        _countdownTime.value = seconds.coerceIn(0, 10)
    }

    fun selectScript(script: Script) {
        _currentId.value = script.id
        _editTitle.value = script.title
        _editContent.value = script.content
        _scrollSpeed.value = script.scrollSpeed
        _fontSize.value = script.fontSize
        _isMirrored.value = script.isMirrored
        _alignment.value = script.alignment
    }

    fun clearEditor() {
        _currentId.value = null
        _editTitle.value = ""
        _editContent.value = ""
        _scrollSpeed.value = 3.0f
        _fontSize.value = 24.0f
        _isMirrored.value = false
        _alignment.value = "LEFT"
    }

    fun saveScript() {
        val title = _editTitle.value.trim()
        val content = _editContent.value.trim()
        if (title.isEmpty() || content.isEmpty()) return

        val scriptToSave = Script(
            id = _currentId.value ?: 0,
            title = title,
            content = content,
            lastModified = System.currentTimeMillis(),
            scrollSpeed = _scrollSpeed.value,
            fontSize = _fontSize.value,
            isMirrored = _isMirrored.value,
            alignment = _alignment.value
        )

        viewModelScope.launch {
            val savedId = repository.insert(scriptToSave)
            if (_currentId.value == null) {
                _currentId.value = savedId.toInt()
            }
        }
    }

    fun deleteScriptById(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
            if (_currentId.value == id) {
                clearEditor()
            }
        }
    }

    fun startPrompter() {
        saveScript() // Auto-save current state before running
        _inPrompterMode.value = true
        _isPlaying.value = false
    }

    fun stopPrompter() {
        _inPrompterMode.value = false
        _isPlaying.value = false
    }

    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
    }

    fun pausePrompter() {
        _isPlaying.value = false
    }
}
