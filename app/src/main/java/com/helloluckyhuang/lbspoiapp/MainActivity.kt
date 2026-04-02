package com.helloluckyhuang.lbspoiapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.amap.api.maps.MapsInitializer
import com.helloluckyhuang.lbspoiapp.ui.HomePage
import com.helloluckyhuang.lbspoiapp.ui.theme.LBSPOIAppTheme
import com.helloluckyhuang.lbspoiapp.ui.MapPage
import com.helloluckyhuang.lbspoiapp.ui.SettingPage
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
        val context = LocalContext.current
        // 设置高德地图条款(直接同意了，反正这是我的校内实习项目)
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)
        // 主界面导航图
        Box (modifier = modifier) {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = "main_page",
                // 切换动画
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300)
                    )
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        tween(300)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        tween(300)
                    )
                }
            ) {
                composable("main_page") {
                    HomePage(
                        viewModel = viewModel,
                        onNavigateToMapPage = { uid ->
                            navController.navigate("map_page/$uid")
                        },
                        onNavigateToSettingPage = {
                            navController.navigate("setting_page")
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
                            navController.popBackStack("main_page", false)
                        }
                    )
                }
                composable("setting_page") {
                    SettingPage(
                        onNavigateToHomePage = {
                            navController.popBackStack("main_page", false)
                        }
                    )
                }
            }
        }
    }
}
