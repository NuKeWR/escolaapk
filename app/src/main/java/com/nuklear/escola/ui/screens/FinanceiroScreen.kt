package com.nuklear.escola.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuklear.escola.data.db.AppDatabase
import com.nuklear.escola.data.model.Pagamento
import com.nuklear.escola.ui.components.*
import com.nuklear.escola.ui.theme.*
import java.time.YearMonth

private val MESES = listOf(
    "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
)

/** Aba Financeiro: recebimentos do mês, quem está em dia ou pendente e as últimas movimentações. */
@Composable
fun FinanceiroScreen(
    onMenuClick: () -> Unit,
    onNovoPagamento: () -> Unit = {},
    onPendencias: () -> Unit = {},
    onHistorico: () -> Unit = {},
    onRecibo: (Long) -> Unit = {}
) {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.get(ctx) }

    var mes by remember { mutableStateOf(YearMonth.now()) }
    var pagamentos by remember { mutableStateOf<List<Pagamento>>(emptyList()) }
    var nomes by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var fotos by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var recebido by remember { mutableStateOf(0.0) }
    var previsto by remember { mutableStateOf(0.0) }
    var emDia by remember { mutableStateOf(0) }
    var pendentes by remember { mutableStateOf(0) }

    LaunchedEffect(mes) {
        val comp = mes.toString() // "2026-11"
        val lista = db.pagamentoDao().getByCompetencia(comp)
        val todos = db.alunoDao().getAll()
        nomes = todos.associate { it.id to it.nome }
        fotos = todos.associate { it.id to it.fotoPath }
        pagamentos = lista
        recebido = lista.sumOf { it.valorPago }

        val ativos = todos.filter { it.ativo == 1 }
        previsto = ativos.sumOf { it.valorMensalidade }
        var d = 0
        var p = 0
        for (a in ativos) {
            if (a.tipo.lowercase() in listOf("bolsista", "professor")) continue
            val ult = db.pagamentoDao().ultimaCompetencia(a.id)
            if (ult != null && ult >= comp) d++ else p++
        }
        emDia = d
        pendentes = p
    }

    val fracao = if (previsto > 0) (recebido / previsto).coerceIn(0.0, 1.0).toFloat() else 0f

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            NukTopBar("Financeiro", onMenuClick)

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Seletor de mês
                item {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(NukDarkGray),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { mes = mes.minusMonths(1) }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Mês anterior", tint = NukPurpleLight)
                        }
                        Text(
                            "${MESES[mes.monthValue - 1]} ${mes.year}",
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NukWhite
                        )
                        IconButton(onClick = { mes = mes.plusMonths(1) }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Próximo mês", tint = NukPurpleLight)
                        }
                    }
                }

                // Recebido no mês
                item {
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF3A2A7A), Color(0xFF1A1432))))
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Recebido no mês", fontSize = 13.sp, color = NukPurpleLight, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.BarChart, contentDescription = null, tint = NukPurpleLight)
                            }
                            Text(moeda(recebido), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = NukWhite)
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NukGreen, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (pagamentos.size == 1) "1 pagamento confirmado"
                                    else "${pagamentos.size} pagamentos confirmados",
                                    fontSize = 13.sp, color = NukGreen
                                )
                            }
                            if (previsto > 0) {
                                Spacer(Modifier.height(14.dp))
                                Box(
                                    Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                        .background(Color.White.copy(alpha = 0.12f))
                                ) {
                                    Box(
                                        Modifier.fillMaxWidth(fracao).fillMaxHeight()
                                            .clip(RoundedCornerShape(3.dp)).background(NukPurple)
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text("de ${moeda(previsto)} previstos", fontSize = 12.sp, color = NukLightGray)
                            }
                        }
                    }
                }

                // Em dia x Pendente
                item {
                    Row(
                        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SituacaoCard(
                            icone = Icons.Default.CheckCircle, cor = NukGreen,
                            rotulo = "Em dia", valor = emDia,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        SituacaoCard(
                            icone = Icons.Default.Schedule, cor = NukOrange,
                            rotulo = "Pendente", valor = pendentes,
                            modifier = Modifier.weight(1f).fillMaxHeight().clickable { onPendencias() }
                        )
                    }
                }

                // Últimas movimentações
                item {
                    SectionHeader("Últimas movimentações", "Histórico") { onHistorico() }
                }

                if (pagamentos.isEmpty()) {
                    item {
                        NukCard {
                            Text(
                                "Nenhum pagamento em ${MESES[mes.monthValue - 1].lowercase()}.",
                                fontSize = 13.sp, color = NukLightGray
                            )
                        }
                    }
                }

                items(pagamentos, key = { it.id }) { p ->
                    val nome = nomes[p.alunoId] ?: "Aluno removido"
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onRecibo(p.id) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = NukDarkGray),
                        border = BorderStroke(1.dp, NukBorder)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Avatar(nome = nome, tamanho = 42.dp, foto = fotos[p.alunoId] ?: "")
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(nome, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NukWhite, maxLines = 1)
                                Text(
                                    dataBr(p.dataPagamento) + if (p.formaPagamento.isNotBlank()) " • ${p.formaPagamento}" else "",
                                    fontSize = 12.sp, color = NukLightGray
                                )
                            }
                            Text(moeda(p.valorPago), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NukGreen)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.CheckCircle, contentDescription = "Pago", tint = NukGreen, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // Ação principal: registrar um pagamento em poucos toques
        FloatingActionButton(
            onClick = onNovoPagamento,
            containerColor = NukPurple,
            contentColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Row(
                Modifier.padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Payments, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Registrar pagamento", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun SituacaoCard(
    icone: ImageVector,
    cor: Color,
    rotulo: String,
    valor: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NukDarkGray),
        border = BorderStroke(1.dp, NukBorder)
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(cor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(rotulo, fontSize = 13.sp, color = NukLightGray)
            }
            Spacer(Modifier.height(8.dp))
            Text("%02d".format(valor), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = NukWhite)
        }
    }
}

/** "2026-11-24" -> "24/11/2026" */
private fun dataBr(iso: String): String {
    val p = iso.split("-")
    return if (p.size == 3) "${p[2]}/${p[1]}/${p[0]}" else iso
}
