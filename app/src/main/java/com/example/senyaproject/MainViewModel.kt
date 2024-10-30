package com.example.senyaproject

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition

class MainViewModel: ViewModel() {

    private val _state = mutableStateOf(MainState())
    val state: State<MainState> = _state



    val points: List<Point> = listOf(
        Point(51.765273, 55.124219),
        Point(51.765316, 55.124147),
        Point(51.765338, 55.124113),
        Point(51.765396, 55.124275),
        Point(51.765383, 55.124286)
    )



    val okeiBorders = mapOf(
        "bl" to Point(51.765072, 55.123865),
        "br" to Point(51.765370, 55.124666),
        "tl" to Point(51.765252, 55.123696),
        "tr" to Point(51.765543, 55.124501),
    )





    fun redactCurrentLocation(location: Point){
        _state.value = state.value.copy(
            currentPosition = location
        )

        Log.i("userLocation", "redact: ${location.latitude} ${location.longitude}")

    }


    fun redactPositionState(isIndoor: Boolean){
        _state.value = state.value.copy(
            isIndoor = isIndoor
        )
    }


    fun redactCameraPosition(position: CameraPosition){
        _state.value = state.value.copy(
            positionSettings =  PositionSettings(
                zoom = position.zoom,
                azimuth = position.azimuth,
                tilt = position.tilt
            )

        )
    }




}