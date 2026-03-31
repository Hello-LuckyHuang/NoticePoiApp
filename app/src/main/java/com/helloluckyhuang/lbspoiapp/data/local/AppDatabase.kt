package com.helloluckyhuang.lbspoiapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [PoiProjectData::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun poiProjectDao(): PoiProjectDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE poi_project ADD COLUMN dataJson TEXT NOT NULL DEFAULT '{}'")
            }
        }
    }
}
