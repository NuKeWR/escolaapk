package com.nuklear.escola.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuklear.escola.data.db.AppDatabase
import com.nuklear.escola.data.model.Aluno
import com.nuklear.escola.ui.components.*
import com.nuklear.escola.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AniversariantesScreen(
    onMenuClick: () -> Unit,
    onAlunoClick: (Long) -> Unit
) {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.get(ctx) }
    val scope = rememberCoroutineScope()

    val hoje = LocalDate.now()
    var mesSelecionado by remember { mutableStateOf(hoje.monthValue) }
    var aniversariantes by remember { mutableStateOf<List<Aluno>>(emptyList()) }

    val meses = (1..12).map { m ->
        val nome = java.time.Month.of(m).getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
            .replaceFirstChar { it.uppercase() }
        m to nome
    }

    fun carregar() {
        scope.launch {
            val mesStr = "%02d".format(mesSelecionado)
            aniversariantes = db.alunoDao().aniversariantes(mesStr)
        }
    }

    LaunchedEffect(mesSelecionado) { carregar() }

    Column(modifier = Modifier.fillMaxSize()) {
        NukTopBar("🎂 Aniversariantes", onMenuClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Seletor de mês
            NukCard {
                SectionTitle("SELECIONE O MÊS", "📅")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Primeira fileira: Jan-Jun
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        meses.take(6).forEach { (num, nome) ->
                            val isAtual = num == mesSelecionado
                            val isHoje = num == hoje.monthValue
                            Button(
                                onClick = { mesSelecionado = num },
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        isAtual -> NukRed
                                        isHoje -> NukMedGray
                                        else -> NukDarkGray
                                    }
                                ),
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                Text(nome.take(3), fontSize = 12.sp,
                                    fontWeight = if (isAtual) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                    // Segunda fileira: Jul-Dez
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        meses.drop(6).forEach { (num, nome) ->
                            val isAtual = num == mesSelecionado
                            val isHoje = num == hoje.monthValue
                            Button(
                                onClick = { mesSelecionado = num },
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        isAtual -> NukRed
                                        isHoje -> NukMedGray
                                        else -> NukDarkGray
                                    }
                                ),
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                Text(nome.take(3), fontSize = 12.sp,
                                    fontWeight = if (isAtual) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }

            // Título
            val mesNome = meses.find { it.first == mesSelecionado }?.second ?: ""
            Text(
                "🎂 ${aniversariantes.size} aniversariante(s) em $mesNome",
                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NukRed
            )

            if (aniversariantes.isEmpty()) {
                NukCard {
                    Text("Nenhum aniversariante neste mês", fontSize = 14.sp, color = NukLightGray)
                }
            }

            // Lista
            aniversariantes.forEach { aluno ->
                val dia = if (aluno.dataNascimento.length >= 10)
                    aluno.dataNascimento.substring(8, 10) else "?"

                val ehHoje = aluno.dataNascimento.length >= 10 &&
                        aluno.dataNascimento.substring(5) == "${"%02d".format(hoje.monthValue)}-${"%02d".format(hoje.dayOfMonth)}"

                NukCard(
                    modifier = if (ehHoje)
                        Modifier.border(2.dp, Color(0xFF9C27B0), RoundedCornerShape(12.dp))
                    else Modifier
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row {
                                Text(if (ehHoje) "🎂🎉 " else "🎂 ", fontSize = 16.sp)
                                Text(aluno.nome, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                    color = NukWhite)
                            }
                            Text("Dia $dia • ${aluno.turma} • ${aluno.horario}",
                                fontSize = 12.sp, color = NukLightGray)
                            Text(aluno.dataNascimento, fontSize = 11.sp, color = NukLightGray)
                            if (ehHoje) {
                                Text("🎉 FAZ ANIVERSÁRIO HOJE!", fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF9C27B0))
                            }
                        }
                        Button(
                            onClick = { onAlunoClick(aluno.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = NukPurple),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) { Text("👤") }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}
