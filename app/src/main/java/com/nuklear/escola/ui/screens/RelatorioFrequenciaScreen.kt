package com.nuklear.escola.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuklear.escola.data.db.AppDatabase
import com.nuklear.escola.data.db.RankingItem
import com.nuklear.escola.ui.components.*
import com.nuklear.escola.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun RelatorioFrequenciaScreen(
    onMenuClick: () -> Unit,
    onFrequenciaAluno: (String) -> Unit
) {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.get(ctx) }
    val scope = rememberCoroutineScope()

    var ranking by remember { mutableStateOf<List<RankingItem>>(emptyList()) }
    var freqMensal by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }

    LaunchedEffect(Unit) {
        scope.launch {
            ranking = db.presencaDao().ranking()

            val hoje = LocalDate.now()
            val meses = mutableListOf<Pair<String, Int>>()
            for (i in 5 downTo 0) {
                val d = hoje.minusMonths(i.toLong())
                val prefix = "${d.year}-${"%02d".format(d.monthValue)}"
                val total = db.presencaDao().countMes("$prefix-")
                meses.add(prefix to total)
            }
            freqMensal = meses
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NukTopBar("📈 Relatório Frequência", onMenuClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionTitle("FREQUÊNCIA MENSAL", "📈")
            freqMensal.forEach { (mes, total) ->
                NukCard {
                    Text("$mes: $total alunos únicos", fontSize = 14.sp, color = NukWhite)
                }
            }

            Spacer(Modifier.height(8.dp))
            SectionTitle("RANKING DE FREQUÊNCIA", "🏆")

            ranking.forEachIndexed { i, item ->
                val medalha = when (i) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "${i + 1}." }
                NukCard(modifier = Modifier.clickable { onFrequenciaAluno(item.aluno) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$medalha ${item.aluno}", fontSize = 13.sp, color = NukWhite,
                            modifier = Modifier.weight(1f))
                        Text("${item.total}x", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = NukPurple)
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}
