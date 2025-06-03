package com.nitc.nitcbustracker.data.network

import com.nitc.nitcbustracker.data.model.Bus
import com.nitc.nitcbustracker.data.model.BusStatus
import com.nitc.nitcbustracker.data.model.GenericResponse
import com.nitc.nitcbustracker.data.model.BusLocation
import com.nitc.nitcbustracker.data.model.LoginRequest
import com.nitc.nitcbustracker.data.model.LoginResponse
import com.nitc.nitcbustracker.data.model.Notice
import com.nitc.nitcbustracker.data.model.RegisterRequest
import com.nitc.nitcbustracker.data.model.Stop
import com.nitc.nitcbustracker.data.model.UserInfo
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @GET("/api/stops/get")
    suspend fun getStops(): List<Stop>

    @GET("/api/location/get-buses")
    suspend fun getLocations(): Response<List<BusLocation>>

    @GET("/api/bus/get")
    suspend fun getBusDetails(@Query("busId") busId: String): Response<Bus>

    @POST("/api/user/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("/api/location/post")
    fun sendLocation(@Body busLocation: BusLocation): Call<ResponseBody>

    @GET("/api/bus-statuses")
    suspend fun getBusStatuses(): Response<List<BusStatus>>

    @POST("/api/user/complete-registration")
    suspend fun completeRegistration(@Body request: RegisterRequest): Response<GenericResponse>

    @GET("/api/user/exists")
    suspend fun checkUserExists(@Query("email") email: String): Response<Map<String, Boolean>>

    @POST("/api/user/partial-registration")
    suspend fun partialRegistration(@Body request: Map<String, String>): Response<Map<String, Any>>

    @GET("/api/user/getinfo")
    suspend fun getUserInfo(@Query("email") email: String): Response<UserInfo>

    @GET("/api/notices/get")
    suspend fun getNotices(): Response<List<Notice>>

    @POST("/api/notices/post")
    suspend fun updateNotices(@Body notices: Notice): Response<GenericResponse>

    @POST("/api/notifications/student")
    fun sendNotificationStudent(@Body request: Notice): Call<Void>

    @POST("/api/notifications/driver")
    fun sendNotificationDriver(@Body request: Notice): Call<Void>

    @POST("/api/notifications/both")
    fun sendNotificationBoth(@Body request: Notice): Call<Void>
}
