package com.nitc.nitcbustracker.data.model

data class BusStatus(
    val bus_id: String,
    val current_stop_name: String,
    val eta: Int
)
