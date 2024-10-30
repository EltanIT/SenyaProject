package com.example.senyaproject

import com.yandex.mapkit.geometry.Point


data class MainState(
    val currentPosition: Point? = null,
    val positionSettings: PositionSettings = PositionSettings(),
    val isIndoor: Boolean = false
)