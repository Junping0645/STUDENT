package com.example.attendance_student

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://cmd ipconfig의 Ipv4주소 사용해주세요/" // 출석 API 엔드포인트 (필요 시 변경)
    private const val NEIS_BASE_URL = "https://open.neis.go.kr/hub/"
    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"

    val attendanceInstance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val schoolInstance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(NEIS_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val geminiInstance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}