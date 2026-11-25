package com.nuklear.escola.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuklear.escola.data.db.AppDatabase
import com.nuklear.escola.data.model.Aluno
import com.nuklear.escola.ui.components.*
import com.nuklear.escola.ui.theme.*
import kotlinx.coroutines.launch
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlunosScreen(
    onMenuClick: () -> Unit,
    onAlunoClick: (Long) -> Unit,
    onCadastrar: () -> Unit = {}
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

    val rotuloFiltro = when (filtroAtivo) {
        1 -> "ativos"
        0 -> "inativos"
        else -> "no total"
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            NukTopBar("Alunos", onMenuClick)

            // Busca
            OutlinedTextField(
                value = busca,
                onValueChange = {
                    busca = it
                    filteredAlunos = if (it.isBlank()) alunos
                    else alunos.filter { a -> a.nome.contains(it, ignoreCase = true) }
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NukLightGray) },
                placeholder = { Text("Buscar aluno...", color = NukLightGray) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NukPurple,
                    unfocusedBorderColor = NukBorder,
                    focusedContainerColor = NukDarkGray,
                    unfocusedContainerColor = NukDarkGray,
                    focusedTextColor = NukWhite,
                    unfocusedTextColor = NukWhite,
                    cursorColor = NukPurple
                )
            )

            // Filtros: situação + horário
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(1 to "Ativos", 0 to "Inativos", -1 to "Todos").forEach { (v, label) ->
                    FilterChip(
                        selected = filtroAtivo == v,
                        onClick = { filtroAtivo = v },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = NukDarkGray,
                            labelColor = NukLightGray,
                            selectedContainerColor = NukPurple,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                PillDropdown(
                    rotulo = if (horarioSel == "Todos") "Todos os horários" else horarioSel,
                    opcoes = horarios,
                    onSelecionar = { horarioSel = it }
                )
            }

            Row(
                Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(NukGreen))
                Spacer(Modifier.width(8.dp))
                Text("${filteredAlunos.size} aluno(s) $rotuloFiltro", fontSize = 13.sp, color = NukLightGray)
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredAlunos, key = { it.id }) { aluno ->
                    val status = statusMensalidade(aluno, db)
                    AlunoListItem(
                        nome = aluno.nome,
                        subtitulo = if (aluno.faixaAtual.isNotBlank()) "Faixa ${aluno.faixaAtual.lowercase()}"
                        else aluno.turma,
                        subtituloCor = corDaFaixa(aluno.faixaAtual),
                        status = status.first,
                        statusColor = status.second,
                        foto = aluno.fotoPath,
                        onClick = { onAlunoClick(aluno.id) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onCadastrar,
            containerColor = NukPurple,
            contentColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Novo aluno")
        }
    }
}

/** Situação da mensalidade de cada aluno, mostrada como selo na lista. */
@Composable
private fun statusMensalidade(aluno: Aluno, db: AppDatabase): Pair<String, Color> {
    var estado by remember(aluno.id, aluno.ativo) { mutableStateOf("..." to NukLightGray) }

    LaunchedEffect(aluno.id, aluno.ativo) {
        val comp = YearMonth.now().toString()
        estado = when {
            aluno.ativo == 0 -> "Inativo" to NukLightGray
            aluno.tipo.lowercase() == "bolsista" -> "Bolsista" to NukYellow
            aluno.tipo.lowercase() == "professor" -> "Isento" to NukBlue
            else -> {
                val ult = db.pagamentoDao().ultimaCompetencia(aluno.id)
                if (ult != null && ult >= comp) "Em dia" to NukGreen
                else "Pendente" to NukOrange
            }
        }
    }

    return estado
}
