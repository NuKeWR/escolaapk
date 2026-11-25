package com.nuklear.escola.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

/** Aba Aulas: marcar quem veio à aula. Toque na linha do aluno para marcar/desmarcar. */
@Composable
fun PresencasScreen(
    onMenuClick: () -> Unit,
    onReconhecer: () -> Unit = {},
    onRelatorio: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.get(ctx) }
    val scope = rememberCoroutineScope()

    val hoje = LocalDate.now()
    var presDia by remember { mutableStateOf("%02d".format(hoje.dayOfMonth)) }
    var presMes by remember { mutableStateOf("%02d".format(hoje.monthValue)) }
    var presAno by remember { mutableStateOf(hoje.year.toString()) }

    var horarios by remember { mutableStateOf(listOf("Todos")) }
    var horarioSel by remember { mutableStateOf("Todos") }
    var alunos by remember { mutableStateOf<List<Aluno>>(emptyList()) }
    val checks = remember { mutableStateMapOf<String, Boolean>() }

    fun carregar() {
        scope.launch {
            horarios = listOf("Todos") + db.alunoDao().getHorarios()
            alunos = if (horarioSel != "Todos") {
                db.alunoDao().getAtivosByHorario(horarioSel)
            } else db.alunoDao().getAtivos()

            val pres = db.presencaDao().getPresentes("$presAno-$presMes-$presDia").toSet()
            checks.clear()
            alunos.forEach { a -> checks[a.nome] = a.nome in pres }
        }
    }

    // Recarrega sozinho ao trocar a data ou o horário (não precisa mais do botão de atualizar)
    LaunchedEffect(presDia, presMes, presAno, horarioSel) { carregar() }

    val marcados = checks.count { it.value }
    val ehHoje = "$presAno-$presMes-$presDia" == hoje.toString()

    Column(modifier = Modifier.fillMaxSize()) {
        NukTopBar("Aulas", onMenuClick)

        // Atalhos para as outras ferramentas de presença
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AtalhoPill(Icons.Default.Face, "Reconhecimento facial", onReconhecer, Modifier.weight(1f))
            AtalhoPill(Icons.Default.BarChart, "Relatórios", onRelatorio, Modifier.weight(1f))
        }

        // Data
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.weight(0.8f)) { DropdownField("Dia", presDia, AlunoOpcoes.diasDoMes()) { presDia = it } }
            Box(Modifier.weight(1f)) { DropdownField("Mês", presMes, AlunoOpcoes.meses.map { it.first }) { presMes = it } }
            Box(Modifier.weight(1f)) {
                DropdownField("Ano", presAno, listOf(hoje.year.toString(), (hoje.year - 1).toString())) { presAno = it }
            }
        }

        // Horário + resumo
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PillDropdown(
                rotulo = if (horarioSel == "Todos") "Todos os horários" else horarioSel,
                opcoes = horarios,
                onSelecionar = { horarioSel = it }
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${if (ehHoje) "Hoje" else "$presDia/$presMes"} · $marcados de ${alunos.size}",
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NukPurpleLight
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(alunos, key = { it.id }) { aluno ->
                val presente = checks[aluno.nome] ?: false
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { checks[aluno.nome] = !presente },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (presente) NukPurple.copy(alpha = 0.14f) else NukDarkGray
                    ),
                    border = BorderStroke(1.dp, if (presente) NukPurple.copy(alpha = 0.6f) else NukBorder)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(nome = aluno.nome, tamanho = 40.dp, foto = aluno.fotoPath)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(aluno.nome, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NukWhite, maxLines = 1)
                            Text(
                                if (aluno.horario.isNotBlank()) aluno.horario else aluno.turma,
                                fontSize = 11.sp, color = NukLightGray, maxLines = 1
                            )
                        }
                        Checkbox(
                            checked = presente,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = NukPurple,
                                checkmarkColor = Color.White,
                                uncheckedColor = NukLightGray
                            )
                        )
                    }
                }
            }
        }

        // Ações fixas no rodapé
        Row(
            Modifier.fillMaxWidth().background(NukBlack).padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { alunos.forEach { a -> checks[a.nome] = true } },
                colors = ButtonDefaults.buttonColors(containerColor = NukMedGray),
                modifier = Modifier.weight(0.45f).height(52.dp)
            ) { Text("Marcar todos", fontSize = 12.sp) }
            Button(
                onClick = {
                    val data = "$presAno-$presMes-$presDia"
                    scope.launch {
                        if (horarioSel != "Todos") db.presencaDao().deleteByDataAndHorario(data, horarioSel)
                        else db.presencaDao().deleteByData(data)
                        val nomes = checks.filter { it.value }.keys
                        nomes.forEach { nome -> db.presencaDao().insert(Presenca(data = data, aluno = nome)) }
                        Toast.makeText(ctx, "✅ ${nomes.size} presença(s) salvas!", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NukPurple),
                modifier = Modifier.weight(1f).height(52.dp)
            ) { Text("Salvar presenças ($marcados)", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun AtalhoPill(icone: ImageVector, texto: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier.clip(forma).background(NukDarkGray).border(1.dp, NukBorder, forma)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icone, contentDescription = null, tint = NukPurpleLight, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = NukWhite, maxLines = 1)
    }
}
