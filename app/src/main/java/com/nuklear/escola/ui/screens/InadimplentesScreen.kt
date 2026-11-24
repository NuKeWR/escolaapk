package com.nuklear.escola.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

data class Inadimplente(
    val aluno: Aluno,
    val status: String,
    val mesesAtraso: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InadimplentesScreen(
    onMenuClick: () -> Unit,
    onPagamentos: (Long) -> Unit
) {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.get(ctx) }
    val scope = rememberCoroutineScope()

    var lista by remember { mutableStateOf<List<Inadimplente>>(emptyList()) }
    var horarios by remember { mutableStateOf(listOf("Todos")) }
    var horarioSel by remember { mutableStateOf("Todos") }
    var expandedHorario by remember { mutableStateOf(false) }

    fun carregar() {
        scope.launch {
            horarios = listOf("Todos") + db.alunoDao().getHorarios()
            val alunos = if (horarioSel != "Todos") {
                db.alunoDao().getAtivosByHorario(horarioSel)
            } else {
                db.alunoDao().getAtivos()
            }
            val hoje = LocalDate.now()
            val comp = "${hoje.year}-${"%02d".format(hoje.monthValue)}"

            lista = alunos.mapNotNull { a ->
                if (a.tipo.lowercase() in listOf("bolsista", "professor")) return@mapNotNull null
                val ult = db.pagamentoDao().ultimaCompetencia(a.id)
                if (ult != null && ult >= comp) return@mapNotNull null

                val meses = if (ult == null) {
                    0
                } else {
                    val (ay, am) = comp.split("-").map { it.toInt() }
                    val (uy, um) = ult.split("-").map { it.toInt() }
                    (ay - uy) * 12 + (am - um)
                }
                val status = if (ult == null) "🔴 Sem pagamento" else "🔴 Atrasado"
                Inadimplente(a, status, meses)
            }
        }
    }

    LaunchedEffect(Unit) { carregar() }
    LaunchedEffect(horarioSel) { carregar() }

    Column(modifier = Modifier.fillMaxSize()) {
        NukTopBar("🔴 Inadimplentes", onMenuClick)

        // Filtro horário
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            ExposedDropdownMenuBox(expanded = expandedHorario, onExpandedChange = { expandedHorario = it }) {
                OutlinedTextField(
                    value = horarioSel, onValueChange = {}, readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    label = { Text("Filtrar por horário") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NukPurple, unfocusedBorderColor = NukMedGray,
                        focusedTextColor = NukWhite, unfocusedTextColor = NukWhite,
                        focusedLabelColor = NukPurple, unfocusedLabelColor = NukLightGray
                    )
                )
                ExposedDropdownMenu(expanded = expandedHorario, onDismissRequest = { expandedHorario = false }) {
                    horarios.forEach { h ->
                        DropdownMenuItem(text = { Text(h) }, onClick = { horarioSel = h; expandedHorario = false })
                    }
                }
            }
        }

        Text("🔴 ${lista.size} inadimplente(s)", fontSize = 14.sp, fontWeight = FontWeight.Bold,
            color = NukRedLight, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))

        LazyColumn(
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(lista) { item ->
                NukCard {
                    Text(item.aluno.nome, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NukWhite)
                    Text("${item.status} | ${item.mesesAtraso} mês(es) atraso",
                        fontSize = 12.sp, color = NukRedLight)
                    Text("R$ ${"%.2f".format(item.aluno.valorMensalidade)} | ${item.aluno.turma} | ${item.aluno.horario}",
                        fontSize = 11.sp, color = NukLightGray)
                    if (item.aluno.telefone.isNotBlank()) {
                        Text("📱 ${item.aluno.telefone}", fontSize = 11.sp, color = NukBlue)
                        Button(
                            onClick = {
                                val quem = item.aluno.responsavel.ifBlank { item.aluno.nome }
                                val valorTxt = "%.2f".format(item.aluno.valorMensalidade)
                                val msg = "Olá, $quem! Tudo bem? Aqui é da Escola Nuklear de Artes Marciais. " +
                                    "Estamos passando para lembrar que a mensalidade de ${item.aluno.nome} " +
                                    "(R$ $valorTxt) está em aberto. Pode nos confirmar o pagamento? " +
                                    "Se já pagou, é só desconsiderar. Obrigado! 🥋"
                                abrirWhatsApp(ctx, item.aluno.telefone, msg)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) { Text("💬 Cobrar no WhatsApp", color = Color.White) }
                    }
                    Button(
                        onClick = { onPagamentos(item.aluno.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = NukPurple),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) { Text("💳 Pagamentos") }
                }
            }
        }
    }
}

// Abre o WhatsApp na conversa com o telefone do aluno, com a mensagem já escrita.
private fun abrirWhatsApp(ctx: Context, telefone: String, mensagem: String) {
    var numero = telefone.filter { it.isDigit() }.trimStart('0')
    // Telefone sem código do país (DDD + número = 10 ou 11 dígitos): acrescenta o 55 do Brasil
    if (numero.length <= 11) numero = "55$numero"
    try {
        val texto = java.net.URLEncoder.encode(mensagem, "UTF-8").replace("+", "%20")
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$numero?text=$texto")))
    } catch (e: Exception) {
        Toast.makeText(ctx, "Não foi possível abrir o WhatsApp", Toast.LENGTH_SHORT).show()
    }
}
