package com.nuklear.escola.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuklear.escola.R
import com.nuklear.escola.security.PinStore
import com.nuklear.escola.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaPin(onDesbloqueado: () -> Unit) {
    val ctx = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf("") }
    var tentativas by remember { mutableStateOf(0) }
    var bloqueadoAte by remember { mutableStateOf(0L) }
    var agora by remember { mutableStateOf(System.currentTimeMillis()) }

    // Depois de 5 erros seguidos, espera 30 segundos antes de aceitar outra tentativa.
    LaunchedEffect(bloqueadoAte) {
        while (System.currentTimeMillis() < bloqueadoAte) {
            agora = System.currentTimeMillis()
            delay(500)
        }
        agora = System.currentTimeMillis()
    }
    val esperando = agora < bloqueadoAte

    fun tentar() {
        if (esperando || pin.length < 4) return
        if (PinStore.conferir(ctx, pin)) {
            onDesbloqueado()
        } else {
            tentativas++
            pin = ""
            if (tentativas % 5 == 0) {
                bloqueadoAte = System.currentTimeMillis() + 30_000L
                erro = "Muitas tentativas. Aguarde 30 segundos."
            } else {
                erro = "PIN incorreto"
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(NukBlack).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo",
            modifier = Modifier.size(90.dp).clip(CircleShape)
        )
        Spacer(Modifier.height(12.dp))
        Text("ESCOLA NUKLEAR", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NukWhite)
        Spacer(Modifier.height(24.dp))
        Text("🔒 Digite o PIN para entrar", fontSize = 14.sp, color = NukLightGray)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = { novo ->
                pin = novo.filter { it.isDigit() }.take(8)
                erro = ""
            },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { tentar() }),
            modifier = Modifier.fillMaxWidth(0.7f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NukPurple, unfocusedBorderColor = NukMedGray,
                focusedTextColor = NukWhite, unfocusedTextColor = NukWhite
            )
        )

        if (erro.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(erro, fontSize = 13.sp, color = NukRedLight)
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { tentar() },
            enabled = !esperando && pin.length >= 4,
            modifier = Modifier.fillMaxWidth(0.7f).height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NukPurple)
        ) { Text("Entrar", fontWeight = FontWeight.Bold) }
    }
}
