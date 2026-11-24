package com.nuklear.escola.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.nuklear.escola.data.model.Aluno
import com.nuklear.escola.data.model.Pagamento
import com.nuklear.escola.ui.components.*
import com.nuklear.escola.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagamentoRapidoScreen(onMenuClick: () -> Unit, onRecibo: (Long) -> Unit = {}) {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.get(ctx) }
    val scope = rememberCoroutineScope()

    var alunosList by remember { mutableStateOf<List<Aluno>>(emptyList()) }
    var busca by remember { mutableStateOf("") }
    var alunoSelecionado by remember { mutableStateOf<Aluno?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Campos pagamento
    val hoje = LocalDate.now()
    var pagDia by remember { mutableStateOf("%02d".format(hoje.dayOfMonth)) }
    var pagMes by remember { mutableStateOf("%02d".format(hoje.monthValue)) }
    var pagAno by remember { mutableStateOf(hoje.year.toString()) }
    var competencia by remember { mutableStateOf(AlunoOpcoes.competenciaAtual()) }
    var valor by remember { mutableStateOf("") }
    var forma by remember { mutableStateOf("Pix") }
    var obs by remember { mutableStateOf("") }
    var comprovanteUri by remember { mutableStateOf<Uri?>(null) }
    var statusText by remember { mutableStateOf("") }

    val comprovanteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()) { uri -> comprovanteUri = uri }

    LaunchedEffect(Unit) {
        alunosList = db.alunoDao().getAtivos()
    }

    // Dialog confirmar
    if (showConfirmDialog && alunoSelecionado != null) {
        val a = alunoSelecionado!!
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("✅ Confirmar Pagamento", color = NukPurple) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Aluno: ${a.nome}", color = NukWhite, fontWeight = FontWeight.Bold)
                    Text("Turma: ${a.turma} — ${a.horario}", color = NukLightGray, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Competência: $competencia", color = NukWhite)
                    Text("Data: $pagAno-$pagMes-$pagDia", color = NukWhite)
                    Text("Valor: R$ $valor", color = NukGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Forma: $forma", color = NukWhite)
                    if (obs.isNotBlank()) Text("Obs: $obs", color = NukLightGray)
                    if (comprovanteUri != null) Text("📎 Comprovante anexado", color = NukGreen)
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val novoId = db.pagamentoDao().insert(Pagamento(
                            alunoId = a.id,
                            dataPagamento = "$pagAno-$pagMes-$pagDia",
                            competencia = competencia,
                            valorPago = valor.toDoubleOrNull() ?: 0.0,
                            formaPagamento = forma,
                            observacao = if (comprovanteUri != null) "$obs [Comprovante: $comprovanteUri]" else obs
                        ))
                        showConfirmDialog = false
                        Toast.makeText(ctx, "✅ Pagamento de ${a.nome} registrado!", Toast.LENGTH_SHORT).show()
                        // Limpa o formulário (a tela fica pronta para o próximo pagamento)
                        alunoSelecionado = null; busca = ""; valor = ""; obs = ""; comprovanteUri = null
                        // Vai direto para o recibo; a seta "←" volta para cá
                        onRecibo(novoId)
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = NukGreen)) {
                    Text("💾 CONFIRMAR", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(onClick = { showConfirmDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NukMedGray)) { Text("Cancelar") }
            },
            containerColor = NukDarkGray
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NukTopBar("💳 Pagamento Rápido", onMenuClick)

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(14.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (statusText.isNotBlank()) {
                NukCard { Text(statusText, color = NukGreen, fontSize = 13.sp) }
            }

            // ─── PASSO 1: Buscar aluno ───
            NukCard {
                SectionTitle("1️⃣ BUSCAR ALUNO", "🔍")
                OutlinedTextField(
                    value = busca, onValueChange = { busca = it; alunoSelecionado = null },
                    placeholder = { Text("Digite o nome do aluno...", color = NukLightGray) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NukPurple, unfocusedBorderColor = NukMedGray,
                        focusedTextColor = NukWhite, unfocusedTextColor = NukWhite))

                if (busca.length >= 2 && alunoSelecionado == null) {
                    val filtrados = alunosList.filter { it.nome.contains(busca, ignoreCase = true) }
                    filtrados.take(5).forEach { aluno ->
                        Button(
                            onClick = {
                                alunoSelecionado = aluno
                                busca = aluno.nome
                                valor = aluno.valorMensalidade.toString()
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NukDarkGray)
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(aluno.nome, color = NukWhite, fontWeight = FontWeight.Bold)
                                Text("${aluno.turma} • ${aluno.horario} • R$ ${aluno.valorMensalidade}",
                                    color = NukLightGray, fontSize = 11.sp)
                            }
                        }
                    }
                    if (filtrados.isEmpty()) Text("Nenhum aluno encontrado", color = NukLightGray, fontSize = 12.sp)
                }

                if (alunoSelecionado != null) {
                    Spacer(Modifier.height(4.dp))
                    Text("✅ ${alunoSelecionado!!.nome}", color = NukGreen, fontWeight = FontWeight.Bold)
                    Text("${alunoSelecionado!!.turma} • ${alunoSelecionado!!.horario}",
                        color = NukLightGray, fontSize = 12.sp)
                }
            }

            // ─── PASSO 2: Dados do pagamento ───
            if (alunoSelecionado != null) {
                NukCard {
                    SectionTitle("2️⃣ DADOS DO PAGAMENTO", "💰")

                    DropdownField("Competência", competencia, AlunoOpcoes.competencias()) { competencia = it }

                    Spacer(Modifier.height(6.dp))
                    Text("Data do Pagamento", fontSize = 12.sp, color = NukLightGray)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.weight(1f)) { DropdownField("Dia", pagDia, AlunoOpcoes.diasDoMes()) { pagDia = it } }
                        Box(Modifier.weight(1.5f)) { DropdownField("Mês", pagMes, AlunoOpcoes.meses.map { it.first }) { pagMes = it } }
                        Box(Modifier.weight(1.2f)) { DropdownField("Ano", pagAno, listOf(hoje.year.toString(), (hoje.year - 1).toString())) { pagAno = it } }
                    }

                    FormField("Valor (R$)", valor) { valor = it }
                    DropdownField("Forma", forma, AlunoOpcoes.formasPagamento) { forma = it }
                    FormField("Observação", obs) { obs = it }
                }

                // ─── PASSO 3: Comprovante ───
                NukCard {
                    SectionTitle("3️⃣ COMPROVANTE (opcional)", "📎")
                    Button(
                        onClick = { comprovanteLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NukPurple)
                    ) { Text(if (comprovanteUri != null) "✅ Comprovante anexado — trocar" else "📎 Anexar comprovante") }
                }

                // ─── BOTÃO REGISTRAR ───
                Button(
                    onClick = {
                        if (valor.isBlank() || valor.toDoubleOrNull() == null) {
                            Toast.makeText(ctx, "Informe o valor!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        showConfirmDialog = true
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NukGreen),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) { Text("💳 REGISTRAR PAGAMENTO", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
