package com.lambda.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambda.aiengine.AiEngine
import com.lambda.aiengine.AiResult
import com.lambda.coreui.design.GlassBadge
import com.lambda.coreui.design.GlassButton
import com.lambda.coreui.design.GlassCard
import com.lambda.coreui.design.GlassHeader
import com.lambda.coreui.design.GlassPanel
import com.lambda.coreui.design.GlassPill
import com.lambda.coreui.design.GlassTextField
import com.lambda.coreui.design.SectionLabel
import com.lambda.coreui.theme.LambdaPalette
import com.lambda.databehavior.entity.LeituraNd
import com.lambda.databehavior.entity.LeituraNt
import com.lambda.databehavior.entity.Ponte
import com.lambda.databehavior.entity.Situacao

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BehaviorScreen(vm: BehaviorViewModel, modifier: Modifier = Modifier, onSpeak: ((String) -> Unit)? = null) {
    val s by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        GlassHeader(
            title = "Tradutor social",
            subtitle = "Duas leituras do mesmo fato. Nenhuma é a “certa” — são traduções.",
        )

        s.dbCount?.let { count ->
            val ok = count > 0
            GlassBadge(
                text = if (ok) "$count respostas locais disponíveis" else "Base local vazia — rode o seed local",
                accent = ok,
            )
        }

        GlassPanel(contentPadding = PaddingValues(18.dp)) {
            SectionLabel("A situação")
            Spacer(Modifier.height(8.dp))
            GlassTextField(
                value = s.query,
                onValueChange = vm::onQueryChange,
                label = "O que aconteceu, quem estava, o que foi dito",
                minLines = 3,
            )
            Spacer(Modifier.height(16.dp))
            SectionLabel("Tipo de relação")
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                s.relacoes.forEach { r ->
                    GlassPill(
                        selected = s.relacao == r,
                        onClick = { vm.onRelacaoChange(r) },
                        label = r.replaceFirstChar { it.uppercase() },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            SectionLabel("Modo de resposta")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AiEngine.Modo.entries.forEach { m ->
                    GlassPill(
                        selected = s.modo == m,
                        onClick = { vm.onModoChange(m) },
                        label = when (m) {
                            AiEngine.Modo.CURTO -> "Curto"
                            AiEngine.Modo.PADRAO -> "Padrão"
                            AiEngine.Modo.DETALHADO -> "Detalhado"
                        },
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            GlassButton(
                onClick = { vm.ask(both = true) },
                enabled = !s.loading && s.query.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (s.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Traduzindo…")
                } else {
                    Text("Ver leitura ND e NT", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        s.error?.let {
            GlassBadge(text = it, accent = false)
        }

        s.resultNd?.let {
            ReadingCard(
                title = "Leitura ND",
                subtitle = "Como pode ser percebido e sentido",
                result = it,
                accent = LambdaPalette.Nd,
                onSpeak = onSpeak,
            )
        }
        s.resultNt?.let {
            ReadingCard(
                title = "Leitura NT",
                subtitle = "Como pode ser interpretado",
                result = it,
                accent = LambdaPalette.Nt,
                onSpeak = onSpeak,
            )
        }

        if (s.resultNd != null || s.resultNt != null) {
            Text(
                "A ponte entre as duas leituras é o diálogo, não a correção. Source e confiança indicam de onde veio a resposta.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        if (s.situacoes.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            SectionLabel("Situações da realidade humana")
            Text(
                "Catálogo curado manualmente. Cada situação traz leitura ND, leitura NT e uma ponte prática.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            val aberta = s.situacaoAberta
            if (aberta != null) {
                SituacaoDetalhe(
                    aberta = aberta,
                    onClose = vm::fecharSituacao,
                    onSpeak = onSpeak,
                )
            } else {
                s.situacoes.forEach { sit ->
                    SituacaoCard(sit = sit, onClick = { vm.abrirSituacao(sit.id) })
                }
            }
        }
    }
}

@Composable
private fun SituacaoCard(sit: Situacao, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                sit.titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            GlassBadge(text = sit.categoria, accent = true)
        }
        Spacer(Modifier.height(6.dp))
        Text(sit.descricao, style = MaterialTheme.typography.bodySmall)
        if (sit.contextoAmbiente.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                sit.contextoAmbiente,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Toque para ver leitura ND, NT e ponte prática.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun SituacaoDetalhe(
    aberta: SituacaoAberta,
    onClose: () -> Unit,
    onSpeak: ((String) -> Unit)?,
) {
    val sit = aberta.situacao
    val fala = buildString {
        append(sit.titulo).append(". ").append(sit.descricao).append(' ')
        aberta.nd?.let {
            append("Leitura ND. Percepção sensorial: ").append(it.percepcaoSensorial).append(' ')
            append("Interpretação literal: ").append(it.interpretacaoLiteral).append(' ')
            append("Emoção provável: ").append(it.emocaoProvavel).append(' ')
            append("Carga cognitiva: ").append(it.cargaCognitiva).append(' ')
            append("Sinal de sobrecarga: ").append(it.sinalDeSobrecarga).append(' ')
            append("Estratégia de regulação: ").append(it.estrategiaDeRegulacao).append(' ')
        }
        aberta.nt?.let {
            append("Leitura NT. Intenção provável: ").append(it.intencaoProvavel).append(' ')
            append("Expectativa social implícita: ").append(it.expectativaSocialImplicita).append(' ')
            append("Como interpreta o comportamento ND: ").append(it.comoInterpretaOComportamentoNd).append(' ')
            append("Mal-entendido comum: ").append(it.malEntendidoComum).append(' ')
        }
        aberta.ponte?.let {
            append("Ponte. O que dizer: ").append(it.oQueDizerScripts).append(' ')
            append("O que fazer: ").append(it.oQueFazer).append(' ')
            append("O que evitar: ").append(it.oQueEvitar).append(' ')
            append("Como explicar meu funcionamento: ").append(it.comoExplicarMeuFuncionamento)
        }
    }

    GlassPanel(contentPadding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                sit.titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            GlassBadge(text = sit.categoria, accent = true)
            Spacer(Modifier.width(8.dp))
            GlassButton(onClick = onClose, accent = false) {
                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Fechar")
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(sit.descricao, style = MaterialTheme.typography.bodyMedium)
        if (sit.contextoAmbiente.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                sit.contextoAmbiente,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            )
        }

        aberta.nd?.let { nd -> NdBlock(nd) }
        aberta.nt?.let { nt -> NtBlock(nt) }
        aberta.ponte?.let { p -> PonteBlock(p) }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassBadge(text = sit.fonte)
            GlassBadge(text = "confiança ${"%.0f".format(sit.nivelConfianca * 100)}%", accent = true)
            if (onSpeak != null) {
                GlassButton(onClick = { onSpeak(fala) }, accent = false) {
                    Icon(Icons.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ouvir tudo")
                }
            }
        }
    }
}

@Composable
private fun NdBlock(nd: LeituraNd) {
    Spacer(Modifier.height(14.dp))
    Text("Leitura ND", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = LambdaPalette.Nd)
    Field("Percepção sensorial", nd.percepcaoSensorial)
    Field("Interpretação literal", nd.interpretacaoLiteral)
    Field("Emoção provável", nd.emocaoProvavel)
    Field("Carga cognitiva", nd.cargaCognitiva)
    Field("Sinal de sobrecarga", nd.sinalDeSobrecarga)
    Field("Estratégia de regulação", nd.estrategiaDeRegulacao)
}

@Composable
private fun NtBlock(nt: LeituraNt) {
    Spacer(Modifier.height(14.dp))
    Text("Leitura NT", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = LambdaPalette.Nt)
    Field("Intenção provável", nt.intencaoProvavel)
    Field("Expectativa social implícita", nt.expectativaSocialImplicita)
    Field("Como interpreta o comportamento ND", nt.comoInterpretaOComportamentoNd)
    Field("Mal-entendido comum", nt.malEntendidoComum)
}

@Composable
private fun PonteBlock(p: Ponte) {
    Spacer(Modifier.height(14.dp))
    Text("Ponte prática", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = LambdaPalette.Turquoise)
    Field("O que dizer (scripts)", p.oQueDizerScripts)
    Field("O que fazer", p.oQueFazer)
    Field("O que evitar", p.oQueEvitar)
    Field("Como explicar meu funcionamento", p.comoExplicarMeuFuncionamento)
}

@Composable
private fun Field(label: String, value: String) {
    if (value.isBlank()) return
    Spacer(Modifier.height(6.dp))
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
    )
    Text(value, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun ReadingCard(
    title: String,
    subtitle: String,
    result: AiResult,
    accent: Color,
    onSpeak: ((String) -> Unit)?,
) {
    GlassPanel(contentPadding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(result.texto, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassBadge(text = result.fonte)
            GlassBadge(text = "confiança ${"%.0f".format(result.nivelConfianca * 100)}%", accent = true)
            if (result.meta.isNotBlank()) {
                GlassBadge(text = result.meta)
            }
        }
        if (onSpeak != null) {
            Spacer(Modifier.height(10.dp))
            GlassButton(onClick = { onSpeak(result.texto) }, accent = false) {
                Icon(Icons.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ouvir")
            }
        }
    }
}
