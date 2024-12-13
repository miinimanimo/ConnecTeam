package com.example.moodly

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth

    // 디버깅을 위한 resultLauncher 수정
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // 구글 로그인 창이 닫히면 무조건 MainActivity로 이동
        navigateToMainActivity()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Firebase Authentication 초기화
        auth = Firebase.auth

        // Google Sign-In 설정
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 구글 로그인 버튼 클릭 리스너 설정
        val googleSignInButton = findViewById<SignInButton>(R.id.googleSignInButton)
        googleSignInButton.setOnClickListener {
            signIn()
        }

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

        // 아이디 찾기 버튼 클릭 리스너 설정
        findViewById<Button>(R.id.findEmailButton).setOnClickListener {
            navigateToFindEmailActivity()
        }

        // 비밀번호 찾기 버튼 클릭 리스너 설정
        findViewById<Button>(R.id.resetPasswordButton).setOnClickListener {
            navigateToResetPasswordActivity()
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        resultLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    // 로그인 성공 시 JWT 토큰 발급 및 MainActivity로 이동 (기존 코드 유지)
                    val user = auth.currentUser
                    // TODO: 장고 서버에 로그인 요청
                    Toast.makeText(this, "Firebase 로그인 성공", Toast.LENGTH_SHORT).show()
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loginUser(email: String, password: String) {
        val credentials = LoginCredentials(email, password)

        val authApiService = RetrofitClient.getAuthApiService(this)

        authApiService.login(credentials).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val accessToken = response.body()?.token?.access
                    if (accessToken != null) {
                        saveToken(accessToken)
                        navigateToMainActivity()
                    } else {
                        Log.e("Login", "토큰이 없습니다.")
                    }
                } else {
                    showSnackbar("ID or PW is not correct.")
                    Log.e("Login", "로그인 실패: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Log.e("Login", "요청 실패: ${t.message}")
            }
        })
    }

    private fun showSnackbar(message: String) {
        val rootView = findViewById<View>(R.id.fragmentContainer)
        Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun saveToken(token: String) {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("jwt_token", token)
            apply()
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToSignUpFragment() {
        val fragment = SignUpFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToFindEmailActivity() {
        val intent = Intent(this, FindEmailActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToResetPasswordActivity() {
        val intent = Intent(this, ResetPasswordActivity::class.java)
        startActivity(intent)
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}