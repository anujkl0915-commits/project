package com.rekaro.app.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.rekaro.app.ui.screens.CameraScreen
import com.rekaro.app.ui.screens.HomeScreen
import com.rekaro.app.ui.screens.ResultScreen
import com.rekaro.app.ui.screens.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val CAMERA = "camera"
    const val RESULT = "result/{itemName}/{category}"

    fun resultRoute(itemName: String, category: String): String {
        return "result/${java.net.URLEncoder.encode(itemName, "UTF-8")}/" +
                "${java.net.URLEncoder.encode(category, "UTF-8")}"
    }
}

@Composable
fun ReKaroNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(400)
            ) + fadeIn(animationSpec = tween(400))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(400)
            ) + fadeOut(animationSpec = tween(400))
        }
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onSplashComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onScanClick = {
                    navController.navigate(Routes.CAMERA)
                },
                onResultClick = { itemName, category ->
                    navController.navigate(Routes.resultRoute(itemName, category))
                }
            )
        }

        composable(Routes.CAMERA) {
            CameraScreen(
                onResult = { itemName, category ->
                    navController.navigate(Routes.resultRoute(itemName, category)) {
                        popUpTo(Routes.HOME)
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.RESULT,
            arguments = listOf(
                navArgument("itemName") { type = NavType.StringType },
                navArgument("category") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val itemName = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("itemName") ?: "Unknown", "UTF-8"
            )
            val category = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("category") ?: "Non-Recyclable", "UTF-8"
            )
            ResultScreen(
                itemName = itemName,
                categoryName = category,
                onScanAgain = {
                    navController.navigate(Routes.CAMERA) {
                        popUpTo(Routes.HOME)
                    }
                },
                onHome = {
                    navController.popBackStack(Routes.HOME, false)
                }
            )
        }
    }
}