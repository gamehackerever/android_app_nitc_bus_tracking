package com.nitc.nitcbustracker

import RetrofitClient.api
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.button.MaterialButton
import com.nitc.nitcbustracker.data.model.Location
import jp.wasabeef.blurry.Blurry
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DriverActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var locationUpdatesStarted = false
    private lateinit var selectedHostel: String

    // Map-related variables
    private lateinit var mapView: MapView
    private lateinit var blurredMap: ImageView
    private var googleMap: GoogleMap? = null

    companion object {
        private const val MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver)

        // Initialize MapView and blurred overlay
        mapView = findViewById(R.id.mapView)
        blurredMap = findViewById(R.id.blurredMap)

        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY)
        }
        mapView.onCreate(mapViewBundle)
        mapView.getMapAsync(this)

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val enableToggle = findViewById<MaterialButton>(R.id.btnEnable)
        val disableToggle = findViewById<MaterialButton>(R.id.btnDisable)
        val spinner = findViewById<Spinner>(R.id.spinnerBuses)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedHostel = parent.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedHostel = ""
            }
        }

        enableToggle.setOnClickListener {
            if (::selectedHostel.isInitialized && selectedHostel.isNotEmpty()) {
                startLocationUpdates()
            } else {
                Toast.makeText(
                    this,
                    "Select a hostel before enabling tracking",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        disableToggle.setOnClickListener {
            stopLocationUpdates()
        }

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                val loc = Location(
                    bus_id = selectedHostel,
                    latitude = location.latitude,
                    longitude = location.longitude
                )
                api.sendLocation(loc).enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        Toast.makeText(this@DriverActivity, "Location sent", Toast.LENGTH_SHORT).show()
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Toast.makeText(this@DriverActivity, "Failed to send location", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Center the map on NITC
        val nitc = LatLng(11.3186, 75.9344)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(nitc, 16f))

        // Wait for camera to settle, then take a snapshot and blur it
        map.setOnCameraIdleListener {
            captureAndBlurSnapshot()
            // Remove listener so it doesn't repeat
            map.setOnCameraIdleListener(null)
        }
    }

    private fun captureAndBlurSnapshot() {
        googleMap?.snapshot { bitmap ->
            Blurry.with(this)
                .radius(15)
                .from(bitmap)
                .into(blurredMap)
            // Hide the MapView, show only the blurred image
            blurredMap.visibility = View.VISIBLE
            mapView.visibility = View.GONE
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
            return
        }
        if (!locationUpdatesStarted) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
            locationUpdatesStarted = true
        }
    }

    private fun stopLocationUpdates() {
        if (locationUpdatesStarted) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            locationUpdatesStarted = false
        }
    }

    // MapView lifecycle management
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        var mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle)
        }
        mapView.onSaveInstanceState(mapViewBundle)
    }
}
