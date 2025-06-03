package com.nitc.nitcbustracker.data.model

data class Bus(
    val bus_id: String,
    val license_number: String,
    val capacity: Int,
    val isRunning: Boolean
)
