package com.nuklear.escola.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun PresencasScreen(onMenuClick: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.get(ctx) }
    val scope = rememberCoroutineScope()

    val hoje = LocalDate.now()
    var presDia by remember { mutableStateOf("%02d".format(hoje.dayOfMonth)) }
    var presMes by remember { mutableStateOf("%02d".format(hoje.monthValue)) }
    var presAno by remember { mutableStateOf(hoje.year.toString()) }

    var horarios by remember { mutableStateOf(listOf("Todos")) }
    var horarioSel by remember { mutableStateOf("Todos") }
    var expandedHorario by remember { mutableStateOf(false) }
    var alunos by remember { mutableStateOf<List<Aluno>>(emptyList()) }
    val checks = remember { mutableStateMapOf<String, Boolean>() }

    fun dataSel() = "$presAno-$presMes-$presDia"

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

    LaunchedEffect(Unit) { carregar() }
    LaunchedEffect(horarioSel) { carregar() }

    Column(modifier = Modifier.fillMaxSize()) {
        NukTopBar("✅ Presenças", onMenuClick)

        // Data com dropdowns
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.weight(0.8f)) { DropdownField("Dia", presDia, AlunoOpcoes.diasDoMes()) { presDia = it } }
            Box(Modifier.weight(1f)) { DropdownField("Mês", presMes, AlunoOpcoes.meses.map { it.first }) { presMes = it } }
            Box(Modifier.weight(1f)) { DropdownField("Ano", presAno, listOf(hoje.year.toString(), (hoje.year - 1).toString())) { presAno = it } }
            Button(onClick = { carregar() }, Modifier.weight(0.5f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NukPurple),
                contentPadding = PaddingValues(4.dp)) { Text("🔄") }
        }

        // Filtro horário
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExposedDropdownMenuBox(expanded = expandedHorario, onExpandedChange = { expandedHorario = it },
                modifier = Modifier.weight(1f)) {
                OutlinedTextField(value = horarioSel, onValueChange = {}, readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true,
                    label = { Text("Horário", fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NukPurple, unfocusedBorderColor = NukMedGray,
                        focusedTextColor = NukWhite, unfocusedTextColor = NukWhite,
                        focusedLabelColor = NukPurple, unfocusedLabelColor = NukLightGray))
                ExposedDropdownMenu(expanded = expandedHorario, onDismissRequest = { expandedHorario = false }) {
                    horarios.forEach { h -> DropdownMenuItem(text = { Text(h) }, onClick = { horarioSel = h; expandedHorario = false }) }
                }
            }
        }

        val ehHoje = "$presAno-$presMes-$presDia" == hoje.toString()
        Text(
            "${if (ehHoje) "📅 HOJE" else "📅 $presAno-$presMes-$presDia"} — ${alunos.size} aluno(s) — ${checks.count { it.value }} presente(s)",
            fontSize = 13.sp, color = NukPurple, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))

        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(alunos, key = { it.id }) { aluno ->
                Card(colors = CardDefaults.cardColors(containerColor = NukDarkGray), shape = RoundedCornerShape(8.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = checks[aluno.nome] ?: false,
                            onCheckedChange = { checks[aluno.nome] = it },
                            colors = CheckboxDefaults.colors(checkedColor = NukPurple, checkmarkColor = NukWhite))
                        Text(aluno.nome, fontSize = 14.sp, color = NukWhite, modifier = Modifier.weight(1f))
                        Text("${aluno.turma} ${aluno.horario}", fontSize = 10.sp, color = NukLightGray)
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val data = "$presAno-$presMes-$presDia"
                scope.launch {
                    if (horarioSel != "Todos") db.presencaDao().deleteByDataAndHorario(data, horarioSel)
                    else db.presencaDao().deleteByData(data)
                    val marcados = checks.filter { it.value }.keys
                    marcados.forEach { nome -> db.presencaDao().insert(Presenca(data = data, aluno = nome)) }
                    Toast.makeText(ctx, "✅ ${marcados.size} presença(s) salvas!", Toast.LENGTH_SHORT).show()
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = NukGreen),
                modifier = Modifier.weight(1f).height(50.dp)) {
                Text("💾 SALVAR", fontWeight = FontWeight.Bold)
            }
            Button(onClick = { alunos.forEach { a -> checks[a.nome] = true } },
                colors = ButtonDefaults.buttonColors(containerColor = NukPurple),
                modifier = Modifier.weight(0.5f).height(50.dp)) { Text("📋 TODOS", fontSize = 12.sp) }
        }
    }
}
