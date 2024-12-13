package com.example.moodly

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdditionalInfoActivity : AppCompatActivity() {

    private lateinit var authApiService: AuthApiService
    private lateinit var idToken: String
    private lateinit var email: String
    private lateinit var name: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_additional_info)

        authApiService = RetrofitClient.getAuthApiService(this)

        // GoogleLoginActivity에서 전달받은 데이터
        idToken = intent.getStringExtra("idToken") ?: ""
        email = intent.getStringExtra("email") ?: ""
        name = intent.getStringExtra("name") ?: ""

        val submitButton: Button = findViewById(R.id.submitButton)
        submitButton.setOnClickListener {
            val dateOfBirth = findViewById<EditText>(R.id.dateOfBirthEditText).text.toString()
            val gender = findViewById<EditText>(R.id.genderEditText).text.toString()
            val phoneNumber = findViewById<EditText>(R.id.phoneNumberEditText).text.toString()

            sendAdditionalInfo(dateOfBirth, gender, phoneNumber)
        }
    }

    private fun sendAdditionalInfo(dateOfBirth: String, gender: String, phoneNumber: String) {
        val signUpData = SignUpData(email, "password_placeholder", dateOfBirth, name, gender, phoneNumber)

        authApiService.register(signUpData).enqueue(object : Callback<SignUpResponse> {
            override fun onResponse(call: Call<SignUpResponse>, response: Response<SignUpResponse>) {
                if (response.isSuccessful) {
                    // 회원가입 성공 후, 로그인 처리
                    sendGoogleLoginData()
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                    Toast.makeText(this@AdditionalInfoActivity, "Sign Up failed: $errorMessage", Toast.LENGTH_SHORT).show()
                    Log.e("AdditionalInfoActivity", "Sign Up failed: $errorMessage")
                }
            }

            override fun onFailure(call: Call<SignUpResponse>, t: Throwable) {
                Toast.makeText(this@AdditionalInfoActivity, "Sign Up failed: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("AdditionalInfoActivity", "Sign Up failed: ${t.message}")
            }
        })
    }

    private fun sendGoogleLoginData() {
        val googleLoginData = GoogleLoginData(idToken, email, name)

        authApiService.googleLogin(googleLoginData).enqueue(object : Callback<GoogleLoginResponse> {
            override fun onResponse(call: Call<GoogleLoginResponse>, response: Response<GoogleLoginResponse>) {
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    val token = loginResponse?.token?.access
                    navigateToMainPage(token)
                } else {
                    Toast.makeText(this@AdditionalInfoActivity, "Login failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GoogleLoginResponse>, t: Throwable) {
                Toast.makeText(this@AdditionalInfoActivity, "Login failed: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun navigateToMainPage(token: String?) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("jwt_token", token)
        startActivity(intent)
    }
}