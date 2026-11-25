package com.nuklear.escola

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.nuklear.escola.notification.DailyCheckWorker
import com.nuklear.escola.notification.NotificationHelper
import com.nuklear.escola.security.PinStore
import com.nuklear.escola.ui.components.LocalVoltar
import com.nuklear.escola.ui.screens.*
import com.nuklear.escola.ui.theme.*

class MainActivity : ComponentActivity() {
    // Trava por PIN: bloqueia ao abrir e depois de ficar 3 minutos fora do app.
    private val bloqueado = mutableStateOf(false)
    private var ultimoStop = 0L

    override fun onStop() {
        super.onStop()
        ultimoStop = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        if (ultimoStop > 0L && PinStore.temPin(this) &&
            System.currentTimeMillis() - ultimoStop > 180_000L) {
            bloqueado.value = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bloqueado.value = PinStore.temPin(this)

        // Configurar notificações
        NotificationHelper.createChannels(this)
        DailyCheckWorker.agendar(this)
        DailyCheckWorker.executarAgora(this)

        // Pedir permissão de notificação (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        setContent {
            EscolaNuklearTheme {
                if (bloqueado.value) TelaPin(onDesbloqueado = { bloqueado.value = false })
                else MainApp()
            }
        }
    }
}

// As 5 abas da barra inferior
private data class Aba(val rotulo: String, val rota: String, val icone: androidx.compose.ui.graphics.vector.ImageVector)

private val ABAS = listOf(
    Aba("Início", "dashboard", Icons.Default.Home),
    Aba("Alunos", "alunos", Icons.Default.People),
    Aba("Aulas", "presencas", Icons.Default.EventAvailable),
    Aba("Financeiro", "financeiro", Icons.Default.Payments),
    Aba("Perfil", "configuracoes", Icons.Default.Person),
)

