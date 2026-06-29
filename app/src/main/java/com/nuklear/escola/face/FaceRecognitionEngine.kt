package com.nuklear.escola.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.nuklear.escola.data.db.AppDatabase
import com.nuklear.escola.data.model.FaceEmbedding
import com.nuklear.escola.data.model.Presenca
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/**
 * Motor de reconhecimento facial.
 * 
 * Pipeline:
 * 1) ML Kit → detecta rostos (bounding boxes)
 * 2) FaceNet TFLite → gera embedding 128-d de cada rosto recortado
 * 3) Similaridade coseno → compara com banco de embeddings cadastrados
 */
object FaceRecognitionEngine {

    private const val TAG = "FaceEngine"
    private const val COSINE_THRESHOLD = 0.55f  // Acima deste valor = match

    private val detector by lazy {
        val opts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setMinFaceSize(0.08f)
            .build()
        FaceDetection.getClient(opts)
    }

    data class IdentifiedPerson(
        val faceBitmap: Bitmap,
        val matched: Boolean,
        val alunoId: Long = 0,
        val alunoNome: String = "",
        val confidence: Float = 0f,
        val isInadimplente: Boolean = false,
        val isAniversariante: Boolean = false,
        val statusFinanceiro: String = "",
        val mesesAtraso: Int = 0
    )

    data class TrainingPhotoResult(
        val totalRostosDetectados: Int = 0,
        val identificados: List<IdentifiedPerson> = emptyList(),
        val desconhecidos: List<Bitmap> = emptyList(),
        val presencasMarcadas: Int = 0,
        val inadimplentes: List<IdentifiedPerson> = emptyList(),
        val aniversariantes: List<IdentifiedPerson> = emptyList(),
        val error: String? = null
    )

    // ═══════════════════════════════════════════════════════
    // PRINCIPAL
    // ═══════════════════════════════════════════════════════

    suspend fun processarFotoTreino(
        context: Context, imageUri: Uri, dataPresenca: String,
        marcarPresencaAuto: Boolean = true
    ): TrainingPhotoResult {
        return withContext(Dispatchers.Default) {
            try {
                val bitmap = loadBitmap(context, imageUri)
                    ?: return@withContext TrainingPhotoResult(error = "Erro ao carregar imagem")
                processarBitmap(context, bitmap, dataPresenca, marcarPresencaAuto)
            } catch (e: Exception) {
                Log.e(TAG, "Erro", e)
                TrainingPhotoResult(error = "Erro: ${e.message}")
            }
        }
    }

    suspend fun processarFotoBitmap(
        context: Context, bitmap: Bitmap, dataPresenca: String,
        marcarPresencaAuto: Boolean = true
    ): TrainingPhotoResult {
        return withContext(Dispatchers.Default) {
            try {
                processarBitmap(context, bitmap, dataPresenca, marcarPresencaAuto)
            } catch (e: Exception) {
                Log.e(TAG, "Erro", e)
                TrainingPhotoResult(error = "Erro: ${e.message}")
            }
        }
    }

