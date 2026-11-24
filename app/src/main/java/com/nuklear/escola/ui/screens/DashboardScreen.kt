package com.nuklear.escola.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuklear.escola.R
import com.nuklear.escola.data.db.AppDatabase
import com.nuklear.escola.data.db.HorarioCount
import com.nuklear.escola.data.model.Aluno
import com.nuklear.escola.ui.components.*
import com.nuklear.escola.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(onMenuClick: () -> Unit, onNavigate: (String) -> Unit = {}) {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.get(ctx) }
    val scope = rememberCoroutineScope()

    var totalAlunos by remember { mutableStateOf(0) }
    var totalKids by remember { mutableStateOf(0) }
    var totalAdultos by remember { mutableStateOf(0) }
    var receitaPrevista by remember { mutableStateOf(0.0) }
    var recebidoMes by remember { mutableStateOf(0.0) }
    var presentesHoje by remember { mutableStateOf(0) }
    var emDia by remember { mutableStateOf(0) }
    var atrasados by remember { mutableStateOf(0) }
    var bolsistas by remember { mutableStateOf(0) }
    var isentos by remember { mutableStateOf(0) }
    var porHorario by remember { mutableStateOf<List<HorarioCount>>(emptyList()) }
    var aniversariantes by remember { mutableStateOf<List<Aluno>>(emptyList()) }

    val hoje = LocalDate.now()
    val comp = "${hoje.year}-${"%02d".format(hoje.monthValue)}"

    LaunchedEffect(Unit) {
        scope.launch {
            totalAlunos = db.alunoDao().countAtivos()
            totalKids = db.alunoDao().countByTurma("Kids")
            totalAdultos = db.alunoDao().countByTurma("Adulto")
            receitaPrevista = db.alunoDao().receitaPrevista()
            recebidoMes = db.pagamentoDao().recebidoMes(comp)
            presentesHoje = db.presencaDao().countPresentes(hoje.toString())
            porHorario = db.alunoDao().countByHorario()
            aniversariantes = db.alunoDao().aniversariantes("%02d".format(hoje.monthValue))

            // Calcular status
            val ativos = db.alunoDao().getAtivos()
            var d = 0; var a = 0; var b = 0; var i = 0
            for (aluno in ativos) {
                when {
                    aluno.tipo.lowercase() == "bolsista" -> b++
                    aluno.tipo.lowercase() == "professor" -> i++
                    else -> {
                        val ult = db.pagamentoDao().ultimaCompetencia(aluno.id)
                        if (ult != null && ult >= comp) d++ else a++
                    }
                }
            }
            emDia = d; atrasados = a; bolsistas = b; isentos = i
        }
    }

    val emAberto = receitaPrevista - recebidoMes
    val taxa = if (receitaPrevista > 0) (recebidoMes / receitaPrevista * 100) else 0.0

    Column(modifier = Modifier.fillMaxSize()) {
        NukTopBar("📊 Dashboard", onMenuClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header com logo
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.size(80.dp).clip(CircleShape).align(Alignment.CenterHorizontally)
            )
            Text("ESCOLA NUKLEAR", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NukPurple,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Text("De Artes Marciais — ${hoje.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                fontSize = 13.sp, color = NukLightGray,
                modifier = Modifier.align(Alignment.CenterHorizontally))

            // Stats row 1
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Total Alunos", "$totalAlunos", NukPurple, Modifier.weight(1f).clickable { onNavigate("alunos") })
                StatCard("Kids", "$totalKids", NukBlue, Modifier.weight(1f).clickable { onNavigate("alunos") })
                StatCard("Adultos", "$totalAdultos", NukBlack, Modifier.weight(1f).clickable { onNavigate("alunos") })
            }

            // Stats row 2
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Presentes Hoje", "$presentesHoje", NukGreen, Modifier.weight(1f).clickable { onNavigate("presencas") })
                StatCard("Inadimplentes", "$atrasados", NukRedDark, Modifier.weight(1f).clickable { onNavigate("inadimplentes") })
            }

            // Financeiro (toque para ver o histórico financeiro)
            NukCard(modifier = Modifier.clickable { onNavigate("historico") }) {
                SectionTitle("FINANCEIRO DO MÊS", "💰")
                Text("Receita Prevista: R$ ${"%.2f".format(receitaPrevista)}",
                    fontSize = 13.sp, color = NukWhite)
                Text("Recebido: R$ ${"%.2f".format(recebidoMes)}",
                    fontSize = 13.sp, color = NukGreen)
                Text("Em Aberto: R$ ${"%.2f".format(emAberto)}",
                    fontSize = 13.sp, color = NukRedLight)
                Text("Taxa Adimplência: ${"%.1f".format(taxa)}%",
                    fontSize = 13.sp, color = NukYellow)
            }

            // Status
            NukCard {
                SectionTitle("STATUS DOS ALUNOS", "📊")
                Text("🟢 Em dia: $emDia", fontSize = 13.sp, color = NukGreen)
                Text("🔴 Atrasados: $atrasados", fontSize = 13.sp, color = NukRedLight)
                Text("🟡 Bolsistas: $bolsistas", fontSize = 13.sp, color = NukYellow)
                Text("🔵 Isentos: $isentos", fontSize = 13.sp, color = NukBlue)
            }

            // Por horário
            if (porHorario.isNotEmpty()) {
                NukCard {
                    SectionTitle("ALUNOS POR HORÁRIO", "🕐")
                    porHorario.forEach { h ->
                        Text("${h.horario}: ${h.total} alunos", fontSize = 13.sp, color = NukWhite)
                    }
                }
            }

            // Aniversariantes (toque para ver a lista completa)
            if (aniversariantes.isNotEmpty()) {
                NukCard(modifier = Modifier.clickable { onNavigate("aniversariantes") }) {
                    SectionTitle("ANIVERSARIANTES DO MÊS", "🎂")
                    aniversariantes.forEach { a ->
                        Text("${a.nome} — ${a.dataNascimento}", fontSize = 13.sp, color = NukWhite)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}
