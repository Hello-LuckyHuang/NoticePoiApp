package com.helloluckyhuang.lbspoiapp.data.local

data class PoiList (
    val pois: MutableList<PoiData> = mutableListOf()
)

data class PoiData (
    val latitude: Double,
    val longitude: Double
)
