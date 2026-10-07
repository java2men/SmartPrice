package ru.embtlab.smartprice.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import ru.embtlab.smartprice.presentation.compare.CompareScreen
import ru.embtlab.smartprice.presentation.compare.CompareViewModel
import ru.embtlab.smartprice.presentation.compare.EditProductScreen
import ru.embtlab.smartprice.presentation.settings.SettingsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Compare.route,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(route = Screen.Compare.route) { backStackEntry ->
            // Получаем общую CompareViewModel для экрана сравнения и экрана редактирования
            val compareViewModel: CompareViewModel = hiltViewModel(backStackEntry)
            CompareScreen(
                viewModel = compareViewModel,
                onOpenSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onEditProduct = { productId ->
                    navController.navigate(Screen.EditProduct.createRoute(productId))
                }
            )
        }

        composable(
            route = Screen.EditProduct.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId").orEmpty()

            // Берём ту же ViewModel из Compare экрана, чтобы изменения мгновенно применялись
            val compareBackStackEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Compare.route)
            }
            val compareViewModel: CompareViewModel = hiltViewModel(compareBackStackEntry)

            EditProductScreen(
                productId = productId,
                viewModel = compareViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}