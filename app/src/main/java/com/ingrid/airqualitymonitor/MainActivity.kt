package com.ingrid.airqualitymonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.ingrid.airqualitymonitor.ui.screens.DetailsScreen
import com.ingrid.airqualitymonitor.ui.screens.HomeScreen
import com.ingrid.airqualitymonitor.ui.screens.LegendaScreen
import com.ingrid.airqualitymonitor.ui.theme.AirQualityTheme
import com.ingrid.airqualitymonitor.ui.viewmodel.LabViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Faz o conteúdo ocupar a tela inteira, incluindo atrás da barra de status
        enableEdgeToEdge()
        // Define a interface usando Compose, envolto no tema do app
        setContent {
            AirQualityTheme {
                AirQualityNavHost()
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AirQualityNavHost() {
    // Controlador que gerencia o histórico de navegação entre telas
    val navController = rememberNavController()
    // ViewModel criada uma única vez e compartilhada entre HomeScreen e DetailsScreen
    val labViewModel: LabViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "home", // Tela inicial do app
        // Animação ao avançar para uma nova tela: desliza da direita + fade in
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        // Animação da tela atual ao ser substituída: sai levemente para a esquerda
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 4 },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        // Animação ao voltar (pop): tela anterior reaparece da esquerda
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 4 },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        // Animação da tela atual ao ser fechada pelo botão voltar: sai para a direita
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        // Rota "home" → exibe a lista de laboratórios
        composable("home") {
            HomeScreen(
                labViewModel = labViewModel,
                onLabSelected = { labId ->
                    navController.navigate("details/$labId")
                },
                onLegendaClick = {
                    navController.navigate("legenda")
                }
            )
        }

        // Rota "legenda" → tela explicativa das variáveis monitoradas
        composable("legenda") {
            LegendaScreen(onBack = { navController.popBackStack() })
        }

        // Rota "details/{labId}" → aceita o ID do laboratório como parâmetro dinâmico
        composable(
            route = "details/{labId}",
            arguments = listOf(navArgument("labId") { type = NavType.StringType })
        ) { backStackEntry ->
            // Recupera o labId que foi passado via navController.navigate(...)
            val labId = backStackEntry.arguments?.getString("labId") ?: return@composable
            DetailsScreen(
                labId = labId,
                labViewModel = labViewModel,
                onBack = { navController.popBackStack() } // Remove a tela atual da pilha e volta
            )
        }
    }
}
