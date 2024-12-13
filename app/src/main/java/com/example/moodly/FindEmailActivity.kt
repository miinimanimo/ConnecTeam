package com.example.moodly

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.moodly.FindEmailRequest
import com.example.moodly.FindEmailResponse
import com.example.moodly.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FindEmailActivity : AppCompatActivity() {

    private lateinit var fullNameEditText: EditText
    private lateinit var phoneNumberEditText: EditText
    private lateinit var findEmailButton: Button
    private lateinit var confirmButton: Button // 확인 버튼
    private lateinit var resultTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_email)

        fullNameEditText = findViewById(R.id.full_name_edit_text)
        phoneNumberEditText = findViewById(R.id.phone_number_edit_text)
        findEmailButton = findViewById(R.id.find_email_button)
        confirmButton = findViewById(R.id.confirm_button) // 확인 버튼 초기화
        resultTextView = findViewById(R.id.result_text_view)

        findEmailButton.setOnClickListener {
            findEmail()
        }

        confirmButton.setOnClickListener {
            // 로그인 페이지로 이동
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // 현재 액티비티 종료
        }
    }

    private fun findEmail() {
        val fullName = fullNameEditText.text.toString().trim()
        val phoneNumber = phoneNumberEditText.text.toString().trim()

        if (fullName.isEmpty() || phoneNumber.isEmpty()) {
            Toast.makeText(this, "모든 필드를 입력하세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val requestBody = FindEmailRequest(fullName, phoneNumber)

        // RetrofitClient를 사용하여 API 서비스 가져오기
        val apiService = RetrofitClient.getAuthApiService(this)

        // 이메일 찾기 요청 보내기
        apiService.findEmail(requestBody).enqueue(object : Callback<FindEmailResponse> {
            override fun onResponse(call: Call<FindEmailResponse>, response: Response<FindEmailResponse>) {
                if (response.isSuccessful) {
                    val email = response.body()?.email
                    if (email != null) {
                        resultTextView.text = "찾은 이메일: $email"
                        confirmButton.visibility = View.VISIBLE // 확인 버튼 보이기
                    } else {
                        resultTextView.text = response.body()?.message ?: "사용자를 찾을 수 없습니다."
                        confirmButton.visibility = View.GONE // 이메일을 찾지 못한 경우 버튼 숨기기
                    }
                } else {
                    resultTextView.text = "서버 오류: ${response.message()}"
                    confirmButton.visibility = View.GONE // 오류 발생 시 버튼 숨기기
                }
            }

            override fun onFailure(call: Call<FindEmailResponse>, t: Throwable) {
                resultTextView.text = "요청 실패: ${t.message}"
                confirmButton.visibility = View.GONE // 요청 실패 시 버튼 숨기기
            }
        })
    }
}
