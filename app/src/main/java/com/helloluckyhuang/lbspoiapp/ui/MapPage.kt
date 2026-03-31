package com.helloluckyhuang.lbspoiapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.helloluckyhuang.lbspoiapp.ui.map.MapCard
import com.helloluckyhuang.lbspoiapp.ui.viewmodel.PoiProjectViewModel

@Composable
fun MapPage(viewModel: PoiProjectViewModel, projectUid: Int, onNavigateToHomePage: () -> Unit) {
    Column {
        Text("$projectUid")
        Button(onClick = {
            onNavigateToHomePage()
        }) {
            Text("返回首页")
        }
        MapCard()
    }
}