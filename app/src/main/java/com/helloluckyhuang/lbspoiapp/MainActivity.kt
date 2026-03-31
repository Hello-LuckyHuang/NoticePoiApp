package com.helloluckyhuang.lbspoiapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.helloluckyhuang.lbspoiapp.ui.HomePage
import com.helloluckyhuang.lbspoiapp.ui.theme.LBSPOIAppTheme
import com.helloluckyhuang.lbspoiapp.ui.viewmodel.MapPage
import com.helloluckyhuang.lbspoiapp.ui.viewmodel.PoiProjectViewModel
import com.helloluckyhuang.lbspoiapp.ui.viewmodel.PoiProjectViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: PoiProjectViewModel by viewModels {
        val repo = (application as PoiApp).poiProjectRepository
        PoiProjectViewModelFactory(repo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LBSPOIAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Pages(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    @Composable
    fun Pages(modifier: Modifier = Modifier) {
        val navController = rememberNavController()
        NavHost(navController, startDestination = "main_page") {
            composable("main_page") {
                HomePage(
                    viewModel = viewModel,
                    onNavigateToMapPage = { uid ->
                        navController.navigate("map_page/$uid")
                    }
                )
            }
            composable(
                route = "map_page/{projectUid}",
                arguments = listOf(
                    navArgument("projectUid") { type = NavType.IntType }
                )
            ) { entry ->
                val projectUid = entry.arguments?.getInt("projectUid") ?: -1
                if (projectUid == -1) {
                    return@composable
                }
                MapPage(
                    viewModel = viewModel,
                    projectUid = projectUid,
                    onNavigateToHomePage = {
                        navController.navigate("main_page")
                    }
                )
            }
        }
    }
}
