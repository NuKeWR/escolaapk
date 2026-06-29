package com.nuklear.escola.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuklear.escola.data.db.AppDatabase
import com.nuklear.escola.data.model.Aluno
import com.nuklear.escola.face.FaceRecognitionEngine
import com.nuklear.escola.face.FaceNetModel
import com.nuklear.escola.ui.components.*
import com.nuklear.escola.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Tela de cadastro de rostos em lote.
 * 
 * Fluxo:
 * 1. Envia UMA foto (pode ter vários rostos)
 * 2. Detecta todos os rostos
 * 3. Mostra cada rosto e você vincula ao aluno
 * 4. SEM precisar reenviar a foto para cada um!
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroLoteScreen(
    onMenuClick: () -> Unit,
    initialFaces: List<Pair<Bitmap, FloatArray>>? = null  // Rostos já detectados (vindo do resultado)
) {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.get(ctx) }
    val scope = rememberCoroutineScope()

    // Rostos detectados: lista de (bitmap, embedding)
    var rostosDetectados by remember { mutableStateOf<List<Pair<Bitmap, FloatArray>>>(initialFaces ?: emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var alunosList by remember { mutableStateOf<List<Aluno>>(emptyList()) }
    var rostosVinculados by remember { mutableStateOf<Set<Int>>(emptySet()) }

    // Dialog vincular
    var indexSelecionado by remember { mutableStateOf(-1) }
    var showVincularDialog by remember { mutableStateOf(false) }
    var expandedAluno by remember { mutableStateOf(false) }
    var alunoEscolhido by remember { mutableStateOf<Aluno?>(null) }
    var buscaAluno by remember { mutableStateOf("") }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()) { }

    // Launcher para foto com múltiplos rostos
    val fotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                isProcessing = true
                // Detectar todos os rostos e gerar embeddings
                val faceNet = FaceNetModel.get(ctx)
                if (!faceNet.isReady) {
                    Toast.makeText(ctx, "❌ Modelo FaceNet não carregado!", Toast.LENGTH_LONG).show()
                    isProcessing = false
                    return@launch
                }

                val bitmap = try {
                    ctx.contentResolver.openInputStream(it)?.use { s ->
                        android.graphics.BitmapFactory.decodeStream(s)
                    }
                } catch (e: Exception) { null }

                if (bitmap == null) {
                    Toast.makeText(ctx, "❌ Erro ao carregar imagem", Toast.LENGTH_SHORT).show()
                    isProcessing = false
                    return@launch
                }

                val input = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
                val opts = com.google.mlkit.vision.face.FaceDetectorOptions.Builder()
                    .setPerformanceMode(com.google.mlkit.vision.face.FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setMinFaceSize(0.08f).build()
                val detector = com.google.mlkit.vision.face.FaceDetection.getClient(opts)

                try {
                    val faces = detector.process(input).await()
                    val pares = mutableListOf<Pair<Bitmap, FloatArray>>()

                    for (face in faces) {
                        val box = face.boundingBox
                        val pad = 20
                        val x = maxOf(0, box.left - pad)
                        val y = maxOf(0, box.top - pad * 2)
                        val w = minOf(bitmap.width - x, box.width() + pad * 2)
                        val h = minOf(bitmap.height - y, box.height() + pad * 3)
                        if (w <= 0 || h <= 0) continue
                        val faceBmp = Bitmap.createBitmap(bitmap, x, y, w, h)
                        val emb = faceNet.getEmbedding(faceBmp) ?: continue
                        pares.add(faceBmp to emb)
                    }

                    rostosDetectados = pares
                    rostosVinculados = emptySet()
                    Toast.makeText(ctx, "✅ ${pares.size} rosto(s) detectados!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(ctx, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                }

                isProcessing = false
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            scope.launch {
                isProcessing = true
                val faceNet = FaceNetModel.get(ctx)
                if (!faceNet.isReady) {
                    isProcessing = false; return@launch
                }
                val input = com.google.mlkit.vision.common.InputImage.fromBitmap(it, 0)
                val opts = com.google.mlkit.vision.face.FaceDetectorOptions.Builder()
                    .setPerformanceMode(com.google.mlkit.vision.face.FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setMinFaceSize(0.08f).build()
                val detector = com.google.mlkit.vision.face.FaceDetection.getClient(opts)

                try {
                    val faces = detector.process(input).await()
                    val pares = mutableListOf<Pair<Bitmap, FloatArray>>()
                    for (face in faces) {
                        val box = face.boundingBox
                        val pad = 20
                        val x = maxOf(0, box.left - pad)
                        val y = maxOf(0, box.top - pad * 2)
                        val w = minOf(it.width - x, box.width() + pad * 2)
                        val h = minOf(it.height - y, box.height() + pad * 3)
                        if (w <= 0 || h <= 0) continue
                        val faceBmp = Bitmap.createBitmap(it, x, y, w, h)
                        val emb = faceNet.getEmbedding(faceBmp) ?: continue
                        pares.add(faceBmp to emb)
                    }
                    rostosDetectados = pares
                    rostosVinculados = emptySet()
                } catch (e: Exception) { }
                isProcessing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        permLauncher.launch(Manifest.permission.CAMERA)
        alunosList = db.alunoDao().getAtivos()
    }

    // Dialog vincular
    if (showVincularDialog && indexSelecionado >= 0) {
        val (faceBmp, faceEmb) = rostosDetectados[indexSelecionado]
        val filtrados = if (buscaAluno.isBlank()) alunosList
        else alunosList.filter { it.nome.contains(buscaAluno, ignoreCase = true) }

        AlertDialog(
            onDismissRequest = { showVincularDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(bitmap = faceBmp.asImageBitmap(), contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text("Vincular rosto", color = NukPurple, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = buscaAluno,
                        onValueChange = { buscaAluno = it },
                        placeholder = { Text("🔍 Buscar aluno...", color = NukLightGray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NukPurple, unfocusedBorderColor = NukMedGray,
                            focusedTextColor = NukWhite, unfocusedTextColor = NukWhite)
                    )

                    filtrados.take(10).forEach { aluno ->
                        Button(
                            onClick = {
                                scope.launch {
                                    FaceRecognitionEngine.cadastrarRosto(ctx, aluno.id, faceEmb)
                                    rostosVinculados = rostosVinculados + indexSelecionado
                                    Toast.makeText(ctx, "✅ ${aluno.nome} vinculado!", Toast.LENGTH_SHORT).show()
                                    showVincularDialog = false
                                    buscaAluno = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = NukDarkGray)
                        ) { Text(aluno.nome, fontSize = 14.sp) }
                    }
                    if (filtrados.size > 10) {
                        Text("+ ${filtrados.size - 10} alunos...", fontSize = 11.sp, color = NukLightGray)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showVincularDialog = false; buscaAluno = "" },
                    colors = ButtonDefaults.buttonColors(containerColor = NukMedGray)) {
                    Text("Cancelar")
                }
            },
            containerColor = NukDarkGray
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NukTopBar("➕ Cadastro em Lote", onMenuClick)

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("➕ CADASTRO EM LOTE", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NukPurple)
            Text("Envie uma foto e vincule cada rosto a um aluno",
                fontSize = 13.sp, color = NukLightGray, textAlign = TextAlign.Center)

            // Botões enviar foto
            if (rostosDetectados.isEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { fotoLauncher.launch("image/*") },
                        Modifier.weight(1f).height(64.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NukPurple),
                        shape = RoundedCornerShape(16.dp)) {
                        Text("🖼 Enviar Foto", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = { cameraLauncher.launch(null) },
                        Modifier.weight(1f).height(64.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NukDarkGray),
                        shape = RoundedCornerShape(16.dp)) {
                        Text("📷 Câmera", fontSize = 16.sp)
                    }
                }
            }

            if (isProcessing) {
                CircularProgressIndicator(color = NukPurple, modifier = Modifier.size(56.dp))
                Text("Detectando rostos...", color = NukYellow)
            }

            // Lista de rostos
            if (rostosDetectados.isNotEmpty()) {
                Text("${rostosDetectados.size} rosto(s) detectados — ${rostosVinculados.size} vinculados",
                    fontSize = 14.sp, color = NukWhite)

                rostosDetectados.forEachIndexed { index, (bmp, _) ->
                    val jaVinculado = index in rostosVinculados

                    NukCard {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Image(bitmap = bmp.asImageBitmap(), contentDescription = null,
                                modifier = Modifier.size(64.dp).clip(CircleShape)
                                    .border(3.dp, if (jaVinculado) NukGreen else NukYellow, CircleShape))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (jaVinculado) "✅ Vinculado!" else "❓ Rosto #${index + 1}",
                                    fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                    color = if (jaVinculado) NukGreen else NukWhite
                                )
                            }
                            if (!jaVinculado) {
                                Button(
                                    onClick = { indexSelecionado = index; showVincularDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = NukPurple)
                                ) { Text("👤 Vincular") }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Botão nova foto
                Button(
                    onClick = { rostosDetectados = emptyList(); rostosVinculados = emptySet() },
                    Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NukMedGray),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("🔄 Nova Foto") }
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}
