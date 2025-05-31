package com.nitc.nitcbustracker.data.model

data class LoginResponse(
    val success: Boolean,
    val message: String?,
    val role: String?,
    val busId: String?
)
