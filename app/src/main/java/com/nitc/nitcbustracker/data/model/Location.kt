package com.nitc.nitcbustracker.data.model

import java.sql.Timestamp

data class Location(
    val bus_id: String,
    val latitude: Double,
    val longitude: Double
)
