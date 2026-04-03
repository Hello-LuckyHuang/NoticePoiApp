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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.helloluckyhuang.lbspoiapp.ui.HomePage
import com.helloluckyhuang.lbspoiapp.ui.MapPage
import com.helloluckyhuang.lbspoiapp.ui.SettingPage
import com.helloluckyhuang.lbspoiapp.ui.theme.LBSPOIAppTheme
import com.helloluckyhuang.lbspoiapp.ui.viewmodel.PoiProjectViewModel
import com.helloluckyhuang.lbspoiapp.ui.viewmodel.PoiProjectViewModelFactory
import com.helloluckyhuang.lbspoiapp.ui.viewmodel.PoiViewModel
import com.helloluckyhuang.lbspoiapp.ui.viewmodel.PoiViewModelFactory
import com.helloluckyhuang.lbspoiapp.util.hasBackgroundLocation

class MainActivity : ComponentActivity() {
    private val viewModel: PoiProjectViewModel by viewModels {
        val repo = (application as PoiApp).poiProjectRepository
        PoiProjectViewModelFactory(repo)
    }

    private val poiViewModel: PoiViewModel by viewModels {
        val repo = (application as PoiApp).poiProjectRepository
        PoiViewModelFactory(repo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置高德地图条款(直接同意了，反正这是我的校内实习项目)
//        MapsInitializer.updatePrivacyShow(context, true, true)
//        MapsInitializer.updatePrivacyAgree(context, true)

        enableEdgeToEdge()
        setContent {
            LBSPOIAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Pages(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // 从后台回到前台时，若用户本来就在定位，且没有后台权限，则恢复前台定位
        if (!hasBackgroundLocation(this) && PoiApp.trackingEnabled) {
            PoiApp.locationRepo.startForegroundOnly()
        }
    }

    override fun onStop() {
        super.onStop()
        // 如果没有背景定位权限，停止定位服务
        if (!hasBackgroundLocation(this)) {
            PoiApp.locationRepo.stop()
        }
    }

    @Composable
    fun Pages(modifier: Modifier = Modifier) {
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
                        poiViewModel = poiViewModel,
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
                        viewModel = poiViewModel,
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
