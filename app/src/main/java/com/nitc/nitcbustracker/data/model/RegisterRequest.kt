package com.nitc.nitcbustracker.data.model

data class RegisterRequest(
    val name: String,
    val username: String,
    val password: String,
    val role: String,
    val hostel: String,
    val email: String,
    val phone: String,
)