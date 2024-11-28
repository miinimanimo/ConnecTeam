package com.example.moodly
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Header

interface AuthApiService {
    @POST("auth/")  // Django의 로그인 URL 경로에 맞게 수정하세요.
    fun login(
        @Body credentials: LoginCredentials
    ): Call<LoginResponse>

    @GET("main/diaries/{year}/{month}/")
    fun getDiariesForMonth(
        @Header("Authorization") token: String,
        @Path("year") year: Int,
        @Path("month") month: Int
    ): Call<DaysResponse>  // DaysResponse로 변경
}

// 서버 응답을 위한 데이터 클래스
data class DaysResponse(
    val days: List<Int>  // [17, 22] 와 같은 형식으로 받을 수 있도록
)

// 로그인 요청에 사용할 데이터 클래스 정의
data class LoginCredentials(
    val email: String,
    val password: String
)

// 서버에서 반환할 JWT 토큰을 포함하는 데이터 클래스 정의
data class LoginResponse(
    val user: User,
    val message: String,
    val token: Token
)

data class User(
    val id: Int,
    val email: String,
    val date_of_birth: String,
    val nameKorea: String,
    val gender: String,
    val is_active: Boolean,
    val is_admin: Boolean
)

data class Token(
    val access: String,
    val refresh: String
)
