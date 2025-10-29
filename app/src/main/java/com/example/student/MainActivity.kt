package com.example.attendance_student


import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor


class MainActivity : AppCompatActivity() {


    // Views
    private lateinit var btnAttendance: Button
    private lateinit var etStudentId: EditText
    private lateinit var etStudentName: EditText
    private lateinit var timerText: TextView


    // Location & Biometric
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var countDownTimer: CountDownTimer? = null
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo


    // Attendance
    private var pendingStudentId = ""
    private var pendingStudentName = ""
    private val validStudents = mapOf(
        "20818" to "우승민",
        "20819" to "이승준",
        "20820" to "이현서"
    )


    private val schoolLat = 35.526255
    private val schoolLng = 129.324446
    private val allowedRadius = 200.0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        // View binding
        btnAttendance = findViewById(R.id.btnAttendance)
        etStudentId = findViewById(R.id.etStudentId)
        etStudentName = findViewById(R.id.etStudentName)
        timerText = findViewById(R.id.timerText)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        // Biometric
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt =
            BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext, "지문 인증 성공!", Toast.LENGTH_SHORT).show()
                    sendAttendanceRequestToServer(pendingStudentId, pendingStudentName)
                }


                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "인증 오류: $errString", Toast.LENGTH_SHORT)
                        .show()
                }


                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "지문 인증 실패", Toast.LENGTH_SHORT).show()
                }
            })


        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("출석 확인")
            .setSubtitle("지문으로 본인 확인을 진행하세요")
            .setNegativeButtonText("취소")
            .build()


        btnAttendance.setOnClickListener { checkLocationAndRequestAttendance() }


        findViewById<Button>(R.id.checkNameButton)?.setOnClickListener {
            val id = etStudentId.text.toString().trim()
            val name = etStudentName.text.toString().trim()
            if (validStudents[id] == name) {
                Toast.makeText(this, "등록된 학생입니다.", Toast.LENGTH_SHORT).show()
                pendingStudentId = id
                pendingStudentName = name
                btnAttendance.isEnabled = true   // ✅ 버튼 활성화
                startCountdown()
            } else {
                Toast.makeText(this, "잘못된 이름 또는 학번입니다.", Toast.LENGTH_SHORT).show()
                btnAttendance.isEnabled = false  // ✅ 버튼 비활성화
            }
        }


    }


    private fun checkLocationAndRequestAttendance() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            return
        }


        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val distance = calculateDistance(it.latitude, it.longitude, schoolLat, schoolLng)
                if (distance <= allowedRadius) {
                    val biometricManager = BiometricManager.from(this)
                    if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                        == BiometricManager.BIOMETRIC_SUCCESS
                    ) {
                        biometricPrompt.authenticate(promptInfo)
                    } else {
                        Toast.makeText(this, "지문 인증을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "학교 반경 밖입니다.", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(this, "위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun startCountdown() {
        countDownTimer?.cancel()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }


        val remainingTime = calendar.timeInMillis - System.currentTimeMillis()
        if (remainingTime > 0) {
            countDownTimer = object : CountDownTimer(remainingTime, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val hours = millisUntilFinished / 1000 / 3600
                    val minutes = (millisUntilFinished / 1000 / 60) % 60
                    val seconds = (millisUntilFinished / 1000) % 60
                    timerText.text =
                        "미인정 지각까지 남은 시간: %02d:%02d:%02d".format(hours, minutes, seconds)
                }


                override fun onFinish() {
                    timerText.text = "출석 시간이 마감되었습니다."
                }
            }.start()
        } else {
            timerText.text = "출석 시간이 마감되었습니다."
        }
    }


    private fun sendAttendanceRequestToServer(studentId: String, name: String) {
        val currentTime =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val request = AttendanceRequest(studentId, name, currentTime)
        val api = RetrofitClient.attendanceInstance.create(AttendanceApi::class.java)
        api.requestAttendance(request).enqueue(object : retrofit2.Callback<AttendanceResponse> {
            override fun onResponse(
                call: retrofit2.Call<AttendanceResponse>,
                response: retrofit2.Response<AttendanceResponse>
            ) {
                Toast.makeText(
                    this@MainActivity,
                    response.body()?.message ?: "서버 응답 없음",
                    Toast.LENGTH_SHORT
                ).show()
            }


            override fun onFailure(call: retrofit2.Call<AttendanceResponse>, t: Throwable) {
                Toast.makeText(
                    this@MainActivity,
                    "서버 연결 실패: ${t.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }


    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }
}