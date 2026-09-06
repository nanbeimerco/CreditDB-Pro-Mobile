package com.creditdb.pro.ui.predict

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.creditdb.pro.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class PredictUiState(
    val title: String = "新規企画アニメ",
    val year: Int = 2024,
    val staffMap: Map<String, List<String>> = mapOf(
        "director" to emptyList(),
        "series_comp" to emptyList(),
        "char_design" to emptyList(),
        "sakkan" to emptyList(),
        "genga" to emptyList(),
        "unit_director" to emptyList(),
        "music" to emptyList(),
        "art_dir" to emptyList(),
        "cv" to emptyList()
    ),
    val predictionResult: PredictionResult? = null,
    val isPredicting: Boolean = false,
    val candidates: List<StaffCandidate> = emptyList(),
    val candidateRole: String = "director",
    val showCandidateDialog: Boolean = false,
    val presets: List<PredictionPreset> = emptyList()
)

class PredictViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CreditRepository(application)
    private val prefs = application.getSharedPreferences("predict_presets", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow(PredictUiState())
    val uiState: StateFlow<PredictUiState> = _uiState.asStateFlow()

    init {
        loadPresets()
        runPrediction()
    }

    private fun loadPresets() {
        val savedJson = prefs.getString("user_presets", null)
        val userList = if (!savedJson.isNullOrBlank()) {
            try {
                json.decodeFromString<List<PredictionPreset>>(savedJson)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        _uiState.update { it.copy(presets = userList) }
    }

    fun saveCurrentAsPreset(name: String) {
        val current = _uiState.value
        val newPreset = PredictionPreset(
            id = System.currentTimeMillis().toString(),
            name = name,
            year = current.year,
            director = current.staffMap["director"] ?: emptyList(),
            seriesComp = current.staffMap["series_comp"] ?: emptyList(),
            charDesign = current.staffMap["char_design"] ?: emptyList(),
            sakkan = current.staffMap["sakkan"] ?: emptyList(),
            genga = current.staffMap["genga"] ?: emptyList(),
            unitDirector = current.staffMap["unit_director"] ?: emptyList(),
            music = current.staffMap["music"] ?: emptyList(),
            artDir = current.staffMap["art_dir"] ?: emptyList(),
            cv = current.staffMap["cv"] ?: emptyList()
        )
        val updated = _uiState.value.presets + newPreset
        _uiState.update { it.copy(presets = updated) }
        prefs.edit().putString("user_presets", json.encodeToString(updated)).apply()
    }

    fun deletePreset(presetId: String) {
        val updated = _uiState.value.presets.filterNot { it.id == presetId }
        _uiState.update { it.copy(presets = updated) }
        prefs.edit().putString("user_presets", json.encodeToString(updated)).apply()
    }

    fun applyPreset(preset: PredictionPreset) {
        val newMap = mapOf(
            "director" to preset.director,
            "series_comp" to preset.seriesComp,
            "char_design" to preset.charDesign,
            "sakkan" to preset.sakkan,
            "genga" to preset.genga,
            "unit_director" to preset.unitDirector,
            "music" to preset.music,
            "art_dir" to preset.artDir,
            "cv" to preset.cv
        )
        _uiState.update {
            it.copy(
                title = preset.name,
                year = preset.year,
                staffMap = newMap
            )
        }
        runPrediction()
    }

    fun clearAllStaff() {
        val emptyMap = mapOf(
            "director" to emptyList<String>(),
            "series_comp" to emptyList<String>(),
            "char_design" to emptyList<String>(),
            "sakkan" to emptyList<String>(),
            "genga" to emptyList<String>(),
            "unit_director" to emptyList<String>(),
            "music" to emptyList<String>(),
            "art_dir" to emptyList<String>(),
            "cv" to emptyList<String>()
        )
        _uiState.update {
            it.copy(
                title = "新規企画アニメ",
                staffMap = emptyMap
            )
        }
        runPrediction()
    }

    fun setTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun setYear(year: Int) {
        _uiState.update { it.copy(year = year) }
        runPrediction()
    }

    fun addStaffMember(role: String, name: String) {
        val currentList = _uiState.value.staffMap[role] ?: emptyList()
        if (currentList.contains(name)) return
        val newMap = _uiState.value.staffMap.toMutableMap()
        newMap[role] = currentList + name
        _uiState.update { it.copy(staffMap = newMap, showCandidateDialog = false) }
        runPrediction()
    }

    fun removeStaffMember(role: String, name: String) {
        val currentList = _uiState.value.staffMap[role] ?: emptyList()
        val newMap = _uiState.value.staffMap.toMutableMap()
        newMap[role] = currentList - name
        _uiState.update { it.copy(staffMap = newMap) }
        runPrediction()
    }

    fun openCandidatePicker(role: String) {
        _uiState.update { it.copy(candidateRole = role, showCandidateDialog = true) }
        searchCandidates(role, "")
    }

    fun closeCandidatePicker() {
        _uiState.update { it.copy(showCandidateDialog = false) }
    }

    fun searchCandidates(role: String, query: String) {
        viewModelScope.launch {
            val list = repository.getStaffCandidates(role, query, limit = 20)
            _uiState.update { it.copy(candidates = list) }
        }
    }

    fun runPrediction() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPredicting = true) }
            val state = _uiState.value
            val res = repository.predictQuality(
                title = state.title,
                year = state.year,
                staffMap = state.staffMap
            )
            _uiState.update { it.copy(predictionResult = res, isPredicting = false) }
        }
    }
}
