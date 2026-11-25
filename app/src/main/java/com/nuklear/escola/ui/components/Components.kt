package com.nuklear.escola.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nuklear.escola.ui.theme.*
import java.util.Locale

// ═══════════════════════════════════════════════════════
// NUKLEAR — componentes do visual "Premium Dark"
// ═══════════════════════════════════════════════════════

private val FormatoBr = Locale("pt", "BR")

/** 1840.0 -> "R$ 1.840,00" */
fun moeda(valor: Double): String = "R$ " + String.format(FormatoBr, "%,.2f", valor)

/** 1840.0 -> "R$ 1.840" (sem centavos, para cartões compactos) */
fun moedaCurta(valor: Double): String = "R$ " + String.format(FormatoBr, "%,.0f", valor)

/** "Maria da Silva Santos" -> "MS" */
fun iniciais(nome: String): String {
    val partes = nome.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        partes.isEmpty() -> "?"
        partes.size == 1 -> partes[0].take(2).uppercase()
        else -> (partes.first().take(1) + partes.last().take(1)).uppercase()
    }
}

/** Cor da faixa: "Azul 2 graus" -> azul. */
fun corDaFaixa(faixa: String): Color {
    val f = faixa.trim().lowercase()
    return when {
        f.startsWith("branca") -> NukWhite
        f.startsWith("cinza") -> Color(0xFFA8A6B5)
        f.startsWith("amarela") -> Color(0xFFF2C94C)
        f.startsWith("laranja") -> Color(0xFFF2994A)
        f.startsWith("verde") -> NukGreen
        f.startsWith("azul") -> Color(0xFF5B9DFF)
        f.startsWith("roxa") -> NukPurpleLight
        f.startsWith("marrom") -> Color(0xFFC08A62)
        f.startsWith("preta") -> Color(0xFFD0CEDB)
        else -> NukLightGray
    }
}

// ─── Cartões ────────────────────────────────────────────

@Composable
fun NukCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NukDarkGray),
        border = BorderStroke(1.dp, NukBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp), content = content)
    }
}

@Composable
fun StatCard(
    titulo: String,
    valor: String,
    cor: Color = NukPurple,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NukDarkGray),
        border = BorderStroke(1.dp, NukBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(valor, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NukWhite)
            Text(titulo, fontSize = 11.sp, color = cor.copy(alpha = 0.9f))
        }
    }
}

/** Indicador do Início: ícone, título, valor grande e uma linha de variação. */
@Composable
fun MetricCard(
    icone: ImageVector,
    titulo: String,
    valor: String,
    detalhe: String = "",
    detalheCor: Color = NukGreen,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NukDarkGray),
        border = BorderStroke(1.dp, NukBorder)
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                    .background(NukPurple.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icone, contentDescription = null, tint = NukPurpleLight, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(titulo, fontSize = 12.sp, color = NukLightGray)
            Text(valor, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = NukWhite)
            if (detalhe.isNotBlank()) {
                Text(detalhe, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = detalheCor)
            }
        }
    }
}

/** Atalho do "Acesso rápido". */
@Composable
fun QuickAction(
    icone: ImageVector,
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val forma = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .width(80.dp)
            .clip(forma)
            .background(NukDarkGray)
            .border(1.dp, NukBorder, forma)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icone, contentDescription = texto, tint = NukPurpleLight, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(6.dp))
        Text(texto, fontSize = 11.sp, color = NukWhite, maxLines = 1)
    }
}

// ─── Alunos ─────────────────────────────────────────────

