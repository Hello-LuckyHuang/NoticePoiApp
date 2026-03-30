package com.helloluckyhuang.lbspoiapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PoiProjectDao {
    // 查询
    @Query("SELECT * FROM poi_project")
    fun getAllProjects(): Flow<List<PoiProjectData>>

    // 插入
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: PoiProjectData)

    // 删除
    @Delete
    suspend fun delete(project: PoiProjectData)

    // 更新
    @Update
    suspend fun update(project: PoiProjectData)
}