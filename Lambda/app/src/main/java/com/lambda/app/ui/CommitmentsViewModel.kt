package com.lambda.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lambda.databehavior.dao.BehaviorDao
import com.lambda.databehavior.entity.Compromisso
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommitmentsViewModel @Inject constructor(private val dao: BehaviorDao) : ViewModel() {
    val items: StateFlow<List<Compromisso>> = dao.compromissosFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(titulo: String, descricao: String, dataHoraMillis: Long?, local: String) {
        if (titulo.isBlank()) return
        viewModelScope.launch {
            dao.upsertCompromisso(
                Compromisso(
                    titulo = titulo.trim(),
                    descricao = descricao.trim(),
                    dataHoraMillis = dataHoraMillis,
                    local = local.trim(),
                    dataGeracao = System.currentTimeMillis(),
                )
            )
        }
    }

    fun toggle(id: Long, done: Boolean) {
        viewModelScope.launch { dao.setCompromissoConcluido(id, done) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { dao.deleteCompromisso(id) }
    }
}