/** Círculo com as iniciais; mostra a foto por cima quando ela existe e carrega. */
@Composable
fun Avatar(
    nome: String,
    modifier: Modifier = Modifier,
    tamanho: Dp = 44.dp,
    foto: String = ""
) {
    Box(
        modifier = modifier
            .size(tamanho)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(NukPurple, NukPurpleDark))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            iniciais(nome),
            color = NukWhite,
            fontWeight = FontWeight.Bold,
            fontSize = (tamanho.value * 0.34f).sp
        )
        if (foto.isNotBlank()) {
            AsyncImage(
                model = foto,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** Selo arredondado ("Em dia", "Pendente"...). Emojis no começo do texto são ignorados. */
@Composable
fun StatusBadge(texto: String, cor: Color, modifier: Modifier = Modifier) {
    val limpo = texto.dropWhile { !it.isLetterOrDigit() }.trim().ifEmpty { texto }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(cor.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(limpo, color = cor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun AlunoListItem(
    nome: String,
    subtitulo: String,
    status: String,
    statusColor: Color = NukGreen,
    foto: String = "",
    subtituloCor: Color = NukLightGray,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NukDarkGray),
        border = BorderStroke(1.dp, NukBorder)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(nome = nome, foto = foto)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    nome, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = NukWhite,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (subtitulo.isNotBlank()) {
                    Text(
                        subtitulo, fontSize = 12.sp, color = subtituloCor,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (status.isNotBlank()) {
                StatusBadge(status, statusColor)
                Spacer(Modifier.width(6.dp))
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = NukLightGray,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** Linha de atalho (ícone + texto + seta), usada no Perfil. */
@Composable
fun LinhaAtalho(icone: ImageVector, texto: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(NukPurple.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icone, contentDescription = null, tint = NukPurpleLight, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(texto, fontSize = 14.sp, color = NukWhite, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NukLightGray, modifier = Modifier.size(20.dp))
    }
}

// ─── Textos e seletores ─────────────────────────────────

@Composable
fun SectionTitle(text: String, emoji: String = "") {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = NukPurpleLight,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

/** Título de seção grande, com ação opcional à direita ("Ver tudo"). */
@Composable
fun SectionHeader(titulo: String, acao: String = "", onAcao: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            titulo, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NukWhite,
            modifier = Modifier.weight(1f)
        )
        if (acao.isNotBlank()) {
            Text(
                "$acao  ›", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NukPurpleLight,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAcao).padding(4.dp)
            )
        }
    }
}

/** Seletor em formato de pílula com menu (ex.: filtro de horário). */
@Composable
fun PillDropdown(
    rotulo: String,
    opcoes: List<String>,
    onSelecionar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var aberto by remember { mutableStateOf(false) }
    val forma = RoundedCornerShape(50)
    Box(modifier) {
        Row(
            modifier = Modifier.clip(forma).background(NukDarkGray).border(1.dp, NukBorder, forma)
                .clickable { aberto = true }.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(rotulo, fontSize = 12.sp, color = NukWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = NukLightGray, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            opcoes.forEach { op ->
                DropdownMenuItem(text = { Text(op) }, onClick = { onSelecionar(op); aberto = false })
            }
        }
    }
}

// ─── Gráfico ────────────────────────────────────────────

/** Gráfico de linha suave com área preenchida; os rótulos ficam alinhados com os pontos. */
@Composable
fun LineChart(
    valores: List<Int>,
    rotulos: List<String>,
    modifier: Modifier = Modifier,
    destaque: Int = valores.size - 1
) {
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(110.dp)) {
            if (valores.isEmpty()) return@Canvas
            val maximo = (valores.maxOrNull() ?: 0).coerceAtLeast(1).toFloat()
            val margemY = 14.dp.toPx()
            val altura = size.height - 2 * margemY
            val larguraColuna = size.width / valores.size
            val pontos = valores.mapIndexed { i, v ->
                Offset(larguraColuna * (i + 0.5f), margemY + altura - (v / maximo) * altura)
            }

            val linha = Path()
            linha.moveTo(pontos[0].x, pontos[0].y)
            for (i in 1 until pontos.size) {
                val a = pontos[i - 1]
                val b = pontos[i]
                val meio = (a.x + b.x) / 2f
                linha.cubicTo(meio, a.y, meio, b.y, b.x, b.y)
            }

            val area = Path()
            area.moveTo(pontos[0].x, size.height)
            area.lineTo(pontos[0].x, pontos[0].y)
            for (i in 1 until pontos.size) {
                val a = pontos[i - 1]
                val b = pontos[i]
                val meio = (a.x + b.x) / 2f
                area.cubicTo(meio, a.y, meio, b.y, b.x, b.y)
            }
            area.lineTo(pontos.last().x, size.height)
            area.close()

            drawPath(
                path = area,
                brush = Brush.verticalGradient(listOf(NukPurple.copy(alpha = 0.30f), Color.Transparent))
            )
            drawPath(
                path = linha,
                color = NukPurple,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            pontos.forEachIndexed { i, p ->
                if (i == destaque) {
                    drawCircle(color = NukPurple.copy(alpha = 0.35f), radius = 9.dp.toPx(), center = p)
                    drawCircle(color = NukWhite, radius = 4.5.dp.toPx(), center = p)
                } else {
                    drawCircle(color = NukPurpleLight, radius = 3.dp.toPx(), center = p)
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            rotulos.forEachIndexed { i, r ->
                Text(
                    r,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    fontWeight = if (i == destaque) FontWeight.Bold else FontWeight.Normal,
                    color = if (i == destaque) NukWhite else NukLightGray
                )
            }
        }
    }
}

// ─── Barra superior ─────────────────────────────────────

// Quando não é nulo, a barra do topo mostra a seta "←" para voltar.
// A MainActivity liga isso em todas as telas que não são uma das 5 abas.
val LocalVoltar = compositionLocalOf<(() -> Unit)?> { null }

/**
 * Barra de título. O parâmetro onMenuClick é mantido só para as telas antigas continuarem
 * compilando (o app não tem mais menu lateral). Emojis no começo do título são ignorados.
 */
@Composable
fun NukTopBar(
    title: String,
    onMenuClick: () -> Unit
) {
    val voltar = LocalVoltar.current
    val limpo = title.dropWhile { !it.isLetterOrDigit() }.trim().ifEmpty { title }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NukBlack)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (voltar != null) {
            IconButton(onClick = voltar) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = NukPurpleLight)
            }
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Text(
            limpo, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NukWhite,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}
