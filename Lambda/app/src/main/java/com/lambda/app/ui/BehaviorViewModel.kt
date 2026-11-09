package com.lambda.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lambda.aiengine.AiEngine
import com.lambda.aiengine.AiRequest
import com.lambda.aiengine.AiResult
import com.lambda.databehavior.dao.BehaviorDao
import com.lambda.databehavior.entity.LeituraNd
import com.lambda.databehavior.entity.LeituraNt
import com.lambda.databehavior.entity.Ponte
import com.lambda.databehavior.entity.Situacao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SituacaoAberta(
    val situacao: Situacao,
    val nd: LeituraNd?,
    val nt: LeituraNt?,
    val ponte: Ponte?,
)

data class BehaviorUiState(
    val query: String = "",
    val visao: AiEngine.Visao = AiEngine.Visao.ND,
    val modo: AiEngine.Modo = AiEngine.Modo.PADRAO,
    val relacao: String = "amizade",
    val relacoes: List<String> = listOf("amizade","amor","família","trabalho","colegas","vizinhos","desconhecidos","atendimento"),
    val loading: Boolean = false,
    val resultNd: AiResult? = null,
    val resultNt: AiResult? = null,
    val error: String? = null,
    val dbCount: Int? = null,
    val situacoes: List<Situacao> = emptyList(),
    val situacaoAberta: SituacaoAberta? = null,
)

@HiltViewModel
class BehaviorViewModel @Inject constructor(
    private val ai: AiEngine,
    private val behaviorDao: BehaviorDao,
) : ViewModel() {
    private val _state = MutableStateFlow(BehaviorUiState())
    val state: StateFlow<BehaviorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val count = runCatching { behaviorDao.countRespostas() }.getOrNull()
            val rels = runCatching { behaviorDao.listRelacoes() }.getOrDefault(emptyList())
            _state.value = _state.value.copy(
                dbCount = count,
                relacoes = if (rels.isNotEmpty()) rels else _state.value.relacoes,
            )
            loadSituacoes(_state.value.relacao)
        }
    }

    fun onQueryChange(v: String) { _state.value = _state.value.copy(query = v) }
    fun onVisaoChange(v: AiEngine.Visao) { _state.value = _state.value.copy(visao = v) }
    fun onModoChange(v: AiEngine.Modo) { _state.value = _state.value.copy(modo = v) }

    fun onRelacaoChange(v: String) {
        _state.value = _state.value.copy(relacao = v, situacaoAberta = null)
        loadSituacoes(v)
    }

    private fun loadSituacoes(relacao: String) {
        viewModelScope.launch {
            val list = runCatching { behaviorDao.listSituacoesPorCategoria(relacao) }.getOrDefault(emptyList())
            _state.value = _state.value.copy(situacoes = list)
        }
    }

    fun abrirSituacao(id: Long) {
        viewModelScope.launch {
            val s = runCatching { behaviorDao.getSituacao(id) }.getOrNull() ?: return@launch
            val nd = runCatching { behaviorDao.getLeituraNd(id) }.getOrNull()
            val nt = runCatching { behaviorDao.getLeituraNt(id) }.getOrNull()
            val ponte = runCatching { behaviorDao.getPonte(id) }.getOrNull()
            _state.value = _state.value.copy(situacaoAberta = SituacaoAberta(s, nd, nt, ponte))
        }
    }

    fun fecharSituacao() { _state.value = _state.value.copy(situacaoAberta = null) }

    fun ask(both: Boolean = true) {
        val q = state.value.query.trim()
        if (q.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val base = AiRequest(q, state.value.visao, state.value.modo, state.value.relacao)
                if (both) {
                    val nd = ai.responder(base.copy(visao = AiEngine.Visao.ND))
                    val nt = ai.responder(base.copy(visao = AiEngine.Visao.NT))
                    _state.value = _state.value.copy(resultNd = nd, resultNt = nt, loading = false)
                } else {
                    val r = ai.responder(base)
                    _state.value = _state.value.copy(
                        resultNd = if (base.visao == AiEngine.Visao.ND) r else state.value.resultNd,
                        resultNt = if (base.visao == AiEngine.Visao.NT) r else state.value.resultNt,
                        loading = false,
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Erro ao buscar resposta.")
            }
        }
    }
}
