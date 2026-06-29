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

@Composable
fun EditarAlunoScreen(
    alunoId: Long,
    onMenuClick: () -> Unit,
    onPagamentos: (Long) -> Unit,
    onFrequencia: (String) -> Unit,
    onDeleted: () -> Unit,
    onVoltar: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.get(ctx) }
    val scope = rememberCoroutineScope()

    var aluno by remember { mutableStateOf<Aluno?>(null) }
    var graduacoes by remember { mutableStateOf<List<Graduacao>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var nome by remember { mutableStateOf("") }
    var sexo by remember { mutableStateOf("M") }
    // Data nascimento em 3 partes
    var dnDia by remember { mutableStateOf("01") }
    var dnMes by remember { mutableStateOf("01") }
    var dnAno by remember { mutableStateOf("2000") }
    var faixa by remember { mutableStateOf("Branca") }
    var tipo by remember { mutableStateOf("Aluno") }
    var valor by remember { mutableStateOf("0") }
    var diaVencimento by remember { mutableStateOf("5") }
    var telefone by remember { mutableStateOf("") }
    var responsavel by remember { mutableStateOf("") }
    // Data matrícula em 3 partes
    var matDia by remember { mutableStateOf("01") }
    var matMes by remember { mutableStateOf("01") }
    var matAno by remember { mutableStateOf("2024") }
    var turma by remember { mutableStateOf("Adulto") }
    var horario by remember { mutableStateOf(AlunoOpcoes.horarios[2]) }
    var modalidade by remember { mutableStateOf("Jiu-Jitsu") }
    var observacoes by remember { mutableStateOf("") }
    var ativo by remember { mutableStateOf(true) }

    var gradFaixa by remember { mutableStateOf("Branca") }
    var gradDia by remember { mutableStateOf("%02d".format(LocalDate.now().dayOfMonth)) }
    var gradMes by remember { mutableStateOf("%02d".format(LocalDate.now().monthValue)) }
    var gradAno by remember { mutableStateOf(LocalDate.now().year.toString()) }
    var gradObs by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("") }
    var numFotos by remember { mutableStateOf(0) }

    fun parseData(data: String): Triple<String, String, String> {
        return if (data.length >= 10 && data.contains("-")) {
            val parts = data.split("-")
            Triple(parts.getOrElse(2) { "01" }, parts.getOrElse(1) { "01" }, parts.getOrElse(0) { "2000" })
        } else {
            Triple("01", "01", "2000")
        }
    }

    fun carregar() {
        scope.launch {
            val a = db.alunoDao().getById(alunoId) ?: return@launch
            aluno = a
            nome = a.nome; sexo = a.sexo.ifBlank { "M" }
            val (dd, dm, dy) = parseData(a.dataNascimento)
            dnDia = dd; dnMes = dm; dnAno = dy
            faixa = a.faixaAtual.ifBlank { "Branca" }
            tipo = a.tipo.ifBlank { "Aluno" }
            valor = a.valorMensalidade.toString()
            diaVencimento = a.diaVencimento.toString()
            telefone = a.telefone; responsavel = a.responsavel
            val (md, mm, my) = parseData(a.dataMatricula)
            matDia = md; matMes = mm; matAno = my
            turma = a.turma.ifBlank { "Adulto" }
            horario = a.horario.ifBlank { AlunoOpcoes.horarios[2] }
            if (horario !in AlunoOpcoes.horarios) horario = AlunoOpcoes.horarios[2]
            modalidade = a.modalidade.ifBlank { "Jiu-Jitsu" }
            if (modalidade !in AlunoOpcoes.modalidades) modalidade = "Jiu-Jitsu"
            observacoes = a.observacoes; ativo = a.ativo == 1
            graduacoes = db.graduacaoDao().getByAluno(alunoId)
            numFotos = db.faceEmbeddingDao().countByAluno(alunoId)

            val hoje = LocalDate.now()
            val comp = "${hoje.year}-${"%02d".format(hoje.monthValue)}"
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

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("⚠️ Confirmar Exclusão") },
            text = { Text("Excluir $nome e todos os dados?", color = NukWhite) },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        db.faceEmbeddingDao().deleteByAluno(alunoId)
                        db.pagamentoDao().deleteByAluno(alunoId)
                        db.presencaDao().deleteByAluno(nome)
                        db.graduacaoDao().deleteByAluno(alunoId)
                        db.alunoDao().delete(alunoId)
                        showDeleteDialog = false; onDeleted()
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = NukPurple)) { Text("SIM, EXCLUIR") }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NukMedGray)) { Text("CANCELAR") }
            },
            containerColor = NukDarkGray
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NukTopBar("✏️ Editar Aluno", onMenuClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Botão voltar
            Button(onClick = { onVoltar() },
                colors = ButtonDefaults.buttonColors(containerColor = NukMedGray),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) { Text("← Voltar") }

            // Header
            Text(nome, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NukPurple)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Status: $statusText", fontSize = 14.sp, color = NukWhite)
                Text("📸 $numFotos foto(s)", fontSize = 14.sp,
                    color = if (numFotos > 0) NukGreen else NukYellow)
            }

            // ─── Campos ───
            FormField("Nome", nome) { nome = it }
            DropdownField("Sexo", sexo, AlunoOpcoes.sexos) { sexo = it }

            SectionTitle("DATA DE NASCIMENTO", "📅")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.weight(1f)) { DropdownField("Dia", dnDia, AlunoOpcoes.diasDoMes()) { dnDia = it } }
                Box(Modifier.weight(1.5f)) { DropdownField("Mês", dnMes, AlunoOpcoes.meses.map { it.first }) { dnMes = it } }
                Box(Modifier.weight(1.2f)) { DropdownField("Ano", dnAno, AlunoOpcoes.anosDisponiveis()) { dnAno = it } }
            }

            DropdownField("Faixa Atual", faixa, AlunoOpcoes.faixas) { faixa = it }
            DropdownField("Tipo", tipo, AlunoOpcoes.tipos) { tipo = it }
            FormField("Valor Mensalidade", valor) { valor = it }
            DropdownField("Dia Vencimento", diaVencimento, AlunoOpcoes.diasVencimento) { diaVencimento = it }
            FormField("Telefone", telefone) { telefone = it }
            FormField("Responsável", responsavel) { responsavel = it }

            SectionTitle("DATA DE MATRÍCULA", "📅")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.weight(1f)) { DropdownField("Dia", matDia, AlunoOpcoes.diasDoMes()) { matDia = it } }
                Box(Modifier.weight(1.5f)) { DropdownField("Mês", matMes, AlunoOpcoes.meses.map { it.first }) { matMes = it } }
                Box(Modifier.weight(1.2f)) { DropdownField("Ano", matAno, AlunoOpcoes.anosDisponiveis().take(10)) { matAno = it } }
            }

            DropdownField("Turma", turma, AlunoOpcoes.turmas) { turma = it }
            DropdownField("Horário", horario, AlunoOpcoes.horarios) { horario = it }
            DropdownField("Modalidade", modalidade, AlunoOpcoes.modalidades) { modalidade = it }
            FormField("Observações", observacoes) { observacoes = it }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ativo:", color = NukWhite, fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Switch(checked = ativo, onCheckedChange = { ativo = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = NukPurple))
            }

            // ─── Botões ───
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            db.alunoDao().update(Aluno(
                                id = alunoId, nome = nome, sexo = sexo,
                                dataNascimento = "$dnAno-$dnMes-$dnDia",
                                faixaAtual = faixa, tipo = tipo,
                                valorMensalidade = valor.toDoubleOrNull() ?: 0.0,
                                diaVencimento = diaVencimento.toIntOrNull() ?: 5,
                                telefone = telefone, responsavel = responsavel,
                                dataMatricula = "$matAno-$matMes-$matDia",
                                turma = turma, horario = horario, modalidade = modalidade,
                                observacoes = observacoes, ativo = if (ativo) 1 else 0,
                                fotoPath = aluno?.fotoPath ?: ""
                            ))
                            Toast.makeText(ctx, "✅ Salvo!", Toast.LENGTH_SHORT).show()
                            carregar()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NukGreen),
                    modifier = Modifier.weight(1f)
                ) { Text("💾 SALVAR") }
                Button(onClick = { onPagamentos(alunoId) },
                    colors = ButtonDefaults.buttonColors(containerColor = NukBlue),
                    modifier = Modifier.weight(1f)
                ) { Text("💳 PAGAM.") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onFrequencia(nome) },
                    colors = ButtonDefaults.buttonColors(containerColor = NukMedGray),
                    modifier = Modifier.weight(1f)
                ) { Text("📅 FREQ.") }
                Button(onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NukRedDark),
                    modifier = Modifier.weight(1f)
                ) { Text("🗑 EXCLUIR") }
            }

            // ─── Graduações ───
            Spacer(Modifier.height(8.dp))
            SectionTitle("GRADUAÇÕES", "🥋")

            NukCard {
                Text("Nova Graduação", fontWeight = FontWeight.Bold, color = NukWhite, fontSize = 14.sp)
                
                Text("Data da Graduação", fontSize = 12.sp, color = NukLightGray)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.weight(1f)) { DropdownField("Dia", gradDia, AlunoOpcoes.diasDoMes()) { gradDia = it } }
                    Box(Modifier.weight(1.5f)) { DropdownField("Mês", gradMes, AlunoOpcoes.meses.map { it.first }) { gradMes = it } }
                    Box(Modifier.weight(1.2f)) { DropdownField("Ano", gradAno, AlunoOpcoes.anosDisponiveis().take(10)) { gradAno = it } }
                }
                
                DropdownField("Faixa", gradFaixa, AlunoOpcoes.faixas) { gradFaixa = it }
                FormField("Observação", gradObs) { gradObs = it }
                Button(
                    onClick = {
                        if (gradFaixa.isBlank()) return@Button
                        scope.launch {
                            db.graduacaoDao().insert(Graduacao(
                                alunoId = alunoId,
                                dataGraduacao = "$gradAno-$gradMes-$gradDia",
                                faixa = gradFaixa, observacao = gradObs))
                            db.alunoDao().updateFaixa(alunoId, gradFaixa)
                            gradObs = ""; carregar()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NukPurple),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("➕ Registrar") }
            }

            graduacoes.forEach { g ->
                NukCard {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🥋 ${g.faixa} — ${g.dataGraduacao}", fontSize = 13.sp, color = NukWhite)
                            if (g.observacao.isNotBlank()) Text(g.observacao, fontSize = 11.sp, color = NukLightGray)
                        }
                        Button(onClick = { scope.launch { db.graduacaoDao().delete(g.id); carregar() } },
                            colors = ButtonDefaults.buttonColors(containerColor = NukRedDark),
                            contentPadding = PaddingValues(8.dp)) { Text("🗑") }
                    }
                }
            }
            
            Spacer(Modifier.height(80.dp))
        }
    }
}
