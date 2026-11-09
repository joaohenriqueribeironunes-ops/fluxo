package com.lambda.app

import com.lambda.databehavior.entity.PerfilUsuario
import com.lambda.databehavior.entity.RespostaRelacionamento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Smoke: entidades do behavior.db têm metadados e perfil do usuário sem campos clínicos. */
class BehaviorEntitySmokeTest {
    @Test
    fun respostaTemMetadadosPadrao() {
        val r = RespostaRelacionamento(
            relacaoTipo = "amizade",
            subtema = "limites",
            perfilNdOuNt = "ND",
            texto = "Posso dizer: preciso de um tempo.",
            tom = "caloroso",
            tamanho = "curto",
            gatilhoDeUso = "convite inesperado",
            tags = "limites,autonomia",
        )
        assertEquals("seed", r.fonte)
        assertFalse(r.revisado)
        assertEquals(0.5, r.nivelConfianca, 0.0001)
    }

    @Test
    fun perfilUsuarioSemCamposClinicos() {
        val p = PerfilUsuario(preferenciasSensoriais = "luz baixa", energiaSocialAtual = "média")
        assertEquals(1, p.id)
        assertEquals("luz baixa", p.preferenciasSensoriais)
    }
}
