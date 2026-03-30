package com.helloluckyhuang.lbspoiapp.data.repository

import com.helloluckyhuang.lbspoiapp.data.local.PoiProjectDao
import com.helloluckyhuang.lbspoiapp.data.local.PoiProjectData

class PoiProjectRepository(
    private val projectDao: PoiProjectDao
) {
    val projects = projectDao.getAllProjects()

    suspend fun addProject(title: String) {
        projectDao.insert(PoiProjectData(title = title))
    }

    suspend fun deleteProject(project: PoiProjectData) {
        projectDao.delete(project)
    }

    suspend fun update(project: PoiProjectData) {
        projectDao.update(project)
    }
}