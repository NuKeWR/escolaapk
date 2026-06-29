package com.nuklear.escola.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.media.ExifInterface
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuklear.escola.data.db.AppDatabase
import com.nuklear.escola.data.model.Aluno
import com.nuklear.escola.data.model.Presenca
import com.nuklear.escola.face.FaceRecognitionEngine
import com.nuklear.escola.face.FaceRecognitionEngine.IdentifiedPerson
import com.nuklear.escola.face.FaceRecognitionEngine.TrainingPhotoResult
import com.nuklear.escola.ui.components.*
import com.nuklear.escola.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReconhecimentoScreen(
    onMenuClick: () -> Unit,
    onAlunoClick: (Long) -> Unit = {},
    onCadastrarAluno: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.get(ctx) }
    val scope = rememberCoroutineScope()

    var modo by remember { mutableStateOf("home") }
    var isProcessing by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }
    var resultado by remember { mutableStateOf<TrainingPhotoResult?>(null) }

    // Data da presença em partes
    val hojeDate = LocalDate.now()
    var presDia by remember { mutableStateOf("%02d".format(hojeDate.dayOfMonth)) }
    var presMes by remember { mutableStateOf("%02d".format(hojeDate.monthValue)) }
    var presAno by remember { mutableStateOf(hojeDate.year.toString()) }
    fun dataPresenca() = "$presAno-$presMes-$presDia"
    var useExifDate by remember { mutableStateOf(true) }
    var dataExif by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Confirmação de presenças (checkboxes)
    val presencaChecks = remember { mutableStateMapOf<Long, Boolean>() }

    // Ação para desconhecidos
    var desconhecidoSelecionado by remember { mutableStateOf<Int?>(null) }
    var showDesconhecidoDialog by remember { mutableStateOf(false) }

    // Cadastro
    var alunosList by remember { mutableStateOf<List<Aluno>>(emptyList()) }
    var alunoSelecionado by remember { mutableStateOf<Aluno?>(null) }
    var expandedAluno by remember { mutableStateOf(false) }
    var faceBitmapCadastro by remember { mutableStateOf<Bitmap?>(null) }
    var embeddingCadastro by remember { mutableStateOf<FloatArray?>(null) }

    var alunosComFace by remember { mutableStateOf(0) }
    var totalAtivos by remember { mutableStateOf(0) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()) { }

    // ── Extrair data EXIF de uma URI ──
    fun extrairDataExif(uri: android.net.Uri): String? {
        return try {
            ctx.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val dateStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                if (dateStr != null && dateStr.length >= 10) {
                    // Formato EXIF: "2025:06:27 19:30:00" → "2025-06-27"
                    dateStr.substring(0, 10).replace(':', '-')
                } else null
            }
        } catch (e: Exception) { null }
    }

    // ── Launcher FOTO DO TREINO (galeria) ──
    val treinoGaleriaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // Tentar extrair data EXIF
            val exifDate = extrairDataExif(it)
            dataExif = exifDate
            if (exifDate != null && useExifDate && exifDate.length >= 10) {
                val parts = exifDate.split("-")
                if (parts.size == 3) { presAno = parts[0]; presMes = parts[1]; presDia = parts[2] }
            }

            scope.launch {
                isProcessing = true
                progressText = "📸 Detectando rostos..."
                val res = FaceRecognitionEngine.processarFotoTreino(
                    ctx, it, dataPresenca(), marcarPresencaAuto = false
                )
                resultado = res
                // Pre-check todos os identificados
                presencaChecks.clear()
                res.identificados.forEach { p -> presencaChecks[p.alunoId] = true }
                isProcessing = false
                if (res.error == null) modo = "resultado"
                else progressText = res.error ?: ""
            }
        }
    }

    val treinoCameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            val h = LocalDate.now()
            presDia = "%02d".format(h.dayOfMonth); presMes = "%02d".format(h.monthValue); presAno = h.year.toString()
            dataExif = null
            scope.launch {
                isProcessing = true
                progressText = "📸 Detectando rostos..."
                val res = FaceRecognitionEngine.processarFotoBitmap(
                    ctx, it, dataPresenca(), marcarPresencaAuto = false
                )
                resultado = res
                presencaChecks.clear()
                res.identificados.forEach { p -> presencaChecks[p.alunoId] = true }
                isProcessing = false
                if (res.error == null) modo = "resultado"
                else progressText = res.error ?: ""
            }
        }
    }

    val cadastroCameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            scope.launch {
                isProcessing = true
                val (face, emb) = FaceRecognitionEngine.detectarRostoUnicoBitmap(ctx, it)
                faceBitmapCadastro = face; embeddingCadastro = emb
                isProcessing = false
            }
        }
    }

    val cadastroGaleriaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                isProcessing = true
                val (face, emb) = FaceRecognitionEngine.detectarRostoUnico(ctx, it)
                faceBitmapCadastro = face; embeddingCadastro = emb
                isProcessing = false
            }
        }
    }

    fun recarregarStats() {
        scope.launch {
            val todos = db.alunoDao().getAtivos()
            totalAtivos = todos.size; alunosList = todos
            var com = 0
            for (a in todos) { if (db.faceEmbeddingDao().countByAluno(a.id) > 0) com++ }
            alunosComFace = com
        }
    }

    LaunchedEffect(Unit) { permLauncher.launch(Manifest.permission.CAMERA); recarregarStats() }
    LaunchedEffect(modo) { if (modo == "home" || modo == "cadastrar") recarregarStats() }

    Column(modifier = Modifier.fillMaxSize()) {
        NukTopBar("📸 Reconhecimento", onMenuClick)

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (modo) {
                // ═══════ HOME ═══════
                "home" -> {
                    Text("☢", fontSize = 48.sp)
                    Text("FOTO DO TREINO", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        color = NukPurple, textAlign = TextAlign.Center)
                    Text("Envie a foto → identifica todos → você confirma",
                        fontSize = 13.sp, color = NukLightGray, textAlign = TextAlign.Center)

                    NukCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$alunosComFace", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = NukGreen)
                                Text("Cadastrados", fontSize = 11.sp, color = NukLightGray)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val sem = totalAtivos - alunosComFace
                                Text("$sem", fontSize = 28.sp, fontWeight = FontWeight.Bold,
                                    color = if (sem > 0) NukYellow else NukGreen)
                                Text("Sem foto", fontSize = 11.sp, color = NukLightGray)
                            }
                        }
                    }

                    // Data da presença com dropdowns
                    NukCard {
                        Text("📅 Data da presença:", fontSize = 13.sp, color = NukLightGray)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.weight(0.8f)) { DropdownField("Dia", presDia, AlunoOpcoes.diasDoMes()) { presDia = it } }
                            Box(Modifier.weight(1.2f)) { DropdownField("Mês", presMes, AlunoOpcoes.meses.map { it.first }) { presMes = it } }
                            Box(Modifier.weight(1f)) { DropdownField("Ano", presAno, listOf(hojeDate.year.toString(), (hojeDate.year - 1).toString())) { presAno = it } }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = useExifDate, onCheckedChange = { useExifDate = it },
                                colors = CheckboxDefaults.colors(checkedColor = NukPurple))
                            Text("Usar data da foto (EXIF) se disponível", fontSize = 12.sp, color = NukLightGray)
                        }
                    }

                    Button(
                        onClick = { treinoGaleriaLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(72.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NukPurple),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("🖼 ENVIAR FOTO DO TREINO", fontSize = 18.sp, fontWeight = FontWeight.Bold) }

                    Button(
                        onClick = { treinoCameraLauncher.launch(null) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NukDarkGray),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("📷 TIRAR FOTO AGORA", fontSize = 16.sp) }

                    HorizontalDivider(color = NukMedGray, modifier = Modifier.padding(vertical = 8.dp))

                    Button(
                        onClick = { modo = "cadastrar" },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NukMedGray),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("➕ CADASTRAR ROSTOS DE ALUNOS") }

                    if (isProcessing) {
                        CircularProgressIndicator(color = NukPurple, modifier = Modifier.size(56.dp))
                        Text(progressText, fontSize = 14.sp, color = NukYellow, textAlign = TextAlign.Center)
                    }
                    if (!isProcessing && progressText.isNotBlank()) {
                        Text(progressText, fontSize = 14.sp, color = NukRedLight, textAlign = TextAlign.Center)
                    }
                }

                // ═══════ RESULTADO ═══════
                "resultado" -> {
                    val res = resultado ?: return@Column

                    Button(onClick = { modo = "home"; resultado = null; progressText = "" },
                        colors = ButtonDefaults.buttonColors(containerColor = NukMedGray)) { Text("← Nova Foto") }

                    Text("📊 RESULTADO", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NukPurple)

                    // Data info
                    NukCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("📅 Presença para:", fontSize = 13.sp, color = NukLightGray)
                            Text(dataPresenca(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NukWhite)
                        }
                        if (dataExif != null) {
                            Text("📷 Data EXIF: $dataExif", fontSize = 11.sp, color = NukLightGray)
                        }
                    }

                    // Stats
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard("Rostos", "${res.totalRostosDetectados}", NukMedGray, Modifier.weight(1f))
                        StatCard("Identif.", "${res.identificados.size}", NukGreen, Modifier.weight(1f))
                        StatCard("Desconh.", "${res.desconhecidos.size}", NukYellow, Modifier.weight(1f))
                    }

                    // ── INADIMPLENTES ──
                    if (res.inadimplentes.isNotEmpty()) {
                        NukCard(modifier = Modifier.border(2.dp, NukRed, RoundedCornerShape(12.dp))) {
                            SectionTitle("INADIMPLENTES NO TREINO", "🚨")
                            res.inadimplentes.forEach { inad ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Image(bitmap = inad.faceBitmap.asImageBitmap(), contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(CircleShape))
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(inad.alunoNome, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NukWhite)
                                        Text("${inad.statusFinanceiro}", fontSize = 11.sp, color = NukRedLight)
                                    }
                                    Button(onClick = { onAlunoClick(inad.alunoId) },
                                        colors = ButtonDefaults.buttonColors(containerColor = NukPurple),
                                        contentPadding = PaddingValues(8.dp)) { Text("💳") }
                                }
                            }
                        }
                    }

                    // ── ANIVERSARIANTES ──
                    if (res.aniversariantes.isNotEmpty()) {
                        NukCard(modifier = Modifier.border(2.dp, Color(0xFF9C27B0), RoundedCornerShape(12.dp))) {
                            SectionTitle("ANIVERSARIANTES", "🎂")
                            res.aniversariantes.forEach { a ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                    Image(bitmap = a.faceBitmap.asImageBitmap(), contentDescription = null,
                                        modifier = Modifier.size(36.dp).clip(CircleShape))
                                    Spacer(Modifier.width(8.dp))
                                    Text("🎂 ${a.alunoNome}", fontSize = 14.sp, color = NukWhite)
                                }
                            }
                        }
                    }

                    // ── IDENTIFICADOS (com checkbox) ──
                    NukCard {
                        SectionTitle("CONFIRMAR PRESENÇAS", "✅")
                        Text("Desmarque quem NÃO estava no treino:", fontSize = 11.sp, color = NukLightGray)
                        Spacer(Modifier.height(4.dp))

                        res.identificados.forEach { ident ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = presencaChecks[ident.alunoId] ?: true,
                                    onCheckedChange = { presencaChecks[ident.alunoId] = it },
                                    colors = CheckboxDefaults.colors(checkedColor = NukGreen))
                                Image(bitmap = ident.faceBitmap.asImageBitmap(), contentDescription = null,
                                    modifier = Modifier.size(32.dp).clip(CircleShape))
                                Spacer(Modifier.width(6.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(ident.alunoNome, fontSize = 13.sp, color = NukWhite)
                                    Text("${(ident.confidence * 100).toInt()}%", fontSize = 10.sp, color = NukLightGray)
                                }
                                if (ident.isAniversariante) Text("🎂", fontSize = 14.sp)
                                if (ident.isInadimplente) Text("🔴", fontSize = 14.sp)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        val jaPresentes = db.presencaDao().getPresentes(dataPresenca()).toSet()
                                        var count = 0
                                        for (ident in res.identificados) {
                                            if (presencaChecks[ident.alunoId] == true && ident.alunoNome !in jaPresentes) {
                                                db.presencaDao().insert(Presenca(data = dataPresenca(), aluno = ident.alunoNome))
                                                count++
                                            }
                                        }
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(ctx, "✅ $count presença(s) marcadas para ${dataPresenca()}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NukGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("💾 CONFIRMAR ${presencaChecks.count { it.value }} PRESENÇA(S)", fontWeight = FontWeight.Bold) }
                    }

                    // ── DESCONHECIDOS (com ações) ──
                    if (res.desconhecidos.isNotEmpty()) {
                        NukCard {
                            SectionTitle("ROSTOS NÃO RECONHECIDOS", "❓")
                            Text("Toque em um rosto para escolher ação:", fontSize = 11.sp, color = NukLightGray)
                            Spacer(Modifier.height(6.dp))

                            res.desconhecidos.forEachIndexed { index, bmp ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    .clickable { desconhecidoSelecionado = index; showDesconhecidoDialog = true },
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Image(bitmap = bmp.asImageBitmap(), contentDescription = null,
                                        modifier = Modifier.size(56.dp).clip(CircleShape)
                                            .border(2.dp, NukYellow, CircleShape))
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("Desconhecido #${index + 1}", fontSize = 14.sp, color = NukWhite)
                                        Text("Toque para: cadastrar, visitante ou ignorar",
                                            fontSize = 11.sp, color = NukLightGray)
                                    }
                                }
                            }
                        }
                    }
                }

                // ═══════ CADASTRAR ROSTO ═══════
                "cadastrar" -> {
                    Button(onClick = { modo = "home"; alunoSelecionado = null },
                        colors = ButtonDefaults.buttonColors(containerColor = NukMedGray)) { Text("← Voltar") }

                    Text("➕ CADASTRAR ROSTO", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NukPurple)

                    NukCard {
                        Text("1️⃣ Selecione o aluno", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NukWhite)
                        ExposedDropdownMenuBox(expanded = expandedAluno, onExpandedChange = { expandedAluno = it }) {
                            OutlinedTextField(
                                value = alunoSelecionado?.nome ?: "Selecionar...",
                                onValueChange = {}, readOnly = true,
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NukPurple, unfocusedBorderColor = NukMedGray,
                                    focusedTextColor = NukWhite, unfocusedTextColor = NukWhite))
                            ExposedDropdownMenu(expanded = expandedAluno, onDismissRequest = { expandedAluno = false }) {
                                alunosList.forEach { a ->
                                    DropdownMenuItem(text = { Text(a.nome) },
                                        onClick = { alunoSelecionado = a; expandedAluno = false })
                                }
                            }
                        }
                    }

                    if (alunoSelecionado != null) {
                        NukCard {
                            Text("2️⃣ Foto do rosto", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NukWhite)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { cadastroCameraLauncher.launch(null) },
                                    Modifier.weight(1f).height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NukPurple),
                                    shape = RoundedCornerShape(12.dp)) { Text("📷 Câmera") }
                                Button(onClick = { cadastroGaleriaLauncher.launch("image/*") },
                                    Modifier.weight(1f).height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NukDarkGray),
                                    shape = RoundedCornerShape(12.dp)) { Text("🖼 Galeria") }
                            }
                        }

                        if (isProcessing) CircularProgressIndicator(color = NukPurple, modifier = Modifier.size(48.dp))

                        if (faceBitmapCadastro != null && embeddingCadastro != null) {
                            Image(bitmap = faceBitmapCadastro!!.asImageBitmap(), contentDescription = null,
                                modifier = Modifier.size(120.dp).clip(CircleShape).border(3.dp, NukGreen, CircleShape))
                            Text("✅ Rosto detectado!", color = NukGreen)
                            Button(
                                onClick = {
                                    scope.launch {
                                        FaceRecognitionEngine.cadastrarRosto(ctx, alunoSelecionado!!.id, embeddingCadastro!!)
                                        Toast.makeText(ctx, "✅ ${alunoSelecionado!!.nome} cadastrado!", Toast.LENGTH_LONG).show()
                                        alunoSelecionado = null; faceBitmapCadastro = null; embeddingCadastro = null; modo = "home"
                                    }
                                },
                                Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NukGreen),
                                shape = RoundedCornerShape(16.dp)
                            ) { Text("💾 SALVAR ROSTO", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }

    // ═══════ DIALOG PARA DESCONHECIDO ═══════
    if (showDesconhecidoDialog && desconhecidoSelecionado != null) {
        val idx = desconhecidoSelecionado!!
        val bmp = resultado?.desconhecidos?.getOrNull(idx)

        AlertDialog(
            onDismissRequest = { showDesconhecidoDialog = false },
            title = { Text("❓ Desconhecido #${idx + 1}", color = NukPurple) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (bmp != null) {
                        Image(bitmap = bmp.asImageBitmap(), contentDescription = null,
                            modifier = Modifier.size(80.dp).clip(CircleShape).align(Alignment.CenterHorizontally))
                    }
                    Text("O que deseja fazer com este rosto?", color = NukWhite, fontSize = 14.sp)
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Vincular a aluno existente
                    Button(
                        onClick = {
                            showDesconhecidoDialog = false
                            // Salvar bitmap e ir para cadastrar
                            if (bmp != null) {
                                scope.launch {
                                    val (_, emb) = FaceRecognitionEngine.detectarRostoUnicoBitmap(ctx, bmp)
                                    faceBitmapCadastro = bmp; embeddingCadastro = emb
                                    modo = "cadastrar"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NukBlue)
                    ) { Text("👤 Vincular a aluno existente") }

                    // Cadastrar como novo aluno
                    Button(
                        onClick = { showDesconhecidoDialog = false; onCadastrarAluno() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NukGreen)
                    ) { Text("➕ Cadastrar como novo aluno") }

                    // Visitante
                    Button(
                        onClick = {
                            showDesconhecidoDialog = false
                            Toast.makeText(ctx, "👋 Registrado como visitante", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NukMedGray)
                    ) { Text("👋 Registrar como visitante") }

                    // Ignorar
                    Button(
                        onClick = { showDesconhecidoDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NukRedDark)
                    ) { Text("❌ Ignorar") }
                }
            },
            containerColor = NukDarkGray
        )
    }
}
