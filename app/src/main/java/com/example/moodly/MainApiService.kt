package com.example.moodly
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface DiaryCreateApiService {
    @Multipart
    @POST("main/diaries/create/")
    fun saveDiaryEntry(
        @Part("title") title: RequestBody,
        @Part("content") content: RequestBody,
        @Part("emotion_category") emotionCategory: RequestBody,
        @Part image: MultipartBody.Part?, // Multipart로 이미지 파일 업로드
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("brightness") brightness: RequestBody
    ): Call<DiaryEntry>

}
data class DiaryEntry(
    val title: String,
    val content: String,
    val emotion_category: Int, // 감정 카테고리 ID
    val image: String?, // 저장된 이미지 URL
    val latitude: Double,
    val longitude: Double,
    val brightness: Int
)
