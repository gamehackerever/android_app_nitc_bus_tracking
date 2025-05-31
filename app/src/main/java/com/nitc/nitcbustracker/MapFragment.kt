package com.nitc.nitcbustracker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.nitc.nitcbustracker.data.model.Bus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MapFragment : Fragment(), OnMapReadyCallback {

    private var mapFragment: SupportMapFragment? = null
    private lateinit var map: GoogleMap
    private val markerMap = mutableMapOf<String, Marker>()
    private val updateInterval = 4000L
    private var updateJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Initialize map fragment only once
        mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance()
            childFragmentManager.beginTransaction()
                .replace(R.id.map, mapFragment!!)
                .commit()
        }
        mapFragment?.getMapAsync(this)
        startRepeatingUpdates()
    }

    override fun onResume() {
        super.onResume()
        // Re-attach map fragment when fragment becomes visible
        mapFragment?.let {
            childFragmentManager.beginTransaction()
                .show(it)
                .commit()
        }
    }

    override fun onPause() {
        super.onPause()
        // Detach map fragment when fragment becomes hidden
        mapFragment?.let {
            childFragmentManager.beginTransaction()
                .hide(it)
                .commit()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(11.25, 75.78), 12f))
    }

    private fun startRepeatingUpdates() {
        updateJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                try {
                    val buses = RetrofitClient.api.getLocations()
                    updateBusMarkers(buses)
                } catch (e: Exception) {
                    Log.e("MapFragment", "Failed to fetch buses", e)
                }
                delay(updateInterval)
            }
        }
    }


    private fun updateBusMarkers(buses: List<Bus>) {
        for (bus in buses) {
            val position = LatLng(bus.latitude, bus.longitude)
            val marker = markerMap[bus.busId]
            if (marker == null) {
                val newMarker = map.addMarker(
                    MarkerOptions().position(position).title("Bus ID: ${bus.busId}")
                )
                if (newMarker != null) markerMap[bus.busId] = newMarker
            } else {
                marker.position = position
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        updateJob?.cancel()
    }
}