/** Em qual aba "mora" cada tela (para destacar a aba certa quando se está numa tela interna). */
private fun abaDaRota(rota: String): String = when {
    rota == "dashboard" || rota == "aniversariantes" -> "dashboard"
    rota == "alunos" || rota == "cadastrar" || rota.startsWith("editar/") -> "alunos"
    rota == "presencas" || rota == "reconhecimento" || rota == "cadastro_lote" ||
        rota == "relatorio" || rota.startsWith("presencas_data/") || rota.startsWith("frequencia/") -> "presencas"
    rota == "financeiro" || rota == "pagamento_rapido" || rota == "inadimplentes" ||
        rota == "historico" || rota.startsWith("pagamentos/") || rota.startsWith("recibo/") -> "financeiro"
    rota == "configuracoes" -> "configuracoes"
    else -> "dashboard"
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route ?: "dashboard"
    val abaAtual = abaDaRota(currentRoute)

    // Qualquer tela que não seja uma das 5 abas ganha a seta "←" no topo para voltar
    val ehAba = ABAS.any { it.rota == currentRoute }
    val acaoVoltar: (() -> Unit)? = if (ehAba) null else ({ navController.popBackStack() })

    // Navegação das abas (guarda e restaura o estado de cada uma)
    fun irParaAba(rota: String) {
        navController.navigate(rota) {
            popUpTo("dashboard") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Abre qualquer tela a partir de um atalho (Início, Perfil...)
    fun irPara(rota: String) {
        if (ABAS.any { it.rota == rota }) irParaAba(rota)
        else navController.navigate(rota) { launchSingleTop = true }
    }

    val semMenu: () -> Unit = {}

    Scaffold(
        containerColor = NukBlack,
        bottomBar = {
            NavigationBar(containerColor = NukBar, tonalElevation = 0.dp) {
                ABAS.forEach { aba ->
                    NavigationBarItem(
                        selected = abaAtual == aba.rota,
                        onClick = {
                            // Tocar na aba em que já se está volta para a tela principal dela
                            if (currentRoute != aba.rota && abaAtual == aba.rota) {
                                if (!navController.popBackStack(aba.rota, false)) irParaAba(aba.rota)
                            } else if (currentRoute != aba.rota) {
                                irParaAba(aba.rota)
                            }
                        },
                        icon = { Icon(aba.icone, contentDescription = aba.rotulo, modifier = Modifier.size(24.dp)) },
                        label = { Text(aba.rotulo, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NukPurpleLight,
                            selectedTextColor = NukPurpleLight,
                            indicatorColor = NukPurple.copy(alpha = 0.18f),
                            unselectedIconColor = NukLightGray,
                            unselectedTextColor = NukLightGray
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            CompositionLocalProvider(LocalVoltar provides acaoVoltar) {
                NavHost(navController = navController, startDestination = "dashboard") {
                    // ─── As 5 abas ───
                    composable("dashboard") {
                        DashboardScreen(onMenuClick = semMenu, onNavigate = { rota -> irPara(rota) })
                    }
                    composable("alunos") {
                        AlunosScreen(
                            onMenuClick = semMenu,
                            onAlunoClick = { id -> navController.navigate("editar/$id") },
                            onCadastrar = { navController.navigate("cadastrar") }
                        )
                    }
                    composable("presencas") {
                        PresencasScreen(
                            onMenuClick = semMenu,
                            onReconhecer = { navController.navigate("reconhecimento") },
                            onRelatorio = { navController.navigate("relatorio") }
                        )
                    }
                    composable("financeiro") {
                        FinanceiroScreen(
                            onMenuClick = semMenu,
                            onNovoPagamento = { navController.navigate("pagamento_rapido") },
                            onPendencias = { navController.navigate("inadimplentes") },
                            onHistorico = { navController.navigate("historico") },
                            onRecibo = { id -> navController.navigate("recibo/$id") }
                        )
                    }
                    composable("configuracoes") {
                        ConfiguracoesScreen(onMenuClick = semMenu, onNavigate = { rota -> irPara(rota) })
                    }

                    // ─── Telas internas ───
                    composable("cadastrar") {
                        CadastrarAlunoScreen(
                            onMenuClick = semMenu,
                            onSaved = { navController.navigate("alunos") { popUpTo("dashboard") } }
                        )
                    }
                    composable("pagamento_rapido") {
                        PagamentoRapidoScreen(
                            onMenuClick = semMenu,
                            onRecibo = { id -> navController.navigate("recibo/$id") }
                        )
                    }
                    composable("editar/{alunoId}", arguments = listOf(navArgument("alunoId") { type = NavType.LongType })) { bs ->
                        EditarAlunoScreen(
                            alunoId = bs.arguments?.getLong("alunoId") ?: 0L, onMenuClick = semMenu,
                            onPagamentos = { id -> navController.navigate("pagamentos/$id") },
                            onFrequencia = { nome -> navController.navigate("frequencia/${java.net.URLEncoder.encode(nome, "UTF-8")}") },
                            onDeleted = { navController.navigate("alunos") { popUpTo("dashboard") } },
                            onVoltar = { navController.popBackStack() }
                        )
                    }
                    composable("reconhecimento") {
                        ReconhecimentoScreen(
                            onMenuClick = semMenu,
                            onAlunoClick = { id -> navController.navigate("editar/$id") },
                            onCadastrarAluno = { navController.navigate("cadastrar") }
                        )
                    }
                    composable("cadastro_lote") { CadastroLoteScreen(onMenuClick = semMenu) }
                    composable("presencas_data/{data}", arguments = listOf(navArgument("data") { type = NavType.StringType })) { bs ->
                        PresencasDataScreen(data = bs.arguments?.getString("data") ?: "", onMenuClick = semMenu)
                    }
                    composable("pagamentos/{alunoId}", arguments = listOf(navArgument("alunoId") { type = NavType.LongType })) { bs ->
                        PagamentosScreen(
                            alunoId = bs.arguments?.getLong("alunoId") ?: 0L, onMenuClick = semMenu,
                            onRecibo = { pagId -> navController.navigate("recibo/$pagId") }
                        )
                    }
                    composable("recibo/{pagamentoId}", arguments = listOf(navArgument("pagamentoId") { type = NavType.LongType })) { bs ->
                        ReciboScreen(pagamentoId = bs.arguments?.getLong("pagamentoId") ?: 0L, onMenuClick = semMenu)
                    }
                    composable("relatorio") {
                        RelatorioFrequenciaScreen(
                            onMenuClick = semMenu,
                            onFrequenciaAluno = { nome -> navController.navigate("frequencia/${java.net.URLEncoder.encode(nome, "UTF-8")}") }
                        )
                    }
                    composable("frequencia/{nome}", arguments = listOf(navArgument("nome") { type = NavType.StringType })) { bs ->
                        FrequenciaAlunoScreen(
                            nomeAluno = java.net.URLDecoder.decode(bs.arguments?.getString("nome") ?: "", "UTF-8"),
                            onMenuClick = semMenu
                        )
                    }
                    composable("aniversariantes") {
                        AniversariantesScreen(
                            onMenuClick = semMenu,
                            onAlunoClick = { id -> navController.navigate("editar/$id") }
                        )
                    }
                    composable("inadimplentes") {
                        InadimplentesScreen(
                            onMenuClick = semMenu,
                            onPagamentos = { id -> navController.navigate("pagamentos/$id") }
                        )
                    }
                    composable("historico") { HistoricoFinanceiroScreen(onMenuClick = semMenu) }
                }
            }
        }
    }
}
