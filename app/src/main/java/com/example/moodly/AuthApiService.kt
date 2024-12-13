package com.example.moodly
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
interface AuthApiService {
    @POST("auth/")  // Django의 로그인 URL 경로에 맞게 수정하세요.
    fun login(
        @Body credentials: LoginCredentials
    ): Call<LoginResponse>

    @POST("register/")
    fun register(@Body signUpData: SignUpData): Call<SignUpResponse>

    @POST("find-email/")
    fun findEmail(@Body requestBody: FindEmailRequest): Call<FindEmailResponse>

    @POST("api/password-reset/")
    fun resetPassword(@Body request: ResetPasswordRequest): Call<Void>

    // Google 로그인 요청 추가
    @POST("google-login/")  // 구글 로그인 처리 URL
    fun googleLogin(@Body googleLoginData: GoogleLoginData): Call<GoogleLoginResponse>
}

// 로그인 요청에 사용할 데이터 클래스 정의
data class LoginCredentials(
    val email: String,
    val password: String
)

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

data class SignUpData(
    val email: String,
    val password: String,
    val date_of_birth: String,
    val nameKorea: String,
    val gender: String,
    val phonenumber: String
)

data class SignUpResponse(
    val message: String // 서버에서 반환되는 메시지에 따라 수정
)

data class FindEmailRequest(
    val full_name: String,
    val phone_number: String
)

data class FindEmailResponse(
    val email: String?,
    val message: String?
)

data class ResetPasswordRequest(
    val email: String,
    val phonenumber: String? = null,
    val nameKorea: String? = null
)

// Google 로그인 요청 데이터 클래스
data class GoogleLoginData(
    val idToken: String,
    val email: String?,
    val name: String?
)

// Google 로그인 응답 데이터 클래스

data class GoogleLoginResponse(
    val user: User,
    val message: String,
    val token: Token,
    val isNewUser: Boolean // 새 사용자 여부 필드 추가
)
