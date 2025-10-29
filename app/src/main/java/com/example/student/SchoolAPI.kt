package com.example.attendance_student

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

// -----------------------------
// 데이터 클래스
// -----------------------------
// --- 급식 ---
data class MealServiceDietInfoResponse(
    val mealServiceDietInfo: List<MealServiceDietInfoItem>?
)

data class MealServiceDietInfoItem(
    val head: List<Any>?,
    val row: List<MealRow>?
)

data class MealRow(
    val MLSV_YMD: String?, // 급식 날짜
    val DDISH_NM: String? // 메뉴
)

// --- 시간표 ---
data class TimeTableResponse(
    val timeTable: List<TimeTableWrapper>?
)

data class TimeTableWrapper(
    val head: List<Any>?,
    val row: List<TimeTableInfo>?
)

data class TimeTableInfo(
    val ALL_TI_YMD: String?, // 날짜
    val ITRT_CNTNT: String?, // 수업 내용
    val PERIO: String?, // 교시
    val ITRT_SUBJ_NM: String?, // 과목명
    val ATPT_OFCDC_SC_CODE: String?,
    val SD_SCHUL_CODE: String?
)

// -----------------------------
// Retrofit API
// -----------------------------
interface SchoolApi {
    // --- 급식 ---
    @GET("mealServiceDietInfo")
    fun getMeals(
        @Query("KEY") mealApiKey: String = "39d0627e75f74f6f9e0f4624bafd1a34", // 급식 API 키 적용
        @Query("Type") type: String = "json",
        @Query("pIndex") pIndex: Int = 1,
        @Query("pSize") pSize: Int = 100,
        @Query("ATPT_OFCDC_SC_CODE") atptCode: String,
        @Query("SD_SCHUL_CODE") schoolCode: String,
        @Query("MLSV_YMD") date: String // YYYYMMDD
    ): Call<MealServiceDietInfoResponse>

    // --- 시간표 ---
    @GET("hisTimetable")
    fun getSchedule(
        @Query("KEY") scheduleApiKey: String = "4cd36fb52f0745fbaa08b517f7226cd8", // 시간표 API 키 적용
        @Query("Type") type: String = "json",
        @Query("pIndex") pIndex: Int = 1,
        @Query("pSize") pSize: Int = 100,
        @Query("ATPT_OFCDC_SC_CODE") atptCode: String,
        @Query("SD_SCHUL_CODE") schoolCode: String,
        @Query("ALL_TI_YMD") date: String
    ): Call<TimeTableResponse>
}