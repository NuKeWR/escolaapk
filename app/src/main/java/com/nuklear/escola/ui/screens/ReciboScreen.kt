package com.nuklear.escola.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuklear.escola.data.db.AppDatabase
import com.nuklear.escola.data.model.*
import com.nuklear.escola.ui.components.*
import com.nuklear.escola.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream

@Composable
fun ReciboScreen(
    pagamentoId: Long,
    onMenuClick: () -> Unit
) {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.get(ctx) }
    val scope = rememberCoroutineScope()

    var pagamento by remember { mutableStateOf<Pagamento?>(null) }
    var aluno by remember { mutableStateOf<Aluno?>(null) }
    var reciboBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pagamentoId) {
        scope.launch {
            val p = db.pagamentoDao().getById(pagamentoId)
            pagamento = p
            if (p != null) {
                aluno = db.alunoDao().getById(p.alunoId)
                // Gerar imagem do recibo
                val a = aluno
                if (a != null) {
                    reciboBitmap = withContext(Dispatchers.Default) {
                        gerarReciboImagem(ctx, p, a)
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NukTopBar("🧾 Recibo", onMenuClick)

        if (pagamento == null || aluno == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Carregando...", color = NukLightGray)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mostrar imagem do recibo
                reciboBitmap?.let { bmp ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = NukDarkGray)
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Recibo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                    }
                }

                // Botão compartilhar imagem
                Button(
                    onClick = {
                        scope.launch {
                            reciboBitmap?.let { bmp ->
                                compartilharReciboImagem(ctx, bmp, aluno!!.nome, pagamento!!.competencia)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NukGreen),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("📤 COMPARTILHAR RECIBO (Imagem)", fontWeight = FontWeight.Bold) }

                // Botão compartilhar texto
                Button(
                    onClick = {
                        val p = pagamento!!
                        val a = aluno!!
                        val texto = buildString {
                            appendLine("═══ ESCOLA NUKLEAR ═══")
                            appendLine("DE ARTES MARCIAIS")
                            appendLine("RECIBO DE PAGAMENTO")
                            appendLine()
                            appendLine("Aluno: ${a.nome}")
                            appendLine("Turma: ${a.turma} — ${a.horario}")
                            appendLine("Competência: ${p.competencia}")
                            appendLine("Data: ${p.dataPagamento}")
                            appendLine("Forma: ${p.formaPagamento}")
                            appendLine("Valor: R$ ${"%.2f".format(p.valorPago)}")
                            if (p.observacao.isNotBlank()) appendLine("Obs: ${p.observacao}")
                            appendLine()
                            appendLine("Recibo #${p.id}")
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, texto)
                        }
                        ctx.startActivity(Intent.createChooser(intent, "Compartilhar"))
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NukBlue),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("📝 Compartilhar como Texto") }

                // Salvar na galeria
                Button(
                    onClick = {
                        scope.launch {
                            reciboBitmap?.let { bmp ->
                                val ok = salvarNaGaleria(ctx, bmp, "recibo_${aluno!!.nome}_${pagamento!!.competencia}")
                                val msg = if (ok) "✅ Recibo salvo na galeria!" else "❌ Erro ao salvar"
                                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NukMedGray),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("💾 Salvar na Galeria") }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Gerar imagem do recibo
// ═══════════════════════════════════════════════════════

private fun gerarReciboImagem(ctx: Context, p: Pagamento, a: Aluno): Bitmap {
    val w = 800
    val h = 1000
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    // Fundo
    canvas.drawColor(Color.parseColor("#1A1A1A"))

    // Barra vermelha topo
    val paintRed = Paint().apply { color = Color.parseColor("#7B2D8E"); style = Paint.Style.FILL }
    canvas.drawRect(0f, 0f, w.toFloat(), 160f, paintRed)

    // Textos do header
    val paintTitle = Paint().apply {
        color = Color.WHITE; textSize = 48f; typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER; isAntiAlias = true
    }
    canvas.drawText("☢ ESCOLA NUKLEAR", w / 2f, 70f, paintTitle)

    val paintSub = Paint().apply {
        color = Color.parseColor("#EEEEEE"); textSize = 28f
        textAlign = Paint.Align.CENTER; isAntiAlias = true
    }
    canvas.drawText("DE ARTES MARCIAIS", w / 2f, 110f, paintSub)
    canvas.drawText("RECIBO DE PAGAMENTO", w / 2f, 148f, paintSub)

    // Corpo
    val paintLabel = Paint().apply {
        color = Color.parseColor("#9E9E9E"); textSize = 26f; isAntiAlias = true
    }
    val paintValue = Paint().apply {
        color = Color.WHITE; textSize = 30f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
    }
    val paintLine = Paint().apply { color = Color.parseColor("#424242"); strokeWidth = 2f }

    var y = 220f
    val left = 50f
    val right = w - 50f

    fun campo(label: String, valor: String) {
        canvas.drawText(label, left, y, paintLabel)
        y += 36f
        canvas.drawText(valor, left, y, paintValue)
        y += 16f
        canvas.drawLine(left, y, right, y, paintLine)
        y += 30f
    }

    campo("Aluno", a.nome)
    campo("Turma", "${a.turma} — ${a.horario}")
    campo("Modalidade", a.modalidade)
    campo("Competência", p.competencia)
    campo("Data Pagamento", p.dataPagamento)
    campo("Forma", p.formaPagamento)

    if (p.observacao.isNotBlank()) {
        campo("Observação", p.observacao)
    }

    // Linha grossa vermelha
    y += 10f
    val paintRedLine = Paint().apply { color = Color.parseColor("#7B2D8E"); strokeWidth = 4f }
    canvas.drawLine(left, y, right, y, paintRedLine)
    y += 40f

    // Valor grande
    val paintValorLabel = Paint().apply {
        color = Color.parseColor("#9E9E9E"); textSize = 28f; textAlign = Paint.Align.CENTER; isAntiAlias = true
    }
    canvas.drawText("VALOR PAGO", w / 2f, y, paintValorLabel)
    y += 60f

    val paintValorGrande = Paint().apply {
        color = Color.parseColor("#4CAF50"); textSize = 72f
        typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER; isAntiAlias = true
    }
    canvas.drawText("R$ ${"%.2f".format(p.valorPago)}", w / 2f, y, paintValorGrande)

    // Rodapé
    y = h - 60f
    canvas.drawLine(left, y, right, y, paintLine)
    y += 30f
    val paintFooter = Paint().apply {
        color = Color.parseColor("#666666"); textSize = 22f
        textAlign = Paint.Align.CENTER; isAntiAlias = true
    }
    canvas.drawText("Recibo #${p.id} — Escola Nuklear de Artes Marciais", w / 2f, y, paintFooter)

    return bmp
}

// ═══════════════════════════════════════════════════════
// Compartilhar e salvar
// ═══════════════════════════════════════════════════════

private suspend fun compartilharReciboImagem(ctx: Context, bmp: Bitmap, nome: String, comp: String) {
    withContext(Dispatchers.IO) {
        try {
            val fileName = "recibo_${nome.replace(" ", "_")}_$comp.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/EscolaNuklear")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                ctx.contentResolver.openOutputStream(it)?.use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    ctx.contentResolver.update(it, values, null, null)
                }

                withContext(Dispatchers.Main) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, it)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    ctx.startActivity(Intent.createChooser(intent, "Compartilhar Recibo"))
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(ctx, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private suspend fun salvarNaGaleria(ctx: Context, bmp: Bitmap, name: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/EscolaNuklear")
                }
            }
            val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                ctx.contentResolver.openOutputStream(it)?.use { out ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
            true
        } catch (e: Exception) { false }
    }
}
