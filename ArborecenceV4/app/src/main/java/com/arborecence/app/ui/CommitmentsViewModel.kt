package com.arborecence.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arborecence.databehavior.dao.BehaviorDao
import com.arborecence.databehavior.entity.Compromisso
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommitmentsViewModel @Inject constructor(private val dao: BehaviorDao) : ViewModel() {
    private val _items = MutableStateFlow<List<Compromisso>>(emptyList())
    val items: StateFlow<List<Compromisso>> = _items.asStateFlow()

    fun load() { viewModelScope.launch { _items.value = runCatching { dao.listCompromissos() }.getOrDefault(emptyList()) } }

    fun add(titulo: String, descricao: String, dataHoraMillis: Long?, local: String) {
        if (titulo.isBlank()) return
        viewModelScope.launch {
            dao.upsertCompromisso(
                Compromisso(
                    titulo = titulo.trim(), descricao = descricao.trim(),
                    dataHoraMillis = dataHoraMillis, local = local.trim(),
                    dataGeracao = System.currentTimeMillis(),
                )
            )
            load()
        }
    }

    fun toggle(id: Long, done: Boolean) { viewModelScope.launch { dao.setCompromissoConcluido(id, done); load() } }
    fun delete(id: Long) { viewModelScope.launch { dao.deleteCompromisso(id); load() } }
}