    private suspend fun processarBitmap(
        context: Context, bitmap: Bitmap, dataPresenca: String, marcarPresencaAuto: Boolean
    ): TrainingPhotoResult {
        // 1. Detectar todos os rostos com ML Kit
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val mlFaces: List<Face> = detector.process(inputImage).await()

        if (mlFaces.isEmpty()) {
            return TrainingPhotoResult(error = "Nenhum rosto detectado na foto")
        }

        // 2. Para cada rosto: recortar + gerar embedding com FaceNet
        val faceNet = FaceNetModel.get(context)
        
        if (!faceNet.isReady) {
            return TrainingPhotoResult(
                error = "❌ Modelo FaceNet não carregado!\nColoque facenet.tflite em assets/"
            )
        }

        data class DetectedItem(val bmp: Bitmap, val emb: FloatArray)
        val detected = mutableListOf<DetectedItem>()

        for (face: Face in mlFaces) {
            val box: Rect = face.boundingBox
            val faceBmp = cropFaceSafe(bitmap, box) ?: continue
            val embedding = faceNet.getEmbedding(faceBmp) ?: continue
            detected.add(DetectedItem(faceBmp, embedding))
        }

        // 3. Carregar embeddings do banco
        val db = AppDatabase.get(context)
        val allEmbeddings: List<FaceEmbedding> = withContext(Dispatchers.IO) {
            db.faceEmbeddingDao().getAll()
        }

        val alunosMap = withContext(Dispatchers.IO) {
            db.alunoDao().getAtivos().associateBy { it.id }
        }

        val hoje = java.time.LocalDate.now()
        val comp = "${hoje.year}-${"%02d".format(hoje.monthValue)}"
        val mesAtual = "%02d".format(hoje.monthValue)

        val identificados = mutableListOf<IdentifiedPerson>()
        val desconhecidos = mutableListOf<Bitmap>()
        val alunosJaIdentificados = mutableSetOf<Long>()

        // 4. Para cada rosto, encontrar melhor match via SIMILARIDADE COSENO
        for (det in detected) {
            var bestId = 0L
            var bestSimilarity = -1f

            for (fe: FaceEmbedding in allEmbeddings) {
                if (fe.alunoId in alunosJaIdentificados) continue
                val stored = deserializeEmbedding(fe.embedding) ?: continue
                val similarity = cosineSimilarity(det.emb, stored)
                if (similarity > bestSimilarity) {
                    bestSimilarity = similarity
                    bestId = fe.alunoId
                }
            }

            if (bestSimilarity >= COSINE_THRESHOLD && bestId > 0) {
                val aluno = alunosMap[bestId]
                if (aluno != null) {
                    alunosJaIdentificados.add(bestId)

                    val ultimaComp: String? = withContext(Dispatchers.IO) {
                        db.pagamentoDao().ultimaCompetencia(bestId)
                    }

                    val statusFin: String
                    val inadimplente: Boolean
                    val mesesAtraso: Int

                    when {
                        aluno.tipo.lowercase() == "bolsista" -> {
                            statusFin = "🟡 Bolsista"; inadimplente = false; mesesAtraso = 0
                        }
                        aluno.tipo.lowercase() == "professor" -> {
                            statusFin = "🔵 Isento"; inadimplente = false; mesesAtraso = 0
                        }
                        ultimaComp != null && ultimaComp >= comp -> {
                            statusFin = "🟢 Em dia"; inadimplente = false; mesesAtraso = 0
                        }
                        ultimaComp == null -> {
                            statusFin = "🔴 Sem pagamento"; inadimplente = true; mesesAtraso = 0
                        }
                        else -> {
                            val parts = comp.split("-")
                            val uparts = ultimaComp.split("-")
                            val ay = parts[0].toInt(); val am = parts[1].toInt()
                            val uy = uparts[0].toInt(); val um = uparts[1].toInt()
                            val m = (ay - uy) * 12 + (am - um)
                            statusFin = "🔴 Atrasado ($m mês)"; inadimplente = true; mesesAtraso = m
                        }
                    }

                    val isAniver = aluno.dataNascimento.length >= 7 &&
                            aluno.dataNascimento.substring(5, 7) == mesAtual

                    identificados.add(IdentifiedPerson(
                        faceBitmap = det.bmp, matched = true,
                        alunoId = bestId, alunoNome = aluno.nome,
                        confidence = bestSimilarity, isInadimplente = inadimplente,
                        isAniversariante = isAniver, statusFinanceiro = statusFin,
                        mesesAtraso = mesesAtraso
                    ))
                }
            } else {
                desconhecidos.add(det.bmp)
            }
        }

        // 5. Marcar presença
        var presencasMarcadas = 0
        if (marcarPresencaAuto) {
            withContext(Dispatchers.IO) {
                val jaPresentes = db.presencaDao().getPresentes(dataPresenca).toSet()
                for (ident in identificados) {
                    if (ident.alunoNome !in jaPresentes) {
                        db.presencaDao().insert(Presenca(data = dataPresenca, aluno = ident.alunoNome))
                        presencasMarcadas++
                    }
                }
            }
        }

        return TrainingPhotoResult(
            totalRostosDetectados = detected.size,
            identificados = identificados,
            desconhecidos = desconhecidos,
            presencasMarcadas = presencasMarcadas,
            inadimplentes = identificados.filter { it.isInadimplente },
            aniversariantes = identificados.filter { it.isAniversariante }
        )
    }

