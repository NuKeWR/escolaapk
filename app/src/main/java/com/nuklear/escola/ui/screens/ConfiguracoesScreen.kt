package com.nuklear.escola.ui.screens

import android.content.Context
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
import androidx.compose.ui.graphics.Color
import com.nuklear.escola.data.db.AppDatabase
import com.nuklear.escola.data.model.*
import com.nuklear.escola.ui.components.*
import com.nuklear.escola.ui.theme.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun ConfiguracoesScreen(onMenuClick: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.get(ctx) }
    val scope = rememberCoroutineScope()

    var totalAlunos by remember { mutableStateOf(0) }
    var totalAtivos by remember { mutableStateOf(0) }
    var totalPagamentos by remember { mutableStateOf(0) }
    var totalPresencas by remember { mutableStateOf(0) }
    var totalGraduacoes by remember { mutableStateOf(0) }
    var importLog by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }

    fun recarregarStats() {
        scope.launch {
            totalAlunos = db.alunoDao().getAll().size
            totalAtivos = db.alunoDao().countAtivos()
            totalPagamentos = db.pagamentoDao().receitaMensal().size
            totalPresencas = db.presencaDao().ranking().sumOf { it.total as Int }
            totalGraduacoes = 0
        }
    }

    LaunchedEffect(Unit) { recarregarStats() }

    // ─── Launchers para importar CSVs ───
    val importAlunosLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val result = importarAlunosCsv(ctx, db, it)
                importLog = result
                recarregarStats()
            }
        }
    }

    val importPresencasLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val result = importarPresencasCsv(ctx, db, it)
                importLog = result
                recarregarStats()
            }
        }
    }

    val importPagamentosLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val result = importarPagamentosCsv(ctx, db, it)
                importLog = result
                recarregarStats()
            }
        }
    }

    val importGraduacoesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val result = importarGraduacoesCsv(ctx, db, it)
                importLog = result
                recarregarStats()
            }
        }
    }

    // ─── Launcher para importar rostos JSON ───
    val importRostosLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val result = importarRostosJson(ctx, db, it)
                importLog = result
                recarregarStats()
            }
        }
    }

    // ─── Launcher para exportar JSON ───
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                val result = exportarBackupJson(ctx, db, it)
                importLog = result
            }
        }
    }

    // ─── Launcher para importar JSON ───
    val importJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val result = importarBackupJson(ctx, db, it)
                importLog = result
                recarregarStats()
            }
        }
    }

    // Dialog limpar banco
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("⚠️ LIMPAR TUDO?", color = NukPurple) },
            text = { Text("Isso apagará TODOS os dados do app.\nEssa ação é irreversível!", color = NukWhite) },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            db.faceEmbeddingDao().deleteAll()
                            db.graduacaoDao().deleteAll()
                            db.pagamentoDao().deleteAll()
                            db.presencaDao().deleteAll()
                            db.alunoDao().deleteAll()
                        }
                        showClearDialog = false
                        importLog = "🗑 Banco limpo! Todos os dados foram apagados."
                        recarregarStats()
                        Toast.makeText(ctx, "🗑 Banco limpo!", Toast.LENGTH_LONG).show()
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = NukPurple)) {
                    Text("SIM, APAGAR TUDO")
                }
            },
            dismissButton = {
                Button(onClick = { showClearDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NukMedGray)) {
                    Text("CANCELAR")
                }
            },
            containerColor = NukDarkGray
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NukTopBar("⚙️ Configurações", onMenuClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ─── Info do App ───
            NukCard {
                Text("☢ Escola Nuklear v1.0", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NukPurple)
                Text("Gestão completa de academia", fontSize = 12.sp, color = NukLightGray)
                Text("Réplica do CT João Filho IA v3.2", fontSize = 11.sp, color = NukLightGray)
            }

            // ─── Estatísticas ───
            NukCard {
                SectionTitle("ESTATÍSTICAS DO BANCO", "📊")
                Text("Total alunos: $totalAlunos ($totalAtivos ativos)", fontSize = 13.sp, color = NukWhite)
                Text("Pagamentos registrados: $totalPagamentos", fontSize = 13.sp, color = NukWhite)
                Text("Presenças registradas: $totalPresencas", fontSize = 13.sp, color = NukWhite)
            }

            // ─── IMPORTAR ROSTOS DO SISTEMA ORIGINAL ───
            NukCard {
                SectionTitle("IMPORTAR ROSTOS (DO PC)", "🧠")
                Text(
                    "Rode exportar_rostos_para_apk.py no PC\n" +
                    "para gerar rostos_para_apk.json,\n" +
                    "depois importe aqui.",
                    fontSize = 11.sp, color = NukLightGray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Button(
                    onClick = { importRostosLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0)),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) { Text("🧠 Importar rostos_para_apk.json", fontWeight = FontWeight.Bold) }
            }

            // ─── IMPORTAR CSVs DO SISTEMA ORIGINAL ───
            NukCard {
                SectionTitle("IMPORTAR DO SISTEMA ORIGINAL", "📥")
                Text(
                    "Importe os CSVs gerados pelo exportar_para_apk.py\n" +
                    "Formato: separador ; (ponto e vírgula)\n" +
                    "Arquivos: alunos.csv, presencas.csv, pagamentos.csv, graduacoes.csv",
                    fontSize = 11.sp, color = NukLightGray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Button(
                    onClick = { importAlunosLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NukPurple)
                ) { Text("👥 Importar alunos.csv") }

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick = { importPresencasLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NukPurple)
                ) { Text("✅ Importar presencas.csv") }

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick = { importPagamentosLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NukPurple)
                ) { Text("💳 Importar pagamentos.csv") }

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick = { importGraduacoesLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NukPurple)
                ) { Text("🥋 Importar graduacoes.csv") }
            }

            // ─── BACKUP JSON ───
            NukCard {
                SectionTitle("BACKUP COMPLETO (JSON)", "💾")

                Button(
                    onClick = {
                        val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                        exportLauncher.launch("escola_nuklear_backup_$ts.json")
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NukBlue)
                ) { Text("📤 Exportar Backup JSON") }

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick = { importJsonLauncher.launch("application/json") },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NukMedGray)
                ) { Text("📥 Importar Backup JSON") }
            }

            // ─── LIMPAR BANCO ───
            NukCard {
                SectionTitle("ZONA DE PERIGO", "⚠️")
                Button(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NukRedDark)
                ) { Text("🗑 LIMPAR TODO O BANCO DE DADOS") }
            }

            // ─── Log de importação ───
            if (importLog.isNotBlank()) {
                NukCard {
                    SectionTitle("LOG", "📋")
                    Text(importLog, fontSize = 12.sp, color = NukGreen)
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════
// Funções de Importação CSV (formato do exportar_para_apk.py)
// Separador: ; (ponto e vírgula)
// ═══════════════════════════════════════════════════════

private fun lerCsv(ctx: Context, uri: Uri): List<List<String>> {
    val linhas = mutableListOf<List<String>>()
    ctx.contentResolver.openInputStream(uri)?.use { stream ->
        BufferedReader(InputStreamReader(stream, "UTF-8")).use { reader ->
            reader.forEachLine { line ->
                if (line.isNotBlank()) {
                    linhas.add(line.split(";").map { it.trim() })
                }
            }
        }
    }
    return linhas
}

private suspend fun importarAlunosCsv(ctx: Context, db: AppDatabase, uri: Uri): String {
    return withContext(Dispatchers.IO) {
        try {
            val linhas = lerCsv(ctx, uri)
            if (linhas.size < 2) return@withContext "❌ CSV vazio ou inválido"

            val header = linhas[0].map { it.lowercase().replace("\uFEFF", "") }
            var importados = 0

            for (i in 1 until linhas.size) {
                val cols = linhas[i]
                if (cols.size < header.size) continue

                val map = header.zip(cols).toMap()
                val aluno = Aluno(
                    nome = map["nome"] ?: continue,
                    sexo = map["sexo"] ?: "",
                    dataNascimento = map["data_nascimento"] ?: "",
                    faixaAtual = map["faixa_atual"] ?: "",
                    tipo = map["tipo"] ?: "",
                    valorMensalidade = map["valor_mensalidade"]?.toDoubleOrNull() ?: 0.0,
                    diaVencimento = map["dia_vencimento"]?.toIntOrNull() ?: 5,
                    telefone = map["telefone"] ?: "",
                    responsavel = map["responsavel"] ?: "",
                    dataMatricula = map["data_matricula"] ?: "",
                    turma = map["turma"] ?: "",
                    horario = map["horario"] ?: "",
                    modalidade = map["modalidade"] ?: "",
                    observacoes = map["observacoes"] ?: "",
                    ativo = map["ativo"]?.toIntOrNull() ?: 1
                )
                db.alunoDao().insert(aluno)
                importados++
            }
            "✅ $importados aluno(s) importados!"
        } catch (e: Exception) {
            "❌ Erro: ${e.message}"
        }
    }
}

private suspend fun importarPresencasCsv(ctx: Context, db: AppDatabase, uri: Uri): String {
    return withContext(Dispatchers.IO) {
        try {
            val linhas = lerCsv(ctx, uri)
            if (linhas.size < 2) return@withContext "❌ CSV vazio"

            val header = linhas[0].map { it.lowercase().replace("\uFEFF", "") }
            var importados = 0

            for (i in 1 until linhas.size) {
                val cols = linhas[i]
                if (cols.size < header.size) continue

                val map = header.zip(cols).toMap()
                db.presencaDao().insert(
                    Presenca(
                        data = map["data"] ?: continue,
                        aluno = map["aluno"] ?: continue
                    )
                )
                importados++
            }
            "✅ $importados presença(s) importadas!"
        } catch (e: Exception) {
            "❌ Erro: ${e.message}"
        }
    }
}

private suspend fun importarPagamentosCsv(ctx: Context, db: AppDatabase, uri: Uri): String {
    return withContext(Dispatchers.IO) {
        try {
            val linhas = lerCsv(ctx, uri)
            if (linhas.size < 2) return@withContext "❌ CSV vazio"

            val header = linhas[0].map { it.lowercase().replace("\uFEFF", "") }
            var importados = 0

            for (i in 1 until linhas.size) {
                val cols = linhas[i]
                if (cols.size < header.size) continue

                val map = header.zip(cols).toMap()
                db.pagamentoDao().insert(
                    Pagamento(
                        alunoId = map["aluno_id"]?.toLongOrNull() ?: continue,
                        dataPagamento = map["data_pagamento"] ?: "",
                        competencia = map["competencia"] ?: "",
                        valorPago = map["valor_pago"]?.toDoubleOrNull() ?: 0.0,
                        formaPagamento = map["forma_pagamento"] ?: "",
                        observacao = map["observacao"] ?: ""
                    )
                )
                importados++
            }
            "✅ $importados pagamento(s) importados!"
        } catch (e: Exception) {
            "❌ Erro: ${e.message}"
        }
    }
}

private suspend fun importarGraduacoesCsv(ctx: Context, db: AppDatabase, uri: Uri): String {
    return withContext(Dispatchers.IO) {
        try {
            val linhas = lerCsv(ctx, uri)
            if (linhas.size < 2) return@withContext "❌ CSV vazio"

            val header = linhas[0].map { it.lowercase().replace("\uFEFF", "") }
            var importados = 0

            for (i in 1 until linhas.size) {
                val cols = linhas[i]
                if (cols.size < header.size) continue

                val map = header.zip(cols).toMap()
                db.graduacaoDao().insert(
                    Graduacao(
                        alunoId = map["aluno_id"]?.toLongOrNull() ?: continue,
                        dataGraduacao = map["data_graduacao"] ?: "",
                        faixa = map["faixa"] ?: "",
                        observacao = map["observacao"] ?: ""
                    )
                )
                importados++
            }
            "✅ $importados graduação(ões) importadas!"
        } catch (e: Exception) {
            "❌ Erro: ${e.message}"
        }
    }
}

// ═══════════════════════════════════════════════════════
// Backup JSON
// ═══════════════════════════════════════════════════════

// ═══════════════════════════════════════════════════════
// Importar rostos do PC (rostos_para_apk.json)
// ═══════════════════════════════════════════════════════

private data class RostosImportData(
    val version: String = "",
    val embedding_dim: Int = 128,
    val alunos: List<RostoAluno> = emptyList()
)

private data class RostoAluno(
    val nome: String,
    val grupo: String = "",
    val embedding: List<Float>,
    val num_fotos: Int = 1
)

private suspend fun importarRostosJson(ctx: Context, db: AppDatabase, uri: Uri): String {
    return withContext(Dispatchers.IO) {
        try {
            val json = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                ?: return@withContext "❌ Erro lendo arquivo"

            val data = Gson().fromJson(json, RostosImportData::class.java)

            if (data.alunos.isEmpty()) {
                return@withContext "❌ Nenhum aluno no arquivo"
            }

            var importados = 0
            var naoEncontrados = 0
            val nomes = mutableListOf<String>()

            for (rostoAluno in data.alunos) {
                // Encontrar o aluno no banco pelo nome
                val alunosBd = db.alunoDao().buscar(rostoAluno.nome)
                val aluno = alunosBd.firstOrNull()

                if (aluno == null) {
                    naoEncontrados++
                    nomes.add("❌ ${rostoAluno.nome}")
                    continue
                }

                // Converter embedding para JSON string
                val embJson = Gson().toJson(rostoAluno.embedding)

                // Salvar embedding
                db.faceEmbeddingDao().insert(
                    FaceEmbedding(
                        alunoId = aluno.id,
                        embedding = embJson,
                        createdAt = java.time.LocalDateTime.now().toString()
                    )
                )
                importados++
                nomes.add("✅ ${rostoAluno.nome}")
            }

            val log = buildString {
                appendLine("🧠 Importação de rostos concluída!")
                appendLine("✅ $importados rosto(s) importados")
                if (naoEncontrados > 0) {
                    appendLine("❌ $naoEncontrados não encontrados no banco")
                    appendLine("   (importe os alunos CSV primeiro!)")
                }
                appendLine()
                nomes.forEach { appendLine("  $it") }
            }
            log
        } catch (e: Exception) {
            "❌ Erro: ${e.message}"
        }
    }
}

private data class BackupData(
    val alunos: List<Aluno>,
    val pagamentos: List<Pagamento>,
    val graduacoes: List<Graduacao>,
    val presencas: List<Presenca>,
    val exportDate: String
)

private suspend fun exportarBackupJson(ctx: Context, db: AppDatabase, uri: Uri): String {
    return withContext(Dispatchers.IO) {
        try {
            val data = BackupData(
                alunos = db.alunoDao().getAll(),
                pagamentos = db.alunoDao().getAll().flatMap { db.pagamentoDao().getByAluno(it.id) },
                graduacoes = db.alunoDao().getAll().flatMap { db.graduacaoDao().getByAluno(it.id) },
                presencas = db.presencaDao().getAll(),
                exportDate = LocalDateTime.now().toString()
            )
            ctx.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(Gson().toJson(data).toByteArray(Charsets.UTF_8))
            }
            "✅ Backup exportado!"
        } catch (e: Exception) {
            "❌ Erro: ${e.message}"
        }
    }
}

private suspend fun importarBackupJson(ctx: Context, db: AppDatabase, uri: Uri): String {
    return withContext(Dispatchers.IO) {
        try {
            val json = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return@withContext "❌ Erro lendo arquivo"
            val data = Gson().fromJson(json, BackupData::class.java)

            data.alunos.forEach { db.alunoDao().insert(it.copy(id = 0)) }
            data.pagamentos.forEach { db.pagamentoDao().insert(it.copy(id = 0)) }
            data.graduacoes.forEach { db.graduacaoDao().insert(it.copy(id = 0)) }
            data.presencas.forEach { db.presencaDao().insert(it.copy(id = 0)) }

            "✅ Importados: ${data.alunos.size} alunos, ${data.pagamentos.size} pagamentos, ${data.graduacoes.size} graduações, ${data.presencas.size} presenças"
        } catch (e: Exception) {
            "❌ Erro: ${e.message}"
        }
    }
}
