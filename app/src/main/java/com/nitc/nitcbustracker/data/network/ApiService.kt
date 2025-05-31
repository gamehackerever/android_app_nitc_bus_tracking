package com.nitc.nitcbustracker.data.network

import com.nitc.nitcbustracker.data.model.Bus
import com.nitc.nitcbustracker.data.model.BusStatus
import com.nitc.nitcbustracker.data.model.GenericResponse
import com.nitc.nitcbustracker.data.model.Location
import com.nitc.nitcbustracker.data.model.LoginRequest
import com.nitc.nitcbustracker.data.model.LoginResponse
import com.nitc.nitcbustracker.data.model.Notice
import com.nitc.nitcbustracker.data.model.RegisterRequest
import com.nitc.nitcbustracker.data.model.UserInfo
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @GET("buses")
    suspend fun getLocations(): List<Bus>

    @POST("login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("location")
    fun sendLocation(@Body location: Location): Call<ResponseBody>

    @GET("bus-statuses") // Adjust to your actual endpoint
    suspend fun getBusStatuses(): Response<List<BusStatus>>

    @POST("/complete-registration")
    suspend fun completeRegistration(@Body request: RegisterRequest): Response<GenericResponse>

    @GET("user/exists")
    suspend fun checkUserExists(@Query("email") email: String): Response<Map<String, Boolean>>

    @POST("/partial-registration")
    suspend fun partialRegistration(@Body request: Map<String, String>): Response<Map<String, Any>>

    @GET("/user/getinfo")
    suspend fun getUserInfo(@Query("email") email: String): Response<UserInfo>

    @GET("/get/notices")
    suspend fun getNotices(): Response<List<Notice>>

    @POST("/update/notices")
    suspend fun updateNotices(@Body notices: Notice): Response<GenericResponse>

    @POST("/push-notification") // Replace with your backend endpoint path
    fun sendNotification(@Body request: Notice): Call<Void>

}