    // ═══════════════════════════════════════════════════════
    // Cadastro individual
    // ═══════════════════════════════════════════════════════

    suspend fun detectarRostoUnico(context: Context, imageUri: Uri): Pair<Bitmap?, FloatArray?> {
        return withContext(Dispatchers.Default) {
            try {
                val bmp = loadBitmap(context, imageUri) ?: return@withContext Pair(null, null)
                detectarRostoUnicoBitmap(context, bmp)
            } catch (e: Exception) {
                Pair(null, null)
            }
        }
    }

    suspend fun detectarRostoUnicoBitmap(context: Context, bitmap: Bitmap): Pair<Bitmap?, FloatArray?> {
        return withContext(Dispatchers.Default) {
            try {
                val input = InputImage.fromBitmap(bitmap, 0)
                val faces: List<Face> = detector.process(input).await()
                if (faces.isEmpty()) return@withContext Pair(null, null)
                val face: Face = faces[0]
                val box: Rect = face.boundingBox
                val faceBmp = cropFaceSafe(bitmap, box) ?: return@withContext Pair(null, null)
                val faceNet = FaceNetModel.get(context)
                val emb = faceNet.getEmbedding(faceBmp) ?: return@withContext Pair(null, null)
                Pair(faceBmp, emb)
            } catch (e: Exception) {
                Pair(null, null)
            }
        }
    }

    suspend fun cadastrarRosto(context: Context, alunoId: Long, embedding: FloatArray) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.get(context)
            db.faceEmbeddingDao().insert(
                FaceEmbedding(
                    alunoId = alunoId,
                    embedding = serializeEmbedding(embedding),
                    createdAt = java.time.LocalDateTime.now().toString()
                )
            )
        }
    }

    // ═══════════════════════════════════════════════════════
    // Métricas de similaridade
    // ═══════════════════════════════════════════════════════

    /** Similaridade coseno: 1 = idêntico, 0 = diferente, -1 = oposto */
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        val n = minOf(a.size, b.size)
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in 0 until n) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = (sqrt(normA) * sqrt(normB)).coerceAtLeast(0.0001f)
        return dot / denom
    }

    // ═══════════════════════════════════════════════════════
    // Utilitários
    // ═══════════════════════════════════════════════════════

    private fun serializeEmbedding(emb: FloatArray): String = Gson().toJson(emb.toList())

    private fun deserializeEmbedding(json: String): FloatArray? {
        return try {
            val type = object : TypeToken<List<Float>>() {}.type
            val list: List<Float> = Gson().fromJson(json, type)
            list.toFloatArray()
        } catch (e: Exception) { null }
    }

    private fun loadBitmap(ctx: Context, uri: Uri): Bitmap? {
        return try {
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) { null }
    }

    private fun cropFaceSafe(bmp: Bitmap, box: Rect): Bitmap? {
        // Expandir bounding box em 30% para pegar mais contexto do rosto
        val expandW = (box.width() * 0.3f).toInt()
        val expandH = (box.height() * 0.3f).toInt()
        val x = maxOf(0, box.left - expandW)
        val y = maxOf(0, box.top - expandH)
        val w = minOf(bmp.width - x, box.width() + expandW * 2)
        val h = minOf(bmp.height - y, box.height() + expandH * 2)
        if (w <= 0 || h <= 0) return null
        return Bitmap.createBitmap(bmp, x, y, w, h)
    }
}
