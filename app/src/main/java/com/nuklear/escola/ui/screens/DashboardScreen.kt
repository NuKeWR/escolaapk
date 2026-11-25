package com.nuklear.escola.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Warning
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
import com.nuklear.escola.data.Perfil
import com.nuklear.escola.data.db.AppDatabase
import com.nuklear.escola.data.db.HorarioCount
import com.nuklear.escola.data.model.Aluno
import com.nuklear.escola.ui.components.*
import com.nuklear.escola.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

private val DIAS_ABREV = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")

/** Tela inicial: resumo da academia, acesso rápido e evolução das presenças. */
@Composable
fun DashboardScreen(
    onMenuClick: () -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.get(ctx) }
    val nome = remember { Perfil.nome(ctx) }

    var totalAlunos by remember { mutableStateOf(0) }
    var novosNoMes by remember { mutableStateOf(0) }
    var receitaMes by remember { mutableStateOf(0.0) }
    var receitaAnterior by remember { mutableStateOf(0.0) }
    var presentesHoje by remember { mutableStateOf(0) }
    var presentesOntem by remember { mutableStateOf(0) }
    var atrasados by remember { mutableStateOf(0) }
    var emAberto by remember { mutableStateOf(0.0) }
    var serie by remember { mutableStateOf<List<Int>>(emptyList()) }
    var rotulos by remember { mutableStateOf<List<String>>(emptyList()) }
    var porHorario by remember { mutableStateOf<List<HorarioCount>>(emptyList()) }
    var aniversariantes by remember { mutableStateOf<List<Aluno>>(emptyList()) }

    val hoje = LocalDate.now()
    val comp = YearMonth.from(hoje).toString() // "2026-11", mesmo formato da competência

    LaunchedEffect(Unit) {
        val ativos = db.alunoDao().getAtivos()
        totalAlunos = ativos.size
        novosNoMes = ativos.count { it.dataMatricula.startsWith(comp) }
        receitaMes = db.pagamentoDao().recebidoMes(comp)
        receitaAnterior = db.pagamentoDao().recebidoMes(YearMonth.from(hoje).minusMonths(1).toString())
        presentesHoje = db.presencaDao().countPresentes(hoje.toString())
        presentesOntem = db.presencaDao().countPresentes(hoje.minusDays(1).toString())
        porHorario = db.alunoDao().countByHorario()
        aniversariantes = db.alunoDao().aniversariantes("%02d".format(hoje.monthValue))

        var atras = 0
        var aberto = 0.0
        for (a in ativos) {
            if (a.tipo.lowercase() in listOf("bolsista", "professor")) continue
            val ult = db.pagamentoDao().ultimaCompetencia(a.id)
            if (ult == null || ult < comp) {
                atras++
                aberto += a.valorMensalidade
            }
        }
        atrasados = atras
        emAberto = aberto

        val dias = (6 downTo 0).map { hoje.minusDays(it.toLong()) }
        serie = dias.map { db.presencaDao().countPresentes(it.toString()) }
        rotulos = dias.map { DIAS_ABREV[it.dayOfWeek.value - 1] }
    }

    // Variações mostradas nos cartões
    val detalheAlunos = if (novosNoMes > 0) "↑ +$novosNoMes no mês" else ""
    val pct = if (receitaAnterior > 0) ((receitaMes - receitaAnterior) / receitaAnterior * 100).roundToInt() else null
    val detalheReceita = when {
        pct == null -> ""
        pct >= 0 -> "↑ +$pct% vs mês anterior"
        else -> "↓ $pct% vs mês anterior"
    }
    val difPresenca = presentesHoje - presentesOntem
    val detalhePresenca = when {
        presentesHoje == 0 && presentesOntem == 0 -> ""
        difPresenca > 0 -> "↑ +$difPresenca vs ontem"
        difPresenca < 0 -> "↓ $difPresenca vs ontem"
        else -> "= igual a ontem"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // ─── Cabeçalho ───
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.size(46.dp).clip(CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("NUKLEAR", fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = NukWhite)
                Text("ESCOLA DE LUTAS", fontSize = 10.sp, letterSpacing = 3.sp, color = NukLightGray)
            }
            // Avisos: leva às pendências; o ponto laranja aparece quando há alguém em atraso
            Box {
                IconButton(onClick = { onNavigate("inadimplentes") }) {
                    Icon(Icons.Default.NotificationsNone, contentDescription = "Avisos", tint = NukWhite)
                }
                if (atrasados > 0) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 10.dp)
                            .size(9.dp).clip(CircleShape).background(NukOrange)
                    )
                }
            }
        }

        Column {
            Text(
                if (nome.isBlank()) "Olá 👋" else "Olá, $nome 👋",
                fontSize = 26.sp, fontWeight = FontWeight.Bold, color = NukWhite
            )
            Text("Resumo da academia", fontSize = 14.sp, color = NukLightGray)
        }

        // ─── Indicadores (cada cartão leva à tela correspondente) ───
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                icone = Icons.Default.Groups, titulo = "Alunos ativos", valor = "$totalAlunos",
                detalhe = detalheAlunos, detalheCor = NukGreen,
                modifier = Modifier.weight(1f).fillMaxHeight().clickable { onNavigate("alunos") }
            )
            MetricCard(
                icone = Icons.Default.AccountBalanceWallet, titulo = "Receita do mês", valor = moedaCurta(receitaMes),
                detalhe = detalheReceita, detalheCor = if ((pct ?: 0) >= 0) NukGreen else NukOrange,
                modifier = Modifier.weight(1f).fillMaxHeight().clickable { onNavigate("financeiro") }
            )
        }
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                icone = Icons.Default.EventAvailable, titulo = "Presenças hoje", valor = "$presentesHoje",
                detalhe = detalhePresenca, detalheCor = if (difPresenca >= 0) NukGreen else NukOrange,
                modifier = Modifier.weight(1f).fillMaxHeight().clickable { onNavigate("presencas") }
            )
            MetricCard(
                icone = Icons.Default.Warning, titulo = "Pendências", valor = "$atrasados",
                detalhe = if (atrasados > 0) "${moedaCurta(emAberto)} em aberto" else "Tudo em dia",
                detalheCor = if (atrasados > 0) NukOrange else NukGreen,
                modifier = Modifier.weight(1f).fillMaxHeight().clickable { onNavigate("inadimplentes") }
            )
        }

        // ─── Acesso rápido ───
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("Acesso rápido", "Ver tudo") { onNavigate("configuracoes") }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickAction(Icons.Default.EventAvailable, "Presença", { onNavigate("presencas") })
                QuickAction(Icons.Default.Payments, "Pagar", { onNavigate("pagamento_rapido") })
                QuickAction(Icons.Default.People, "Alunos", { onNavigate("alunos") })
                QuickAction(Icons.Default.Face, "Rosto", { onNavigate("reconhecimento") })
                QuickAction(Icons.Default.BarChart, "Relatórios", { onNavigate("relatorio") })
                QuickAction(Icons.Default.Cake, "Aniversários", { onNavigate("aniversariantes") })
            }
        }

        // ─── Evolução das presenças ───
        NukCard(modifier = Modifier.clickable { onNavigate("presencas") }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Evolução de Presenças", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = NukWhite, modifier = Modifier.weight(1f)
                )
                Text("Últimos 7 dias", fontSize = 12.sp, color = NukLightGray)
            }
            Spacer(Modifier.height(10.dp))
            if (serie.isNotEmpty()) LineChart(valores = serie, rotulos = rotulos)
        }

        // ─── Alunos por horário ───
        if (porHorario.isNotEmpty()) {
            NukCard {
                SectionTitle("ALUNOS POR HORÁRIO")
                porHorario.forEach { h ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(h.horario, fontSize = 13.sp, color = NukWhite, modifier = Modifier.weight(1f))
                        Text("${h.total}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NukPurpleLight)
                    }
                }
            }
        }

        // ─── Aniversariantes do mês ───
        if (aniversariantes.isNotEmpty()) {
            NukCard(modifier = Modifier.clickable { onNavigate("aniversariantes") }) {
                SectionTitle("ANIVERSARIANTES DO MÊS")
                aniversariantes.take(5).forEach { a ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(a.nome, fontSize = 13.sp, color = NukWhite, modifier = Modifier.weight(1f))
                        Text(diaMes(a.dataNascimento), fontSize = 13.sp, color = NukLightGray)
                    }
                }
                if (aniversariantes.size > 5) {
                    Text("+ ${aniversariantes.size - 5} no mês", fontSize = 12.sp, color = NukPurpleLight,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

/** "2010-03-15" -> "15/03" */
private fun diaMes(dataIso: String): String {
    val p = dataIso.split("-")
    return if (p.size == 3) "${p[2]}/${p[1]}" else dataIso
}
