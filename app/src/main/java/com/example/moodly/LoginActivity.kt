package com.example.moodly

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.moodly.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 로그인 버튼 클릭 시
        binding.loginButton.setOnClickListener {
            // 로그인 성공 로직 처리
            val isSuccess = performLogin() // 사용자 인증 함수
            if (isSuccess) {
                // 로그인 성공 시 MainActivity로 이동
                startActivity(Intent(this, MainActivity::class.java))
                finish() // 현재 로그인 Activity 종료
            }
        }
    }

    private fun performLogin(): Boolean {
        // 로그인 검증 로직 추가 (예: 서버 요청, 입력값 검증 등)
        return true
    }

}