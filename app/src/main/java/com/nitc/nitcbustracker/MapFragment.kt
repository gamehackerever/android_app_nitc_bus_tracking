package com.nitc.nitcbustracker

import RetrofitClient
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
import com.nitc.nitcbustracker.data.model.BusLocation
import com.nitc.nitcbustracker.data.model.Stop
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MapFragment : Fragment(), OnMapReadyCallback {

    private var mapFragment: SupportMapFragment? = null
    private lateinit var map: GoogleMap
    private val markerMap = mutableMapOf<String, Marker>()
    private val updateInterval = 4000L
    private var updateJob: Job? = null
    private var lastBusDetails: Map<String, Bus> = emptyMap()
    private var lastLocations: List<BusLocation> = emptyList()
    private var hasZoomedToMarkers = false
    private var stops: List<Stop> = emptyList()


    private var busIdText: TextView? = null
    private var statusText: TextView? = null
    private var stopText: TextView? = null
    private var nextStopText: TextView? = null
    private var etaText: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_map, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        busIdText = view.findViewById(R.id.title)
        statusText = view.findViewById(R.id.status_text_view)
        stopText = view.findViewById(R.id.current_stop_text)
        nextStopText = view.findViewById(R.id.next_stop_text)
        etaText = view.findViewById(R.id.eta_text_view)

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

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(11.25, 75.78), 12f))

        map.setOnMarkerClickListener { marker ->
            val busId = marker.title?.substringAfter("Bus ID: ")?.trim()
            val bus = lastBusDetails[busId]
            val location = lastLocations.find { it.bus_id == busId }
            if (bus != null && location != null) {
                updateInfoCard(location, bus)
            }
            false
        }

        resetInfoCard()
        map.setOnMapClickListener {
            resetInfoCard()
        }
    }

    private fun startRepeatingUpdates() {
        updateJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                stops = RetrofitClient.api.getStops()
            } catch (e: Exception) {
                Log.e("MapFragment", "Error fetching stops", e)
            }
            while (isActive) {
                try {
                    val locationsResponse = RetrofitClient.api.getLocations()
                    if (locationsResponse.isSuccessful) {
                        val rawLocations = locationsResponse.body() ?: emptyList()

                        lastLocations = rawLocations.distinctBy { it.bus_id }

                        // Fetch bus details in parallel as before
                        val busDetailsList = lastLocations.map { location ->
                            async {
                                try {
                                    val response = RetrofitClient.api.getBusDetails(location.bus_id)
                                    if (response.isSuccessful) response.body() else null
                                } catch (e: Exception) {
                                    Log.e("MapFragment", "Error fetching bus ${location.bus_id}", e)
                                    null
                                }
                            }
                        }.awaitAll().filterNotNull()

                        lastBusDetails = busDetailsList.associateBy { it.bus_id }

                        updateBusMarkers(lastLocations)
                    }
                } catch (e: Exception) {
                    Log.e("MapFragment", "Failed to fetch bus locations", e)
                }
                delay(updateInterval)
            }
        }
    }

    private fun updateBusMarkers(locations: List<BusLocation>) {
        // Remove markers for buses not present anymore
        val currentBusIds = locations.map { it.bus_id }.toSet()
        markerMap.keys.filterNot { currentBusIds.contains(it) }.forEach { removedId ->
            markerMap[removedId]?.remove()
            markerMap.remove(removedId)
        }

        // Add/update markers for current buses
        locations.forEach { location ->
            val position = LatLng(location.latitude, location.longitude)
            val marker = markerMap[location.bus_id]
            if (marker == null) {
                map.addMarker(
                    MarkerOptions().position(position).title("Bus ID: ${location.bus_id}")
                )?.let { markerMap[location.bus_id] = it
                    Log.d("BusDebug", "Bus ${location.bus_id}: Lat=${location.latitude}, Lng=${location.longitude}" )}
            } else {
                marker.position = position
            }
        }

        // --- Auto-zoom to fit all markers only ONCE ---
        if (!hasZoomedToMarkers && markerMap.isNotEmpty()) {
            val builder = com.google.android.gms.maps.model.LatLngBounds.Builder()
            markerMap.values.forEach { marker ->
                builder.include(marker.position)
            }
            val bounds = builder.build()
            val padding = 100 // pixels, adjust as needed
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
            hasZoomedToMarkers = true
        }
    }


    private fun updateInfoCard(location: BusLocation, bus: Bus) {
        val nearestStop = findNearestStop(location.latitude, location.longitude)
        if (nearestStop != null) {
            val dist = distanceBetween(
                location.latitude, location.longitude,
                nearestStop.lattitude.toDouble(), nearestStop.longitude.toDouble()
            )
            Log.d(
                "BusDebug",
                "Bus ${bus.bus_id}: Lat=${location.latitude}, Lng=${location.longitude} | Nearest stop: ${nearestStop.name} (${nearestStop.lattitude}, ${nearestStop.longitude}) at ${"%.2f".format(dist)} km"
            )
        } else {
            Log.d(
                "BusDebug",
                "Bus ${bus.bus_id}: Lat=${location.latitude}, Lng=${location.longitude} | Nearest stop: NONE"
            )
        }

        busIdText?.text = "Bus ${bus.bus_id}"
        statusText?.text = "Near: ${nearestStop?.name ?: "Unknown"}"
        stopText?.text = "License: ${bus.license_number}"
        nextStopText?.text = "Capacity: ${bus.capacity}"
        etaText?.text = "${calculateMockEta(location)} mins"
    }


    private fun resetInfoCard() {
        busIdText?.text = "Tracking"
        etaText?.text = "--"
        stopText?.text = "License Number: --"
        nextStopText?.text = "Capacity: --"
        statusText?.text = "--"
    }

    private fun findNearestStop(lat: Double, lng: Double): Stop? {
        if (stops.isEmpty()) {
            Log.d("BusDebug", "No stops loaded")
            return null
        }
        stops.forEach { stop ->
            val dist = distanceBetween(lat, lng, stop.lattitude.toDouble(), stop.longitude.toDouble())
            Log.d("BusDebug", "Dist to ${stop.name}: $dist km")
        }
        return stops.minByOrNull { stop ->
            distanceBetween(lat, lng, stop.lattitude.toDouble(), stop.longitude.toDouble())
        }
    }


    private fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371 // kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat/2) * sin(dLat/2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon/2) * sin(dLon/2)
        val c = 2 * atan2(sqrt(a), sqrt(1-a))
        return earthRadius * c
    }

    private fun calculateMockEta(location: BusLocation): Int =
        (5..15).random() // Replace with real ETA logic if available

    override fun onDestroyView() {
        super.onDestroyView()
        updateJob?.cancel()
    }
}
