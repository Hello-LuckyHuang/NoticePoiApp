package com.helloluckyhuang.lbspoiapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PoiProjectData::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun poiProjectDao(): PoiProjectDao
}