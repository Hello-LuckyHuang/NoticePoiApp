package com.helloluckyhuang.lbspoiapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.helloluckyhuang.lbspoiapp.ui.map.MapCard
import com.helloluckyhuang.lbspoiapp.ui.viewmodel.PoiProjectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPage(
    viewModel: PoiProjectViewModel,
    projectUid: Int,
    onNavigateToHomePage: () -> Unit
) {
    val poiList by viewModel.currentMapPoiList.collectAsState()
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
                        viewModel.saveCurrentMapPoiList(projectUid) {
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
            projectUid = projectUid,
            poiList = poiList,
            viewModel = viewModel
        )
    }
}
