package com.helloluckyhuang.lbspoiapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.helloluckyhuang.lbspoiapp.data.repository.PoiProjectRepository

class PoiProjectViewModelFactory(
    private val repo: PoiProjectRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PoiProjectViewModel::class.java)) {
            return PoiProjectViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}