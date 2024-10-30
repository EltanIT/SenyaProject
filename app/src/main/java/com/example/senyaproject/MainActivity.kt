package com.example.senyaproject

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.PointF
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.senyaproject.ui.theme.SenyaProjectTheme
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingRouter
import com.yandex.mapkit.directions.driving.DrivingRouterType
import com.yandex.mapkit.directions.driving.DrivingSession
import com.yandex.mapkit.directions.driving.VehicleOptions
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.RotationType
import com.yandex.mapkit.map.TextStyle
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.image.ImageProvider


class MainActivity : ComponentActivity(), LocationListener {


    private val START_LOCATION = Point(51.765273, 55.124219)
    private val END_LOCATION = Point(51.765338, 55.124113)

    private var mapObjects: MapObjectCollection? = null
    private var drivingRouter: DrivingRouter? = null
    private var drivingSession: DrivingSession? = null

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.setApiKey("08bfcad4-f93e-4bd0-902f-8e210fa902eb")


        setContent {
            viewModel = viewModel()

            LaunchedEffect(true) {
                initLocationListener(this@MainActivity)
            }


            SenyaProjectTheme {
                requestLocationPermission()
                MapRoutes(viewModel)
            }
        }
    }


    @Composable
    fun MapRoutes(viewModel: MainViewModel) {
        val state = viewModel.state.value

        var map by remember{
            mutableStateOf<MapView?>(null)
        }


        LaunchedEffect(key1 = state.currentPosition) {

            Log.i("userLocation", "changeCamera: ${state.currentPosition}")
            map?.let { map ->
//                map.map.move(
//                    CameraPosition(
//                        state.currentPosition ?: START_LOCATION,
//                        state.positionSettings.zoom,
//                        state.positionSettings.azimuth,
//                        state.positionSettings.tilt,
//                    ),
//                    Animation(Animation.Type.SMOOTH, 0.4f),
//                    null
//                )

                drivingRouter = DirectionsFactory.getInstance().createDrivingRouter(
                    DrivingRouterType.COMBINED)
                mapObjects = map.map.mapObjects.addCollection()
                submitRequests(state.currentPosition)
            }
        }


        LaunchedEffect(key1 = state.isIndoor) {
            if (state.isIndoor){ // УБРАТЬ ВОСКЛИЦАТЕЛЬНЫЙ ЗНАК!!!!
                val polyline = Polyline(viewModel.points)
                map?.let { map ->
                    val polylineMapObject = map.map.mapObjects.addPolyline(polyline)

                    polylineMapObject.apply {
                        strokeWidth = 2.5f
                        setStrokeColor(ContextCompat.getColor(this@MainActivity, R.color.purple_700))
                        outlineWidth = 1f
                    }

                    map.map.mapObjects.addPlacemark().apply {
                        geometry = viewModel.points.lastOrNull()!!
                        setIcon(ImageProvider.fromResource(this@MainActivity, R.drawable.location))
                        setText(
                            "Конечная точка!",
                            TextStyle().apply {
                                size = 10f
                                placement = TextStyle.Placement.RIGHT
                                offset = 5f
                            },
                        )
                    }
                }

            }else{
                map?.let { map ->
                    drivingRouter = DirectionsFactory.getInstance().createDrivingRouter(
                        DrivingRouterType.COMBINED)
                    mapObjects = map.map.mapObjects.addCollection()
                    submitRequests(state.currentPosition)
                }
            }
        }


        AndroidView(factory = { MapView(it) }, Modifier.fillMaxSize()){
            map = it
            map?.let { map->

                map.map.move(
                    CameraPosition(
                        state.currentPosition ?: START_LOCATION,
                        18f,
                        0f,
                        0f,
                    ),
                    Animation(Animation.Type.SMOOTH, 2f),
                    null
                )

                map.map.addCameraListener { map, position, updateReason, p3 ->
                    //                        viewModel.redactCameraPosition(position)
                }

            }


        }


        LaunchedEffect(key1 = true) {
            snapshotFlow { map }.collect{
                it?.let {
                    MapKitFactory.initialize(this@MainActivity)
                    MapKitFactory.getInstance().onStart()
                    it.onStart()


                    val mapKit = MapKitFactory.getInstance()
                    val locationMapKit: UserLocationLayer = mapKit.createUserLocationLayer(map!!.mapWindow)

                    locationMapKit.apply {
                        isVisible = true
                    }

                    locationMapKit.setObjectListener(object : UserLocationObjectListener{
                        override fun onObjectAdded(userLocationView: UserLocationView) {
                            Log.i("userLocationIcon", "added")
                            userLocationView.arrow.setIcon(
                                ImageProvider.fromResource(this@MainActivity, R.drawable.location),
                                IconStyle().setAnchor(PointF(0f, 0f))
                                    .setRotationType(RotationType.ROTATE)
                                    .setZIndex(0f)
                                    .setScale(1f)
                            )

                            userLocationView.accuracyCircle.isVisible = false
                        }

                        override fun onObjectRemoved(userLocationView: UserLocationView) {
                            Log.i("userLocationIcon", "removed")
                            userLocationView.accuracyCircle.isVisible = false
                        }

                        override fun onObjectUpdated(p0: UserLocationView, p1: ObjectEvent) {
                            Log.i("userLocationIcon", "update")
                        }

                    })
                }
            }
        }


    }

    private fun submitRequests(currentLocation: Point?) {
        val drivingOptions = DrivingOptions().apply {
            routesCount = 1
        }

        val vehicleOptions = VehicleOptions()


        Log.i("userLocationRoute", "${currentLocation}")
        val points = buildList {
            add(RequestPoint((currentLocation ?: START_LOCATION), RequestPointType.WAYPOINT, null, null))
            add(RequestPoint(END_LOCATION, RequestPointType.WAYPOINT, null, null))
        }

        val drivingRouteListener = object : DrivingSession.DrivingRouteListener {
            override fun onDrivingRoutes(drivingRoutes: MutableList<DrivingRoute>) {
                mapObjects?.clear()
                for (route in drivingRoutes){
                    mapObjects?.addPolyline(route.geometry)
                }

            }

            override fun onDrivingRoutesError(error: com.yandex.runtime.Error) {
                Log.e("routesError", error.toString())
            }
        }

        drivingSession = drivingRouter?.requestRoutes(
            points,
            drivingOptions,
            vehicleOptions,
            drivingRouteListener
        )
    }


    @Composable
    private fun requestLocationPermission(){
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission Accepted: Do something
                Log.d("ExampleScreen","PERMISSION GRANTED")

            } else {
                // Permission Denied: Do something
                Log.d("ExampleScreen","PERMISSION DENIED")
            }
        }

        LaunchedEffect(true) {
            if (ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            ){
                launcher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }

            if (ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
            ){
                launcher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
    }



    @SuppressLint("MissingPermission")
    fun checkIfUserInBuilding(position: Point) {
        if (position.longitude >= viewModel.okeiBorders["bl"]?.longitude?:0.0 &&
            position.longitude <= viewModel.okeiBorders["tr"]?.longitude?:0.0){
            if (position.latitude >= viewModel.okeiBorders["bl"]?.latitude?:0.0 &&
                position.latitude <= viewModel.okeiBorders["tr"]?.latitude?:0.0
                ){
                viewModel.redactPositionState(true)
                Log.i("userLocation", "Пользователь в здании")
                return
            }
        }

        viewModel.redactPositionState(false)
        Log.i("userLocation", "Пользователь не в здании")
    }



    @SuppressLint("MissingPermission")
    fun initLocationListener(context: Context){
        val locationManager: LocationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val locationListener: LocationListener = this
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            2000,
            1f,
            locationListener
        ) // здесь можно указать другие более подходящие вам параметры
        val lastPos = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        Log.i("userLocation", "first init: ${viewModel} ${lastPos}")
        lastPos?.let {
            viewModel?.redactCurrentLocation(Point(lastPos.latitude, lastPos.longitude))
        }
    }

    override fun onLocationChanged(loc: Location) {
        val point = Point(loc.latitude, loc.longitude)
        Log.i("userLocation", "locationChanged: ${viewModel} ${loc}")
        viewModel?.redactCurrentLocation(point)
        checkIfUserInBuilding(point)
    }







}
