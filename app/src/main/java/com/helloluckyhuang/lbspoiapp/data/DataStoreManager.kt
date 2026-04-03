package com.helloluckyhuang.lbspoiapp.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

object DataStoreManager {
    private val KEY_OPEN_FLOAT_FRAME_SWITCH = booleanPreferencesKey("open_float_frame")

    fun getSwitchFlow(context: Context): Flow<Boolean> {
        return context.dataStore.data.map {
            it[KEY_OPEN_FLOAT_FRAME_SWITCH] ?: false
        }
    }

    suspend fun setSwitch(context: Context, value: Boolean) {
        context.dataStore.edit {
            it[KEY_OPEN_FLOAT_FRAME_SWITCH] = value
        }
    }
}