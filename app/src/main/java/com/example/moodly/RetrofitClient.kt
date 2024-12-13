package com.example.moodly

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://211.188.50.51:8000/"  // Django API의 기본 URL

    private fun getOkHttpClient(context: Context, includeAuth: Boolean): OkHttpClient {
        val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

        // Interceptor to add Authorization header if needed
        val authInterceptor = Interceptor { chain ->
            val token = sharedPreferences.getString("jwt_token", null)
            val request: Request = if (includeAuth && token != null) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token") // 헤더에 토큰 추가
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }

        // Logging Interceptor 추가
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // 요청 및 응답 본문 로그 출력
        }

        // OkHttpClient에 Interceptor 추가
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor) // 인증 인터셉터 추가
            .addInterceptor(loggingInterceptor) // 로깅 인터셉터 추가
            .build()
    }

    fun getAuthApiService(context: Context): AuthApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(getOkHttpClient(context, false)) // AuthApiService용 OkHttpClient (토큰 제외)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(AuthApiService::class.java)
    }

    fun getMainApiService(context: Context): MainApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(getOkHttpClient(context, true)) // MainApiService용 OkHttpClient (토큰 포함)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(MainApiService::class.java)
    }
}
