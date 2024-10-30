package com.example.senyaproject

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class UserLocationManager: LocationListener {

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation


    @SuppressLint("MissingPermission")
    fun SetUpLocationListener(context: Context) // это нужно запустить в самом начале работы программы
    {
        val locationManager: LocationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val locationListener: LocationListener = UserLocationManager()
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            2000,
            1f,
            locationListener
        ) // здесь можно указать другие более подходящие вам параметры
        _currentLocation.value = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    }

    override fun onLocationChanged(location: Location) {
        _currentLocation.value = location
        Log.i("userLocation", "${currentLocation.value?.latitude} ${currentLocation.value?.longitude}")
    }
}