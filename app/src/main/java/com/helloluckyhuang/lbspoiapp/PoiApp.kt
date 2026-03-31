package com.helloluckyhuang.lbspoiapp

import android.app.Application
import kotlin.getValue
import androidx.room.Room
import com.helloluckyhuang.lbspoiapp.data.local.AppDatabase
import com.helloluckyhuang.lbspoiapp.data.repository.PoiProjectRepository

class PoiApp : Application() {
    // 单例数据库
    val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "app_database"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    val poiProjectRepository: PoiProjectRepository by lazy {
        PoiProjectRepository(database.poiProjectDao())
    }

    companion object {
        lateinit var instance: PoiApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
