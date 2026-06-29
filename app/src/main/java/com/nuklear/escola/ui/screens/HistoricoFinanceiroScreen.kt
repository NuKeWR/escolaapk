package com.nuklear.escola.ui.screens

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
import com.nuklear.escola.data.db.ReceitaMensal
import com.nuklear.escola.data.db.TotalPorAluno
import com.nuklear.escola.ui.components.*
import com.nuklear.escola.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HistoricoFinanceiroScreen(onMenuClick: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.get(ctx) }
    val scope = rememberCoroutineScope()

    var receitaMensal by remember { mutableStateOf<List<ReceitaMensal>>(emptyList()) }
    var totalPorAluno by remember { mutableStateOf<List<TotalPorAluno>>(emptyList()) }

    LaunchedEffect(Unit) {
        scope.launch {
            receitaMensal = db.pagamentoDao().receitaMensal()
            totalPorAluno = db.pagamentoDao().totalPorAluno()
        }
    }

    val totalGeral = receitaMensal.sumOf { it.total }

    Column(modifier = Modifier.fillMaxSize()) {
        NukTopBar("💰 Histórico Financeiro", onMenuClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionTitle("RECEITA MENSAL", "💰")
            receitaMensal.forEach { rm ->
                NukCard {
                    Text(rm.competencia, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NukWhite)
                    Text("R$ ${"%.2f".format(rm.total)}", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        color = NukGreen)
                }
            }

            Text("Total últimos 12 meses: R$ ${"%.2f".format(totalGeral)}",
                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NukYellow,
                modifier = Modifier.padding(vertical = 4.dp))

            Spacer(Modifier.height(8.dp))
            SectionTitle("TOTAL POR ALUNO", "👤")

            totalPorAluno.forEach { ta ->
                NukCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(ta.nome, fontSize = 13.sp, color = NukWhite, modifier = Modifier.weight(1f))
                        Text("R$ ${"%.2f".format(ta.total)}", fontSize = 13.sp,
                            fontWeight = FontWeight.Bold, color = NukGreen)
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}
