package com.nuklear.escola.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.nuklear.escola.data.model.Aluno
import com.nuklear.escola.ui.components.*
import com.nuklear.escola.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate

// ═══════════════════════════════════════════
// Opções pré-definidas para dropdowns
// ═══════════════════════════════════════════
object AlunoOpcoes {
    val sexos = listOf("M", "F")
    val tipos = listOf("Aluno", "Bolsista", "Professor")
    val turmas = listOf("Kids", "Adulto")
    val horarios = listOf(
        "18:00-19:00 BJJ Kids",
        "19:00-20:00 Muay Thai",
        "20:00-21:30 BJJ Adultos"
    )
    val modalidades = listOf("Jiu-Jitsu", "Muay Thai", "Judô", "Wrestling")
    val faixas = listOf(
        "Branca", "Cinza", "Amarela", "Laranja", "Verde",
        "Azul", "Roxa", "Marrom", "Preta",
        "Branca 1 grau", "Branca 2 graus", "Branca 3 graus", "Branca 4 graus",
        "Azul 1 grau", "Azul 2 graus", "Azul 3 graus", "Azul 4 graus",
        "Roxa 1 grau", "Roxa 2 graus", "Roxa 3 graus", "Roxa 4 graus",
        "Marrom 1 grau", "Marrom 2 graus", "Marrom 3 graus", "Marrom 4 graus",
        "Preta 1 grau", "Preta 2 graus", "Preta 3 graus"
    )
    val diasVencimento = (1..31).map { it.toString() }
    val formasPagamento = listOf("Pix", "Dinheiro", "Cartão", "Transferência")
    
    // Meses para selecionar
    val meses = listOf(
        "01" to "Janeiro", "02" to "Fevereiro", "03" to "Março",
        "04" to "Abril", "05" to "Maio", "06" to "Junho",
        "07" to "Julho", "08" to "Agosto", "09" to "Setembro",
        "10" to "Outubro", "11" to "Novembro", "12" to "Dezembro"
    )
    
    fun anosDisponiveis(): List<String> {
        val anoAtual = LocalDate.now().year
        return (anoAtual downTo anoAtual - 60).map { it.toString() }
    }
    
    fun diasDoMes(): List<String> = (1..31).map { "%02d".format(it) }
    
    fun competenciaAtual(): String {
        val hoje = LocalDate.now()
        return "${hoje.year}-${"%02d".format(hoje.monthValue)}"
    }
    
