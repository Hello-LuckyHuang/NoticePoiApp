package com.helloluckyhuang.lbspoiapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.sp
import com.helloluckyhuang.lbspoiapp.PoiApp
import com.helloluckyhuang.lbspoiapp.ui.floatframe.closeFloat
import com.helloluckyhuang.lbspoiapp.ui.map.MapCard
import com.helloluckyhuang.lbspoiapp.ui.viewmodel.PoiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPage(
    viewModel: PoiViewModel,
    projectUid: Int,
    onNavigateToHomePage: () -> Unit
) {
    val projectName by viewModel.projectName.collectAsState()

    LaunchedEffect(projectUid) {
        viewModel.loadPoiProject(projectUid)
    }

    Column {
        TopAppBar(
            title = {
                Text("你将去的地方 $projectName")
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.primary,
            ),
            navigationIcon = {
                IconButton(
                    onClick = {
                        viewModel.saveCurrentMapPoiListAndExit {
                            onNavigateToHomePage()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            },
            windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
        )
        MapCard(
            viewModel = viewModel
        )
    }
}
