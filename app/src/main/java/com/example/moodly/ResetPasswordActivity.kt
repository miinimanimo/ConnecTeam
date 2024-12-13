package com.example.moodly

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var nameEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var resetButton: Button
    private lateinit var apiService: AuthApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        // UI 요소 초기화
        emailEditText = findViewById(R.id.emailEditText)
        nameEditText = findViewById(R.id.nameEditText)
        phoneEditText = findViewById(R.id.phoneEditText)
        resetButton = findViewById(R.id.resetButton)

        // RetrofitClient를 통해 AuthApiService 인스턴스 얻기
        apiService = RetrofitClient.getAuthApiService(this)

        // 비밀번호 재설정 버튼 클릭 리스너
        resetButton.setOnClickListener { resetPassword() }
    }

    private fun resetPassword() {
        // 입력값 정리
        val email = cleanInput(emailEditText.text.toString().trim())
        val name = cleanInput(nameEditText.text.toString().trim())
        val phone = cleanInput(phoneEditText.text.toString().trim())

        // 입력 필드가 비어있는지 확인
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "모든 필드를 입력하세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // 비밀번호 재설정을 위한 요청 객체 생성
        val request = ResetPasswordRequest(email, phone, name)

        // API 요청
        apiService.resetPassword(request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ResetPasswordActivity,
                        "비밀번호 재설정 이메일이 전송되었습니다.",
                        Toast.LENGTH_SHORT).show()
                    // 로그인 페이지로 이동
                    startActivity(Intent(this@ResetPasswordActivity, LoginActivity::class.java))
                    finish()
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "알 수 없는 오류가 발생했습니다."
                    Toast.makeText(this@ResetPasswordActivity,
                        "이메일 전송 실패: $errorMessage",
                        Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@ResetPasswordActivity,
                    "네트워크 오류: ${t.message}",
                    Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun cleanInput(input: String): String {
        // 비정상적인 공백 문자(예: \xa0)를 일반 공백으로 대체
        return input.replace('\u00A0', ' ').trim()
    }
}