    fun competencias(): List<String> {
        val hoje = LocalDate.now()
        val lista = mutableListOf<String>()
        for (i in -2..12) {
            val d = hoje.minusMonths(i.toLong() * -1)
            lista.add("${d.year}-${"%02d".format(d.monthValue)}")
        }
        return lista.sorted().reversed()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastrarAlunoScreen(
    onMenuClick: () -> Unit,
    onSaved: () -> Unit
) {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.get(ctx) }
    val scope = rememberCoroutineScope()

    var nome by remember { mutableStateOf("") }
    var sexo by remember { mutableStateOf("M") }
    // Data nascimento em partes
    var dnDia by remember { mutableStateOf("01") }
    var dnMes by remember { mutableStateOf("01") }
    var dnAno by remember { mutableStateOf("2000") }
    var faixa by remember { mutableStateOf("Branca") }
    var tipo by remember { mutableStateOf("Aluno") }
    var valor by remember { mutableStateOf("0") }
    var diaVencimento by remember { mutableStateOf("5") }
    var telefone by remember { mutableStateOf("") }
    var responsavel by remember { mutableStateOf("") }
    var turma by remember { mutableStateOf("Adulto") }
    var horario by remember { mutableStateOf(AlunoOpcoes.horarios[2]) }
    var modalidade by remember { mutableStateOf("Jiu-Jitsu") }
    var observacoes by remember { mutableStateOf("") }
    var fotoUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()) { uri -> fotoUri = uri }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()) { _ -> }

    Column(modifier = Modifier.fillMaxSize()) {
        NukTopBar("➕ Cadastrar Aluno", onMenuClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionTitle("FOTO DO ALUNO", "📸")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { cameraLauncher.launch(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = NukPurple),
                    modifier = Modifier.weight(1f)) { Text("📷 Câmera") }
                Button(onClick = { galleryLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = NukDarkGray),
                    modifier = Modifier.weight(1f)) { Text("🖼 Galeria") }
            }
            if (fotoUri != null) Text("✅ Foto selecionada", fontSize = 12.sp, color = NukGreen)

            FormField("Nome completo *", nome) { nome = it }
            DropdownField("Sexo", sexo, AlunoOpcoes.sexos) { sexo = it }
            
            // Data nascimento como 3 dropdowns
            SectionTitle("DATA DE NASCIMENTO", "📅")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.weight(1f)) { DropdownField("Dia", dnDia, AlunoOpcoes.diasDoMes()) { dnDia = it } }
                Box(Modifier.weight(1.5f)) { DropdownField("Mês", dnMes, AlunoOpcoes.meses.map { it.first }) { dnMes = it } }
                Box(Modifier.weight(1.2f)) { DropdownField("Ano", dnAno, AlunoOpcoes.anosDisponiveis()) { dnAno = it } }
            }
            
            DropdownField("Faixa", faixa, AlunoOpcoes.faixas) { faixa = it }
            DropdownField("Tipo", tipo, AlunoOpcoes.tipos) { tipo = it }
            FormField("Valor Mensalidade", valor) { valor = it }
            DropdownField("Dia Vencimento", diaVencimento, AlunoOpcoes.diasVencimento) { diaVencimento = it }
            FormField("Telefone", telefone) { telefone = it }
            FormField("Responsável", responsavel) { responsavel = it }
            DropdownField("Turma", turma, AlunoOpcoes.turmas) { turma = it }
            DropdownField("Horário", horario, AlunoOpcoes.horarios) { horario = it }
            DropdownField("Modalidade", modalidade, AlunoOpcoes.modalidades) { modalidade = it }
            FormField("Observações", observacoes) { observacoes = it }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (nome.isBlank()) {
                        Toast.makeText(ctx, "Nome é obrigatório!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val dataNasc = "$dnAno-$dnMes-$dnDia"
                    scope.launch {
                        db.alunoDao().insert(Aluno(
                            nome = nome, sexo = sexo, dataNascimento = dataNasc,
                            faixaAtual = faixa, tipo = tipo,
                            valorMensalidade = valor.toDoubleOrNull() ?: 0.0,
                            diaVencimento = diaVencimento.toIntOrNull() ?: 5,
                            telefone = telefone, responsavel = responsavel,
                            dataMatricula = LocalDate.now().toString(),
                            turma = turma, horario = horario, modalidade = modalidade,
                            observacoes = observacoes, ativo = 1,
                            fotoPath = fotoUri?.toString() ?: ""
                        ))
                        Toast.makeText(ctx, "✅ $nome cadastrado!", Toast.LENGTH_SHORT).show()
                        onSaved()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NukGreen)
            ) { Text("💾 CADASTRAR ALUNO", fontWeight = FontWeight.Bold, fontSize = 16.sp) }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun FormField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(label, fontSize = 12.sp, color = NukLightGray)
        OutlinedTextField(
            value = value, onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NukPurple, unfocusedBorderColor = NukMedGray,
                focusedTextColor = NukWhite, unfocusedTextColor = NukWhite))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, fontSize = 12.sp, color = NukLightGray)
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = value, onValueChange = {}, readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NukPurple, unfocusedBorderColor = NukMedGray,
                    focusedTextColor = NukWhite, unfocusedTextColor = NukWhite))
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { opt ->
                    DropdownMenuItem(text = { Text(opt) },
                        onClick = { onSelect(opt); expanded = false })
                }
            }
        }
    }
}
