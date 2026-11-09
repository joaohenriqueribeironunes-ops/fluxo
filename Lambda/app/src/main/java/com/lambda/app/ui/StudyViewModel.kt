package com.lambda.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lambda.dataacademic.dao.AcademicDao
import com.lambda.dataacademic.entity.FraseExplicativa
import com.lambda.dataacademic.entity.Etimologia
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudyUiState(
    val query: String = "",
    val frases: List<FraseExplicativa> = emptyList(),
    val etimologias: List<Etimologia> = emptyList(),
    val loading: Boolean = false,
    val fraseCount: Int? = null,
    val etimCount: Int? = null,
)

@HiltViewModel
class StudyViewModel @Inject constructor(private val dao: AcademicDao) : ViewModel() {
    private val _state = MutableStateFlow(StudyUiState())
    val state: StateFlow<StudyUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                fraseCount = runCatching { dao.countFrases() }.getOrNull(),
                etimCount = runCatching { dao.countEtimologias() }.getOrNull(),
            )
        }
    }

    fun onQuery(v: String) { _state.value = _state.value.copy(query = v) }
    fun search() {
        val q = state.value.query.trim(); if (q.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val sanitized = Regex("[\\p{L}\\p{N}]+").findAll(q).map { it.value }.filter { it.length >= 2 }.joinToString(" ") { "\"$it\"" }
            val frases = runCatching { if (sanitized.isNotBlank()) dao.searchFrasesFts(sanitized, 20) else emptyList() }.getOrDefault(emptyList())
            val et = runCatching { dao.searchEtimologia(q, 15) }.getOrDefault(emptyList())
            _state.value = _state.value.copy(frases = frases, etimologias = et, loading = false)
        }
    }
}
