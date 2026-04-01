package com.helloluckyhuang.lbspoiapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    LaunchedEffect(projectUid) {
        viewModel.loadMapPoiList(projectUid)
    }

    Column {
        TopAppBar(
            title = {
                Text("地图")
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
            }
        )
        MapCard(
            projectUid = projectUid,
            poiList = poiList,
            viewModel = viewModel
        )
    }

}
