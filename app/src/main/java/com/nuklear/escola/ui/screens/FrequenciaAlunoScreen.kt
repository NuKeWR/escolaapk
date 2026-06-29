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
fun FrequenciaAlunoScreen(
    nomeAluno: String,
    onMenuClick: () -> Unit
) {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.get(ctx) }
    val scope = rememberCoroutineScope()

    var presencas by remember { mutableStateOf<List<String>>(emptyList()) }
    var showDeleteAll by remember { mutableStateOf(false) }

    fun carregar() {
        scope.launch { presencas = db.presencaDao().getByAluno(nomeAluno) }
    }

    LaunchedEffect(nomeAluno) { carregar() }

    if (showDeleteAll) {
        AlertDialog(
            onDismissRequest = { showDeleteAll = false },
            title = { Text("⚠️ Confirmar") },
            text = { Text("Excluir TODAS as presenças de $nomeAluno?", color = NukWhite) },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        db.presencaDao().deleteByAluno(nomeAluno)
                        showDeleteAll = false; carregar()
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = NukPurple)) { Text("SIM") }
            },
            dismissButton = {
                Button(onClick = { showDeleteAll = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NukMedGray)) { Text("NÃO") }
            },
            containerColor = NukDarkGray
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NukTopBar("📅 Frequência", onMenuClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("📅 $nomeAluno", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NukPurple)
            Text("Total: ${presencas.size} presença(s)", fontSize = 14.sp, color = NukWhite)

            if (presencas.isNotEmpty()) {
                Button(
                    onClick = { showDeleteAll = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NukRedDark),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🗑 Excluir Todas") }
            }

            presencas.forEach { data ->
                NukCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("📅 $data", fontSize = 14.sp, color = NukWhite)
                        Button(
                            onClick = {
                                scope.launch {
                                    db.presencaDao().delete(data, nomeAluno)
                                    carregar()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NukRedDark),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) { Text("✖", fontSize = 14.sp) }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}
