package com.nuklear.escola.ui.screens

import android.widget.Toast
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
import com.nuklear.escola.ui.components.*
import com.nuklear.escola.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun PresencasDataScreen(
    data: String,
    onMenuClick: () -> Unit
) {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.get(ctx) }
    val scope = rememberCoroutineScope()

    var presentes by remember { mutableStateOf<List<String>>(emptyList()) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    fun carregar() {
        scope.launch { presentes = db.presencaDao().getPresentes(data) }
    }

    LaunchedEffect(data) { carregar() }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("⚠️ Confirmar") },
            text = { Text("Excluir TODAS as ${presentes.size} presenças de $data?", color = NukWhite) },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        db.presencaDao().deleteByData(data)
                        showDeleteAllDialog = false
                        Toast.makeText(ctx, "Presenças de $data removidas", Toast.LENGTH_SHORT).show()
                        carregar()
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = NukPurple)) { Text("SIM") }
            },
            dismissButton = {
                Button(onClick = { showDeleteAllDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NukMedGray)) { Text("NÃO") }
            },
            containerColor = NukDarkGray
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NukTopBar("📅 Presenças em $data", onMenuClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("📅 $data", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NukPurple)
            Text("${presentes.size} aluno(s) presente(s)", fontSize = 14.sp, color = NukWhite)

            if (presentes.isNotEmpty()) {
                Button(
                    onClick = { showDeleteAllDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NukRedDark),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🗑 Excluir Todas as Presenças desta Data") }
            }

            presentes.sorted().forEach { aluno ->
                NukCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("✅ $aluno", fontSize = 14.sp, color = NukWhite)
                        Button(
                            onClick = {
                                scope.launch {
                                    db.presencaDao().delete(data, aluno)
                                    carregar()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NukRedDark),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) { Text("✖", fontSize = 14.sp) }
                    }
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}
