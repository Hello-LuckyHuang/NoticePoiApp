package com.helloluckyhuang.lbspoiapp

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import kotlin.getValue
import androidx.room.Room
import com.amap.api.maps.MapsInitializer
import com.helloluckyhuang.lbspoiapp.data.local.AppDatabase
import com.helloluckyhuang.lbspoiapp.data.repository.LocationRepository
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
        lateinit var locationRepo: LocationRepository

        // 是否开启定位跟踪
        var trackingEnabled: Boolean = false
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        MapsInitializer.updatePrivacyShow(instance, true, true)
        MapsInitializer.updatePrivacyAgree(instance, true)

        locationRepo = LocationRepository(this)

        ProcessLifecycleOwner.get().lifecycle.addObserver(
            AppLifecycleObserver()
        )
    }
}
