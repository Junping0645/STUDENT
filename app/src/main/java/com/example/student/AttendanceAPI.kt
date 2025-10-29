package com.example.attendance_student

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class AttendanceRequest(
    val studentId: String,
    val name: String,
    val time: String
)

data class AttendanceResponse(
    val message: String
)

interface AttendanceApi {
    @POST("attendance")
    fun requestAttendance(@Body request: AttendanceRequest): Call<AttendanceResponse>
}
