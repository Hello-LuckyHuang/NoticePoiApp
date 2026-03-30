package com.helloluckyhuang.lbspoiapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.helloluckyhuang.lbspoiapp.ui.HomePage
import com.helloluckyhuang.lbspoiapp.ui.theme.LBSPOIAppTheme
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
                    HomePage(modifier = Modifier.padding(innerPadding), viewModel = viewModel)
                }
            }
        }
    }
}