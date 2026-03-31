package com.helloluckyhuang.lbspoiapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "poi_project")
data class PoiProjectData(
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
    var title: String
)