package com.nuklear.escola

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.nuklear.escola.ui.screens.*
import com.nuklear.escola.ui.theme.*
import com.nuklear.escola.notification.NotificationHelper
import com.nuklear.escola.notification.DailyCheckWorker
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar notificações
        NotificationHelper.createChannels(this)
        DailyCheckWorker.agendar(this)
        DailyCheckWorker.executarAgora(this)

        // Pedir permissão de notificação (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        setContent { EscolaNuklearTheme { MainApp() } }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route ?: "dashboard"

    val menuItems = listOf(
        "📊  Dashboard" to "dashboard",
        "👥  Alunos" to "alunos",
        "➕  Cadastrar Aluno" to "cadastrar",
        "💳  Pagamento Rápido" to "pagamento_rapido",
        "📸  Reconhecimento Facial" to "reconhecimento",
        "🧠  Cadastro de Rostos" to "cadastro_lote",
        "✅  Presenças" to "presencas",
        "📈  Relatório Frequência" to "relatorio",
        "🎂  Aniversariantes" to "aniversariantes",
        "🔴  Inadimplentes" to "inadimplentes",
        "💰  Histórico Financeiro" to "historico",
        "⚙️  Configurações" to "configuracoes",
    )

    // Barra inferior: telas mais usadas
    data class BottomItem(val label: String, val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
    val bottomItems = listOf(
        BottomItem("Home", "dashboard", Icons.Default.Home),
        BottomItem("Alunos", "alunos", Icons.Default.People),
        BottomItem("Pagar", "pagamento_rapido", Icons.Default.Payment),
        BottomItem("Foto", "reconhecimento", Icons.Default.CameraAlt),
        BottomItem("Config", "configuracoes", Icons.Default.Settings),
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = NukBlack, modifier = Modifier.width(280.dp)) {
                Box(
                    modifier = Modifier.fillMaxWidth().background(NukPurple).padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(painter = painterResource(id = R.drawable.logo), contentDescription = "Logo",
                            modifier = Modifier.size(70.dp).clip(CircleShape))
                        Spacer(Modifier.height(6.dp))
                        Text("ESCOLA NUKLEAR", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NukCream)
                        Text("De Artes Marciais", fontSize = 11.sp, color = NukCream.copy(alpha = 0.8f))
                    }
                }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    menuItems.forEach { (label, route) ->
                        NavigationDrawerItem(
                            label = { Text(label, fontSize = 14.sp, color = NukWhite) },
                            selected = currentRoute == route,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(route) { popUpTo("dashboard") { saveState = true }; launchSingleTop = true; restoreState = true }
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = NukBlack, selectedContainerColor = NukPurpleDark))
                    }
                    Spacer(Modifier.weight(1f))
                    Text("v1.2 — Escola Nuklear", fontSize = 11.sp, color = NukLightGray, modifier = Modifier.padding(16.dp))
                }
            }
        }
    ) {
        val openMenu: () -> Unit = { scope.launch { drawerState.open() } }

        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = NukDarkGray, tonalElevation = 8.dp) {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo("dashboard") { saveState = true }; launchSingleTop = true; restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(22.dp)) },
                            label = { Text(item.label, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NukCream,
                                selectedTextColor = NukCream,
                                indicatorColor = NukPurple,
                                unselectedIconColor = NukLightGray,
                                unselectedTextColor = NukLightGray
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                NavHost(navController = navController, startDestination = "dashboard") {
                    composable("dashboard") { DashboardScreen(onMenuClick = openMenu) }
                    composable("alunos") { AlunosScreen(onMenuClick = openMenu, onAlunoClick = { id -> navController.navigate("editar/$id") }) }
                    composable("cadastrar") { CadastrarAlunoScreen(onMenuClick = openMenu, onSaved = { navController.navigate("alunos") { popUpTo("dashboard") } }) }
                    composable("pagamento_rapido") { PagamentoRapidoScreen(onMenuClick = openMenu) }
                    composable("editar/{alunoId}", arguments = listOf(navArgument("alunoId") { type = NavType.LongType })) { bs ->
                        EditarAlunoScreen(alunoId = bs.arguments?.getLong("alunoId") ?: 0L, onMenuClick = openMenu,
                            onPagamentos = { id -> navController.navigate("pagamentos/$id") },
                            onFrequencia = { nome -> navController.navigate("frequencia/${java.net.URLEncoder.encode(nome, "UTF-8")}") },
                            onDeleted = { navController.navigate("alunos") { popUpTo("dashboard") } },
                            onVoltar = { navController.popBackStack() })
                    }
                    composable("reconhecimento") { ReconhecimentoScreen(onMenuClick = openMenu, onAlunoClick = { id -> navController.navigate("editar/$id") }, onCadastrarAluno = { navController.navigate("cadastrar") }) }
                    composable("cadastro_lote") { CadastroLoteScreen(onMenuClick = openMenu) }
                    composable("presencas") { PresencasScreen(onMenuClick = openMenu) }
                    composable("presencas_data/{data}", arguments = listOf(navArgument("data") { type = NavType.StringType })) { bs -> PresencasDataScreen(data = bs.arguments?.getString("data") ?: "", onMenuClick = openMenu) }
                    composable("pagamentos/{alunoId}", arguments = listOf(navArgument("alunoId") { type = NavType.LongType })) { bs -> PagamentosScreen(alunoId = bs.arguments?.getLong("alunoId") ?: 0L, onMenuClick = openMenu, onRecibo = { pagId -> navController.navigate("recibo/$pagId") }) }
                    composable("recibo/{pagamentoId}", arguments = listOf(navArgument("pagamentoId") { type = NavType.LongType })) { bs -> ReciboScreen(pagamentoId = bs.arguments?.getLong("pagamentoId") ?: 0L, onMenuClick = openMenu) }
                    composable("relatorio") { RelatorioFrequenciaScreen(onMenuClick = openMenu, onFrequenciaAluno = { nome -> navController.navigate("frequencia/${java.net.URLEncoder.encode(nome, "UTF-8")}") }) }
                    composable("frequencia/{nome}", arguments = listOf(navArgument("nome") { type = NavType.StringType })) { bs -> FrequenciaAlunoScreen(nomeAluno = java.net.URLDecoder.decode(bs.arguments?.getString("nome") ?: "", "UTF-8"), onMenuClick = openMenu) }
                    composable("aniversariantes") { AniversariantesScreen(onMenuClick = openMenu, onAlunoClick = { id -> navController.navigate("editar/$id") }) }
                    composable("inadimplentes") { InadimplentesScreen(onMenuClick = openMenu, onPagamentos = { id -> navController.navigate("pagamentos/$id") }) }
                    composable("historico") { HistoricoFinanceiroScreen(onMenuClick = openMenu) }
                    composable("configuracoes") { ConfiguracoesScreen(onMenuClick = openMenu) }
                }
            }
        }
    }
}
