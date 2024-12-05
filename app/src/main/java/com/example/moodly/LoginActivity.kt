package com.example.moodly

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 로그인 버튼 클릭 리스너 설정
        findViewById<View>(R.id.loginButton).setOnClickListener {
            val email = findViewById<EditText>(R.id.emailEditText).text.toString()
            val password = findViewById<EditText>(R.id.passwordEditText).text.toString()
            loginUser(email, password)
        }
        // 회원가입 버튼 클릭 리스너 설정
        findViewById<Button>(R.id.signUpButton).setOnClickListener {
            navigateToSignUpFragment()
        }
    }

    private fun loginUser(email: String, password: String) {
        val credentials = LoginCredentials(email, password)

        // excludeAuth = true로 설정하여 토큰이 없는 상태로 Retrofit 인스턴스를 가져옴
        val authApiService = RetrofitClient.getAuthApiService(this)

        authApiService.login(credentials).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val accessToken = response.body()?.token?.access // access 토큰을 가져옴
                    if (accessToken != null) {
                        // 토큰을 SharedPreferences에 저장
                        saveToken(accessToken)

                        // MainActivity로 이동
                        navigateToMainActivity()
                    } else {
                        Log.e("Login", "토큰이 없습니다.")
                    }
                } else {
                    Log.e("Login", "로그인 실패: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Log.e("Login", "요청 실패: ${t.message}")
            }
        })
    }


    private fun saveToken(token: String) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("jwt_token", token)
            apply()
        }
    }

    private fun navigateToMainActivity() {
        // MainActivity로 이동
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // 현재 Activity 종료
    }
    private fun navigateToSignUpFragment() {
        val fragment = SignUpFragment()

        // FragmentTransaction을 시작하고 Fragment를 FrameLayout에 추가
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment) // fragmentContainer는 FrameLayout의 ID
            .addToBackStack(null) // 뒤로가기 버튼으로 돌아갈 수 있도록 설정
            .commit()
    }
}