package com.nuklear.escola.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuklear.escola.data.db.AppDatabase
import com.nuklear.escola.data.model.Aluno
import com.nuklear.escola.ui.components.*
import com.nuklear.escola.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlunosScreen(
    onMenuClick: () -> Unit,
    onAlunoClick: (Long) -> Unit
) {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.get(ctx) }
    val scope = rememberCoroutineScope()

    var alunos by remember { mutableStateOf<List<Aluno>>(emptyList()) }
    var filteredAlunos by remember { mutableStateOf<List<Aluno>>(emptyList()) }
    var busca by remember { mutableStateOf("") }
    var filtroAtivo by remember { mutableStateOf(1) } // 1=ativos, 0=inativos, -1=todos
    var horarios by remember { mutableStateOf(listOf("Todos")) }
    var horarioSel by remember { mutableStateOf("Todos") }
    var expandedHorario by remember { mutableStateOf(false) }

    fun carregar() {
        scope.launch {
            horarios = listOf("Todos") + db.alunoDao().getHorarios()
            val list = when {
                horarioSel != "Todos" && filtroAtivo >= 0 ->
                    db.alunoDao().getAtivosByHorario(horarioSel).let { l ->
                        if (filtroAtivo == 0) l.filter { it.ativo == 0 } else l
                    }
                filtroAtivo == 1 -> db.alunoDao().getAtivos()
                filtroAtivo == 0 -> db.alunoDao().getInativos()
                else -> db.alunoDao().getAll()
            }
            alunos = list
            filteredAlunos = if (busca.isBlank()) list
            else list.filter { it.nome.contains(busca, ignoreCase = true) }
        }
    }

    LaunchedEffect(Unit) { carregar() }
    LaunchedEffect(filtroAtivo, horarioSel) { carregar() }

    Column(modifier = Modifier.fillMaxSize()) {
        NukTopBar("👥 Alunos", onMenuClick)

        // Search + Filter
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = busca,
                onValueChange = {
                    busca = it
                    filteredAlunos = if (it.isBlank()) alunos
                    else alunos.filter { a -> a.nome.contains(it, ignoreCase = true) }
                },
                placeholder = { Text("🔍 Buscar...", color = NukLightGray) },
                modifier = Modifier.weight(1f).height(52.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NukPurple,
                    unfocusedBorderColor = NukMedGray,
                    focusedTextColor = NukWhite,
                    unfocusedTextColor = NukWhite
                )
            )

            ExposedDropdownMenuBox(expanded = expandedHorario, onExpandedChange = { expandedHorario = it }) {
                OutlinedTextField(
                    value = horarioSel,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.width(120.dp).height(52.dp).menuAnchor(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NukPurple,
                        unfocusedBorderColor = NukMedGray,
                        focusedTextColor = NukWhite,
                        unfocusedTextColor = NukWhite
                    )
                )
                ExposedDropdownMenu(expanded = expandedHorario, onDismissRequest = { expandedHorario = false }) {
                    horarios.forEach { h ->
                        DropdownMenuItem(
                            text = { Text(h) },
                            onClick = { horarioSel = h; expandedHorario = false }
                        )
                    }
                }
            }
        }

        // Toggle buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(1 to "Ativos", 0 to "Inativos", -1 to "Todos").forEach { (v, label) ->
                FilterChip(
                    selected = filtroAtivo == v,
                    onClick = { filtroAtivo = v },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (v == 1) NukGreen else if (v == 0) NukPurple else NukMedGray,
                        selectedLabelColor = NukWhite
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text("${filteredAlunos.size} aluno(s)", fontSize = 12.sp, color = NukLightGray,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))

        LazyColumn(
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filteredAlunos, key = { it.id }) { aluno ->
                val status = getStatusText(aluno, db, scope)
                AlunoListItem(
                    nome = aluno.nome,
                    subtitulo = "${aluno.turma}  •  ${aluno.horario}",
                    status = status.first,
                    statusColor = status.second,
                    onClick = { onAlunoClick(aluno.id) }
                )
            }
        }
    }
}

@Composable
private fun getStatusText(
    aluno: Aluno,
    db: AppDatabase,
    scope: kotlinx.coroutines.CoroutineScope
): Pair<String, androidx.compose.ui.graphics.Color> {
    var text by remember(aluno.id) { mutableStateOf("..." to NukLightGray) }

    LaunchedEffect(aluno.id) {
        val hoje = java.time.LocalDate.now()
        val comp = "${hoje.year}-${"%02d".format(hoje.monthValue)}"
        val s = when {
            aluno.tipo.lowercase() == "bolsista" -> "🟡 Bolsista" to NukYellow
            aluno.tipo.lowercase() == "professor" -> "🔵 Isento" to NukBlue
            else -> {
                val ult = db.pagamentoDao().ultimaCompetencia(aluno.id)
                if (ult != null && ult >= comp) "🟢 Em dia" to NukGreen
                else "🔴 Atrasado" to NukRedLight
            }
        }
        text = s
    }

    return text
}
