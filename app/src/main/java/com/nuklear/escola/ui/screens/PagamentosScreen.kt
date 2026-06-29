package com.nuklear.escola.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuklear.escola.data.db.AppDatabase
import com.nuklear.escola.data.model.*
import com.nuklear.escola.ui.components.*
import com.nuklear.escola.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagamentosScreen(
    alunoId: Long,
    onMenuClick: () -> Unit,
    onRecibo: (Long) -> Unit = {}
) {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.get(ctx) }
    val scope = rememberCoroutineScope()

    var aluno by remember { mutableStateOf<Aluno?>(null) }
    var historico by remember { mutableStateOf<List<Pagamento>>(emptyList()) }
    var statusText by remember { mutableStateOf("") }

    val hoje = LocalDate.now()
    var pagDia by remember { mutableStateOf("%02d".format(hoje.dayOfMonth)) }
    var pagMes by remember { mutableStateOf("%02d".format(hoje.monthValue)) }
    var pagAno by remember { mutableStateOf(hoje.year.toString()) }
    var pagComp by remember { mutableStateOf(AlunoOpcoes.competenciaAtual()) }
    var pagValor by remember { mutableStateOf("") }
    var pagForma by remember { mutableStateOf("Pix") }
    var pagObs by remember { mutableStateOf("") }

    var editando by remember { mutableStateOf<Pagamento?>(null) }

    fun carregar() {
        scope.launch {
            aluno = db.alunoDao().getById(alunoId)
            historico = db.pagamentoDao().getByAluno(alunoId)
            pagValor = aluno?.valorMensalidade?.toString() ?: "0"

            val comp = "${hoje.year}-${"%02d".format(hoje.monthValue)}"
            val a = aluno ?: return@launch
            statusText = when {
                a.tipo.lowercase() == "bolsista" -> "🟡 Bolsista"
                a.tipo.lowercase() == "professor" -> "🔵 Isento"
                else -> {
                    val ult = db.pagamentoDao().ultimaCompetencia(a.id)
                    if (ult != null && ult >= comp) "🟢 Em dia" else "🔴 Atrasado"
                }
            }
        }
    }

    LaunchedEffect(alunoId) { carregar() }

    // Dialog editar
    if (editando != null) {
        val p = editando!!
        var eData by remember(p.id) { mutableStateOf(p.dataPagamento) }
        var eComp by remember(p.id) { mutableStateOf(p.competencia) }
        var eValor by remember(p.id) { mutableStateOf(p.valorPago.toString()) }
        var eForma by remember(p.id) { mutableStateOf(p.formaPagamento) }
        var eObs by remember(p.id) { mutableStateOf(p.observacao) }

        AlertDialog(
            onDismissRequest = { editando = null },
            title = { Text("✏️ Editar Pagamento", color = NukPurple) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormField("Data", eData) { eData = it }
                    FormField("Competência", eComp) { eComp = it }
                    FormField("Valor", eValor) { eValor = it }
                    FormField("Forma", eForma) { eForma = it }
                    FormField("Observação", eObs) { eObs = it }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        db.pagamentoDao().update(
                            p.copy(dataPagamento = eData, competencia = eComp,
                                valorPago = eValor.toDoubleOrNull() ?: 0.0,
                                formaPagamento = eForma, observacao = eObs)
                        )
                        editando = null; carregar()
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = NukGreen)) {
                    Text("💾 SALVAR")
                }
            },
            dismissButton = {
                Button(onClick = { editando = null },
                    colors = ButtonDefaults.buttonColors(containerColor = NukMedGray)) {
                    Text("CANCELAR")
                }
            },
            containerColor = NukDarkGray
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NukTopBar("💳 Pagamentos", onMenuClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("💳 ${aluno?.nome ?: ""}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NukPurple)
            Text("$statusText | Mensalidade: R$ ${"%.2f".format(aluno?.valorMensalidade ?: 0.0)}",
                fontSize = 13.sp, color = NukWhite)

            // Form
            NukCard {
                SectionTitle("NOVO PAGAMENTO", "➕")
                
                DropdownField("Competência", pagComp, AlunoOpcoes.competencias()) { pagComp = it }
                
                Spacer(Modifier.height(4.dp))
                Text("Data do Pagamento", fontSize = 12.sp, color = NukLightGray)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.weight(1f)) { DropdownField("Dia", pagDia, AlunoOpcoes.diasDoMes()) { pagDia = it } }
                    Box(Modifier.weight(1.5f)) { DropdownField("Mês", pagMes, AlunoOpcoes.meses.map { it.first }) { pagMes = it } }
                    Box(Modifier.weight(1.2f)) { DropdownField("Ano", pagAno, listOf(hoje.year.toString(), (hoje.year - 1).toString())) { pagAno = it } }
                }
                
                FormField("Valor", pagValor) { pagValor = it }
                DropdownField("Forma", pagForma, AlunoOpcoes.formasPagamento) { pagForma = it }
                FormField("Observação", pagObs) { pagObs = it }

                Button(
                    onClick = {
                        scope.launch {
                            db.pagamentoDao().insert(Pagamento(
                                alunoId = alunoId, dataPagamento = "$pagAno-$pagMes-$pagDia",
                                competencia = pagComp,
                                valorPago = pagValor.toDoubleOrNull() ?: 0.0,
                                formaPagamento = pagForma, observacao = pagObs))
                            pagObs = ""
                            Toast.makeText(ctx, "✅ Pagamento registrado!", Toast.LENGTH_SHORT).show()
                            carregar()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NukGreen)
                ) { Text("💾 REGISTRAR", fontWeight = FontWeight.Bold) }
            }

            // Histórico
            SectionTitle("HISTÓRICO", "📋")
            historico.forEach { h ->
                NukCard {
                    Text("📅 ${h.competencia} — R$ ${"%.2f".format(h.valorPago)}",
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NukWhite)
                    Text("Pago em: ${h.dataPagamento} | ${h.formaPagamento}",
                        fontSize = 11.sp, color = NukLightGray)
                    if (h.observacao.isNotBlank()) {
                        Text("Obs: ${h.observacao}", fontSize = 11.sp, color = NukLightGray)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)) {
                        Button(onClick = { editando = h },
                            colors = ButtonDefaults.buttonColors(containerColor = NukBlue),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) { Text("✏️ Editar", fontSize = 11.sp) }
                        Button(onClick = { onRecibo(h.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = NukGreen),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) { Text("🧾 Recibo", fontSize = 11.sp) }
                        Button(onClick = {
                            scope.launch { db.pagamentoDao().delete(h.id); carregar() }
                        },
                            colors = ButtonDefaults.buttonColors(containerColor = NukRedDark),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) { Text("🗑", fontSize = 11.sp) }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}
