package com.example.attendance_student

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GeminiApi {
    @POST("v1beta/models/gemini-pro:generateContent")
    fun sendQuery(
        @Header("Authorization") apiKey: String,
        @Body request: GeminiRequest
    ): Call<GeminiResponse>
}

data class GeminiRequest(
    val contents: List<Content>
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String
)

data class GeminiResponse(
    val candidates: List<Candidate>?,
    val error: GeminiError?
)

data class Candidate(
    val content: Content
)

data class GeminiError(
    val message: String
)
