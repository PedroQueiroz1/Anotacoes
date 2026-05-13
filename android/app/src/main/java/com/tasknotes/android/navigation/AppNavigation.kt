package com.tasknotes.android.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tasknotes.android.ui.category.CategoryScreen
import com.tasknotes.android.ui.category.CategoryViewModel
import com.tasknotes.android.ui.home.HomeScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {

        composable("home") {
            HomeScreen(
                onNavigateToCategory = { id, name ->
                    navController.navigate("category/$id/${Uri.encode(name)}")
                }
            )
        }

        composable(
            route     = "category/{categoryId}/{categoryName}",
            arguments = listOf(
                navArgument("categoryId")   { type = NavType.LongType },
                navArgument("categoryName") { type = NavType.StringType }
            )
        ) {
            val vm: CategoryViewModel = viewModel()
            CategoryScreen(
                viewModel = vm,
                onBack    = { navController.popBackStack() }
            )
        }
    }
}
