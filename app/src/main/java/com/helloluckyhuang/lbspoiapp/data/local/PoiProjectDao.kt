package com.helloluckyhuang.lbspoiapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PoiProjectDao {
    // 查询
    @Query("SELECT * FROM poi_project ORDER BY createTime DESC, uid DESC")
    fun getAllProjects(): Flow<List<PoiProjectData>>

    // 插入
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: PoiProjectData): Long

    // 删除
    @Delete
    suspend fun delete(project: PoiProjectData)

    // 更新
    @Update
    suspend fun update(project: PoiProjectData)

    // 查询
    @Query("SELECT * FROM poi_project WHERE uid = :uid LIMIT 1")
    suspend fun getProjectById(uid: Int): PoiProjectData?
}
