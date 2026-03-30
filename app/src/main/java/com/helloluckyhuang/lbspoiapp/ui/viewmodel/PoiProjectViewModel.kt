package com.helloluckyhuang.lbspoiapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helloluckyhuang.lbspoiapp.data.local.PoiProjectData
import com.helloluckyhuang.lbspoiapp.data.repository.PoiProjectRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PoiProjectViewModel(
    private val repo: PoiProjectRepository
) : ViewModel() {
    val projects = repo.projects.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addProject(title: String) {
        viewModelScope.launch {
            repo.addProject(title)
        }
    }

    fun deleteProject(project: PoiProjectData) {
        viewModelScope.launch {
            repo.deleteProject(project)
        }
    }

    fun updateProject(project: PoiProjectData) {
        viewModelScope.launch {
            repo.update(project)
        }
    }
}