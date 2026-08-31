# 대현고 스마트 출석 시스템 (STUDENT)

위치 기반 인증과 지문(생체) 인증을 결합한 **학생 자율 출석 체크 안드로이드 앱**입니다.
학교 반경 안에 있을 때만, 본인 지문으로 인증해야 출석이 처리되므로 대리 출석을 막을 수 있습니다.
여기에 NEIS 급식·시간표 조회와 Gemini AI 질의 기능이 함께 들어 있습니다.

> `app_name`: **대현고 스마트 출석 시스템** · 패키지: `com.example.attendance_student`

---

## 주요 기능

### 1. 위치 + 지문 기반 출석
- **학생 정보 확인**: 학번·이름을 입력하면 앱에 등록된 학생 목록과 대조해 유효성을 검사합니다. 일치할 때만 `출석하기` 버튼이 활성화됩니다.
- **위치 검증(GPS)**: `FusedLocationProviderClient`로 현재 위치를 받아, 학교 좌표(위도 `35.526255`, 경도 `129.324446`)와의 거리를 **하버사인(Haversine) 공식**으로 계산합니다. 반경 **200m** 이내일 때만 다음 단계로 진행합니다.
- **지문 인증**: 위치 조건을 통과하면 `BiometricPrompt`로 지문 본인 확인을 요청합니다. 인증 성공 시에만 출석 요청을 서버로 전송합니다.
- **서버 전송**: 인증이 끝나면 학번·이름·타임스탬프(`yyyy-MM-dd HH:mm:ss`)를 FastAPI 서버로 POST 합니다.

### 2. 지각 카운트다운 타이머
- 정보 확인이 완료되면 그날 **23:59까지 남은 시간**을 `HH:MM:SS` 형태로 실시간 표시합니다.
- 시간이 지나면 "출석 시간이 마감되었습니다" 문구로 바뀝니다.

### 3. 급식 조회 (NEIS)
- NEIS(교육정보 개방 포털) `mealServiceDietInfo` API를 호출해 특정 날짜(`YYYYMMDD`)의 급식 메뉴를 가져옵니다.

### 4. 시간표 조회 (NEIS)
- NEIS `hisTimetable`(고등학교 시간표) API로 교시·과목·수업 내용을 조회합니다.

### 5. Gemini AI 질의
- Google `gemini-pro` 모델의 `generateContent`를 호출해, 사용자가 입력한 질문에 대한 AI 응답을 받아 화면에 표시합니다.
- 우측 네비게이션 드로어에서 `Gemini와 대화하기`, `급식 보기`, `시간표 보기` 메뉴로 이동할 수 있습니다.

### 6. 설정 화면
- 앱 버전(1.0) 등 기본 정보를 보여주는 `SettingsActivity`.

---

## 화면 구성
- **메인 화면(`MainActivity`)**: 타이틀, 지각 카운트다운, 학생 정보 입력 카드, 출석 버튼, 급식·시간표 섹션, Gemini 질의 카드, 우측 네비게이션 드로어.
- **설정 화면(`SettingsActivity`)**: 앱 버전 정보.

---

## FastAPI 백엔드 (기능 설명)

> 출석 데이터를 받는 FastAPI 서버는 이 저장소에 포함되어 있지 않습니다. 아래는 앱이 기대하는 **서버의 동작 명세**입니다.

앱은 출석 인증이 끝나면 다음과 같이 서버에 요청합니다.

**엔드포인트**
```
POST /attendance
Content-Type: application/json
```

**요청 본문**
```json
{
  "studentId": "20818",
  "name": "우승민",
  "time": "2026-08-31 09:12:34"
}
```

**응답 본문**
```json
{
  "message": "출석이 완료되었습니다."
}
```

**서버가 담당해야 하는 역할**
- 전달받은 학번·이름·시각을 검증하고 출석 기록으로 저장(DB/파일 등).
- 처리 결과를 `message` 필드에 담아 반환 → 앱이 이 메시지를 토스트로 표시합니다.
- (선택) 지각/정상 여부 판정, 중복 출석 방지, 관리자용 조회 API 등으로 확장 가능.

앱 쪽 통신 스펙은 `AttendanceApi.kt`(`AttendanceRequest` / `AttendanceResponse`)에 정의되어 있어, 이 형식에 맞춰 서버를 구현하면 됩니다.

---

## 기술 스택

| 구분 | 사용 기술 |
|------|-----------|
| 언어 | Kotlin |
| 플랫폼 | Android (minSdk 24 / targetSdk 34 / compileSdk 34, JVM 17) |
| 네트워크 | Retrofit 2.11.0, Gson Converter, OkHttp 4.11.0 |
| 위치 | Google Play Services Location 21.3.0 |
| 생체 인증 | AndroidX Biometric 1.1.0 |
| UI | Material Components 1.11.0, ConstraintLayout, DrawerLayout, CardView |
| 외부 API | NEIS 오픈 API(급식·시간표), Google Gemini API |

**요청 권한**: `INTERNET`, `USE_BIOMETRIC`, `USE_FINGERPRINT`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_NETWORK_STATE`

---

## 시스템 구성

앱은 세 종류의 백엔드와 통신합니다.

```
[Android 앱]
   ├─ 출석 전송 ──▶ FastAPI 서버 (로컬/자체 호스팅, HTTP)
   ├─ 급식/시간표 ─▶ NEIS 오픈 API (https://open.neis.go.kr/hub/)
   └─ AI 질의 ────▶ Google Gemini API (https://generativelanguage.googleapis.com/)
```

---

## 빌드 및 실행

1. Android Studio에서 프로젝트를 엽니다.
2. **FastAPI 서버 주소 설정** — `RetrofitClient.kt`의 `BASE_URL`을 실제 서버 IP로 변경합니다.
   ```kotlin
   private const val BASE_URL = "http://<서버_IPv4_주소>:<포트>/"
   ```
   (현재 값은 안내용 플레이스홀더이므로 반드시 교체해야 출석 전송이 동작합니다. 윈도우에서는 `cmd`의 `ipconfig`로 IPv4 주소를 확인할 수 있습니다.)
3. **네트워크 보안 설정** — HTTP(평문) 통신을 위해 `res/xml/network_security_config.xml`의 도메인에 서버 IP를 등록합니다.
4. 실제 지문·GPS를 사용하므로 **실기기 테스트**를 권장합니다.
5. `Run` ▶ 로 설치·실행합니다.

---

## 참고 / 개선 아이디어

현재 코드에는 다음 값들이 소스에 직접 들어가 있습니다. 학습·데모 단계에서는 편하지만, 실제 배포 전에는 분리하는 편이 안전합니다.

- **등록 학생 목록**: `MainActivity`의 `validStudents` 맵에 하드코딩 → 서버 조회 방식으로 이전 권장.
- **학교 좌표·반경**: 상수로 고정 → 설정값/서버 관리로 분리하면 여러 학교에 재사용 가능.
- **API 키**: NEIS 키가 `SchoolAPI.kt`에, Gemini 키가 요청 시 필요 → `local.properties`나 `BuildConfig` 등으로 옮겨 공개 저장소 노출을 방지.
- **평문 HTTP**: `usesCleartextTraffic="true"`로 로컬 서버와 통신 → 운영 환경에서는 HTTPS 적용 권장.

---

## 라이선스

별도 명시되어 있지 않습니다. 필요 시 저장소 소유자에게 문의하세요.
