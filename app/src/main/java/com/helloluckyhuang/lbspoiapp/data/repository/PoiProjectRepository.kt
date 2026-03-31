package com.helloluckyhuang.lbspoiapp.data.repository

import com.helloluckyhuang.lbspoiapp.data.local.PoiProjectDao
import com.helloluckyhuang.lbspoiapp.data.local.PoiProjectData
import java.util.UUID

class PoiProjectRepository(
    private val projectDao: PoiProjectDao
) {
    val projects = projectDao.getAllProjects()

    suspend fun addProject(title: String): Int {
        return projectDao.insert(PoiProjectData(title = title)).toInt()
    }

    suspend fun deleteProject(project: PoiProjectData) {
        projectDao.delete(project)
    }

    suspend fun update(project: PoiProjectData) {
        projectDao.update(project)
    }

    suspend fun getProjectById(uid: Int): PoiProjectData? {
        return projectDao.getProjectById(uid)
    }
}