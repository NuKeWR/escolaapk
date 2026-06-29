package com.nuklear.escola.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuklear.escola.ui.theme.*

@Composable
fun StatCard(
    titulo: String,
    valor: String,
    cor: androidx.compose.ui.graphics.Color = NukPurple,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(valor, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NukWhite)
            Text(titulo, fontSize = 11.sp, color = NukWhite.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun NukCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NukDarkGray)
    ) {
        Column(modifier = Modifier.padding(14.dp), content = content)
    }
}

@Composable
fun AlunoListItem(
    nome: String,
    subtitulo: String,
    status: String,
    statusColor: androidx.compose.ui.graphics.Color = NukGreen,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = NukDarkGray)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(nome, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NukWhite,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitulo.isNotBlank()) {
                    Text(subtitulo, fontSize = 11.sp, color = NukLightGray)
                }
                Text(status, fontSize = 11.sp, color = statusColor)
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = NukPurple,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun SectionTitle(text: String, emoji: String = "") {
    Text(
        "$emoji $text",
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = NukPurple,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
fun NukTopBar(
    title: String,
    onMenuClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NukPurple,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenuClick) {
                Text("☰", fontSize = 22.sp, color = NukWhite)
            }
            Spacer(Modifier.width(8.dp))
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NukWhite)
        }
    }
}
